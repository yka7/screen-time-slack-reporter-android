package jp.co.screentime.slackreporter.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.domain.model.AppSettings
import jp.co.screentime.slackreporter.domain.model.SendResult
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.domain.usecase.SendDailyReportUseCase
import jp.co.screentime.slackreporter.platform.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DailySlackReportWorkerTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var usageRepository: UsageRepository
    private lateinit var sendDailyReportUseCase: SendDailyReportUseCase
    private lateinit var notificationHelper: NotificationHelper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        settingsRepository = mockk()
        usageRepository = mockk()
        sendDailyReportUseCase = mockk()
        notificationHelper = mockk(relaxed = true)
    }

    @Test
    fun `doWork returns success when send is disabled`() = runBlocking {
        val settings = AppSettings(
            webhookUrl = "https://hooks.slack.com/services/xxx",
            sendEnabled = false,
            sendHour = 21,
            sendMinute = 0,
            excludedPackages = emptySet()
        )
        every { settingsRepository.settingsFlow } returns flowOf(settings)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns success when webhook is not configured`() = runBlocking {
        val settings = AppSettings(
            webhookUrl = "",
            sendEnabled = true,
            sendHour = 21,
            sendMinute = 0,
            excludedPackages = emptySet()
        )
        every { settingsRepository.settingsFlow } returns flowOf(settings)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns failure when usage access is not granted`() = runBlocking {
        val settings = AppSettings(
            webhookUrl = "https://hooks.slack.com/services/xxx",
            sendEnabled = true,
            sendHour = 21,
            sendMinute = 0,
            excludedPackages = emptySet()
        )
        every { settingsRepository.settingsFlow } returns flowOf(settings)
        every { usageRepository.isUsageAccessGranted() } returns false

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `doWork returns success when report is sent successfully`() = runBlocking {
        val settings = AppSettings(
            webhookUrl = "https://hooks.slack.com/services/xxx",
            sendEnabled = true,
            sendHour = 21,
            sendMinute = 0,
            excludedPackages = emptySet()
        )
        every { settingsRepository.settingsFlow } returns flowOf(settings)
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { sendDailyReportUseCase() } returns SendResult(
            status = SendStatus.SUCCESS,
            lastSentEpochMillis = System.currentTimeMillis()
        )

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork returns success when report status is NOT_SENT`() = runBlocking {
        val settings = AppSettings(
            webhookUrl = "https://hooks.slack.com/services/xxx",
            sendEnabled = true,
            sendHour = 21,
            sendMinute = 0,
            excludedPackages = emptySet()
        )
        every { settingsRepository.settingsFlow } returns flowOf(settings)
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { sendDailyReportUseCase() } returns SendResult(
            status = SendStatus.NOT_SENT
        )

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `WORK_NAME constant is correctly defined`() {
        assertEquals("daily_slack_report_worker", DailySlackReportWorker.WORK_NAME)
    }

    private fun createWorker(): DailySlackReportWorker {
        return TestListenableWorkerBuilder<DailySlackReportWorker>(context)
            .setWorkerFactory(TestWorkerFactory())
            .build() as DailySlackReportWorker
    }

    private inner class TestWorkerFactory : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: androidx.work.WorkerParameters
        ): ListenableWorker {
            return DailySlackReportWorker(
                appContext,
                workerParameters,
                settingsRepository,
                usageRepository,
                sendDailyReportUseCase,
                notificationHelper
            )
        }
    }
}
