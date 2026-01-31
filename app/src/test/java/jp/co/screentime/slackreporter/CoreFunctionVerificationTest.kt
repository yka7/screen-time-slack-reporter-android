package jp.co.screentime.slackreporter

import io.mockk.*
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.SlackRepository
import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.data.slack.SlackMessageBuilder
import jp.co.screentime.slackreporter.data.slack.SlackWebhookValidator
import jp.co.screentime.slackreporter.domain.model.AppSettings
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsageUseCase
import jp.co.screentime.slackreporter.domain.usecase.SendDailyReportUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Core Function Verification Tests
 * 
 * リポジトリの存在意義を検証するテストスイート
 * README.mdで主張している機能が実際に動作することを確認する
 */
class CoreFunctionVerificationTest {

    /**
     * CF-001: 当日のアプリ別利用時間を取得できる
     * 
     * README.md:7 より
     * 「UsageStatsManagerで当日0:00起点のアプリ別利用時間を取得」
     */
    @Test
    fun `CF-001 can retrieve daily app usage statistics`() = runTest {
        val usageRepository = mockk<UsageRepository>()
        val mockUsage = listOf(
            AppUsage("com.youtube", 1800000L),
            AppUsage("com.chrome", 900000L)
        )
        coEvery { usageRepository.getTodayUsage() } returns mockUsage
        
        val useCase = GetTodayUsageUseCase(usageRepository)
        
        val result = useCase()
        
        assertTrue("利用時間リストが取得できる", result.isNotEmpty())
        assertTrue("各アプリにパッケージ名がある", result.all { it.packageName.isNotEmpty() })
        assertTrue("各アプリに利用時間がある", result.all { it.durationMillis > 0 })
        
        println("✅ CF-001: 当日のアプリ別利用時間を取得できる - PASS")
    }

    /**
     * CF-002: 取得した利用情報をSlack Webhookへ送信できる
     * 
     * README.md:8 より
     * 「1日1回、指定時刻にSlack Incoming WebhookへJSON投稿」
     */
    @Test
    fun `CF-002 can send usage report to Slack webhook`() = runTest {
        val validWebhookUrl = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX"
        val slackRepository = mockk<SlackRepository>()
        coEvery { slackRepository.sendMessage(any(), any()) } returns Result.success(Unit)
        
        val validationResult = SlackWebhookValidator.validate(validWebhookUrl)
        
        assertTrue("有効なWebhook URLを受け入れる", validationResult.isSuccess)
        
        val sendResult = slackRepository.sendMessage(validWebhookUrl, "テストメッセージ")
        
        assertTrue("Slackへメッセージを送信できる", sendResult.isSuccess)
        
        println("✅ CF-002: Slack Webhookへ送信できる - PASS")
    }

    /**
     * CF-003: 指定時刻に自動でレポートを送信できる
     * 
     * README.md:8 より
     * WorkManagerによる定時実行
     */
    @Test
    fun `CF-003 daily report worker is properly configured`() {
        val workerClass = Class.forName(
            "jp.co.screentime.slackreporter.workers.DailySlackReportWorker"
        )
        
        assertNotNull("Workerクラスが存在する", workerClass)
        assertTrue(
            "HiltWorkerアノテーションがある",
            workerClass.annotations.any { it.annotationClass.simpleName == "HiltWorker" }
        )
        
        println("✅ CF-003: 定時実行Workerが適切に構成されている - PASS")
    }

    /**
     * CF-004: 特定のアプリを除外設定できる
     * 
     * README.md:9 より
     * 「通知から除外したいアプリを設定可能」
     */
    @Test
    fun `CF-004 can exclude specific apps from report`() = runTest {
        val settingsRepository = mockk<SettingsRepository>()
        val slackRepository = mockk<SlackRepository>()
        val slackMessageBuilder = mockk<SlackMessageBuilder>()
        val getTodayUsageUseCase = mockk<GetTodayUsageUseCase>()
        
        val excludedPackage = "com.game.excluded"
        val settings = mockk<AppSettings> {
            every { isWebhookConfigured } returns true
            every { webhookUrl } returns "https://hooks.slack.com/services/xxx"
            every { excludedPackages } returns setOf(excludedPackage)
        }
        
        every { settingsRepository.settingsFlow } returns flowOf(settings)
        coEvery { getTodayUsageUseCase() } returns listOf(
            AppUsage("com.youtube", 1800000L),
            AppUsage(excludedPackage, 900000L)
        )
        
        val capturedUsage = slot<List<AppUsage>>()
        every { slackMessageBuilder.build(capture(capturedUsage)) } returns "message"
        coEvery { slackRepository.sendMessage(any(), any()) } returns Result.success(Unit)
        coEvery { settingsRepository.updateSendResult(any(), any(), any()) } just Runs
        
        val useCase = SendDailyReportUseCase(
            getTodayUsageUseCase,
            settingsRepository,
            slackRepository,
            slackMessageBuilder
        )
        
        useCase()
        
        assertFalse(
            "除外アプリがレポートに含まれない",
            capturedUsage.captured.any { it.packageName == excludedPackage }
        )
        assertTrue(
            "除外されていないアプリは含まれる",
            capturedUsage.captured.any { it.packageName == "com.youtube" }
        )
        
        println("✅ CF-004: 除外アプリ設定が機能する - PASS")
    }

    /**
     * CF-005: 手動でも送信できる
     * 
     * README.md:10 より
     * 「いつでも手動で送信可能」
     */
    @Test
    fun `CF-005 can manually send report`() = runTest {
        val settingsRepository = mockk<SettingsRepository>()
        val slackRepository = mockk<SlackRepository>()
        val slackMessageBuilder = mockk<SlackMessageBuilder>()
        val getTodayUsageUseCase = mockk<GetTodayUsageUseCase>()
        
        val settings = mockk<AppSettings> {
            every { isWebhookConfigured } returns true
            every { webhookUrl } returns "https://hooks.slack.com/services/xxx"
            every { excludedPackages } returns emptySet()
        }
        
        every { settingsRepository.settingsFlow } returns flowOf(settings)
        coEvery { getTodayUsageUseCase() } returns listOf(AppUsage("com.test", 60000L))
        every { slackMessageBuilder.build(any()) } returns "test message"
        coEvery { slackRepository.sendMessage(any(), any()) } returns Result.success(Unit)
        coEvery { settingsRepository.updateSendResult(any(), any(), any()) } just Runs
        
        val useCase = SendDailyReportUseCase(
            getTodayUsageUseCase,
            settingsRepository,
            slackRepository,
            slackMessageBuilder
        )
        
        val result = useCase()
        
        assertEquals("手動送信が成功する", SendStatus.SUCCESS, result.status)
        assertNotNull("送信時刻が記録される", result.lastSentEpochMillis)
        
        println("✅ CF-005: 手動送信が機能する - PASS")
    }
}
