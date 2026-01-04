package jp.co.screentime.slackreporter.domain.usecase

import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.SlackRepository
import jp.co.screentime.slackreporter.data.slack.SlackMessageBuilder
import jp.co.screentime.slackreporter.domain.model.SendResult
import jp.co.screentime.slackreporter.domain.model.SendStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 日次レポートをSlackへ送信するユースケース
 */
class SendDailyReportUseCase @Inject constructor(
    private val getTodayUsageUseCase: GetTodayUsageUseCase,
    private val settingsRepository: SettingsRepository,
    private val slackRepository: SlackRepository,
    private val slackMessageBuilder: SlackMessageBuilder
) {
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
        val message = slackMessageBuilder.build(filteredUsage)

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

}
