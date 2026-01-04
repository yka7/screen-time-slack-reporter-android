package jp.co.screentime.slackreporter.domain.usecase

import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.SlackRepository
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.domain.model.SendResult
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 日次レポートをSlackへ送信するユースケース
 */
class SendDailyReportUseCase @Inject constructor(
    private val getTodayUsageUseCase: GetTodayUsageUseCase,
    private val settingsRepository: SettingsRepository,
    private val slackRepository: SlackRepository,
    private val appLabelResolver: AppLabelResolver
) {
    companion object {
        private const val TOP_APPS_COUNT = 5
        private const val TARGET_MINUTES = 30
    }

    /**
     * 日次レポートを送信
     *
     * @return 送信結果
     */
    suspend operator fun invoke(): SendResult {
        val settings = settingsRepository.settingsFlow.first()

        if (!settings.isWebhookConfigured) {
            return SendResult(
                status = SendStatus.FAILED,
                errorMessage = "Webhook URLが設定されていません"
            )
        }

        val allUsage = getTodayUsageUseCase()

        // 除外適用
        val filteredUsage = allUsage.filter { usage ->
            usage.packageName !in settings.excludedPackages
        }

        // メッセージ生成
        val message = buildSlackMessage(filteredUsage)

        // Slack送信
        return try {
            val result = slackRepository.sendMessage(settings.webhookUrl, message)
            if (result.isSuccess) {
                val now = System.currentTimeMillis()
                settingsRepository.updateSendResult(SendStatus.SUCCESS, now, null)
                SendResult(
                    status = SendStatus.SUCCESS,
                    lastSentEpochMillis = now
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                settingsRepository.updateSendResult(SendStatus.FAILED, null, error)
                SendResult(
                    status = SendStatus.FAILED,
                    errorMessage = error
                )
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            settingsRepository.updateSendResult(SendStatus.FAILED, null, error)
            SendResult(
                status = SendStatus.FAILED,
                errorMessage = error
            )
        }
    }

    /**
     * Slackメッセージを生成
     */
    private fun buildSlackMessage(usageList: List<AppUsage>): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.JAPAN)
        val dateString = dateFormat.format(Date())

        val totalMinutes = millisToMinutes(usageList.sumOf { it.durationMillis })
        val diffMinutes = totalMinutes - TARGET_MINUTES
        val diffString = if (diffMinutes >= 0) "+${diffMinutes}分" else "${diffMinutes}分"

        val totalTimeString = formatDuration(totalMinutes)

        val sb = StringBuilder()

        // ヘッダー
        sb.appendLine("📱 *$dateString の利用状況*")
        sb.appendLine("合計: *$totalTimeString* (目標${TARGET_MINUTES}分との差: $diffString)")
        sb.appendLine()

        if (usageList.isEmpty()) {
            sb.appendLine("本日は利用が検出されませんでした。")
            return sb.toString()
        }

        // 上位アプリ
        val topApps = usageList.take(TOP_APPS_COUNT)
        val otherApps = usageList.drop(TOP_APPS_COUNT)

        topApps.forEach { usage ->
            val appName = appLabelResolver.getAppLabel(usage.packageName)
            val durationString = formatDuration(usage.durationMinutes)
            sb.appendLine("• $appName - $durationString")
        }

        // その他
        if (otherApps.isNotEmpty()) {
            val otherMinutes = millisToMinutes(otherApps.sumOf { it.durationMillis })
            val otherDurationString = formatDuration(otherMinutes)
            sb.appendLine("• その他 - $otherDurationString")
        }

        return sb.toString()
    }

    /**
     * 時間をフォーマット
     */
    private fun formatDuration(minutes: Int): String {
        return when {
            minutes < 1 -> "1分未満"
            minutes < 60 -> "${minutes}分"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}時間${mins}分" else "${hours}時間"
            }
        }
    }

    private fun millisToMinutes(durationMillis: Long): Int {
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    }
}
