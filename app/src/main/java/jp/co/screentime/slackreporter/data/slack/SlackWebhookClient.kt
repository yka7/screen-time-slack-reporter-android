package jp.co.screentime.slackreporter.data.slack

import jp.co.screentime.slackreporter.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Slack Incoming Webhookクライアント
 */
@Singleton
class SlackWebhookClient @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Slackへメッセージを送信
     *
     * @param webhookUrl Webhook URL
     * @param text 送信するテキスト
     * @return 送信結果
     */
    suspend fun sendMessage(webhookUrl: String, text: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val validatedUrl = SlackWebhookValidator.validate(webhookUrl).getOrElse { error ->
                    return@withContext Result.failure(error)
                }

                val json = JSONObject().apply {
                    put("text", text)
                }

                val requestBody = json.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url(validatedUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            Exception("Slack API error: ${response.code} ${response.message}")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * テスト送信
     *
     * @param webhookUrl Webhook URL
     * @return 送信結果
     */
    suspend fun sendTestMessage(webhookUrl: String): Result<Unit> {
        return sendMessage(
            webhookUrl,
            "📱 ScreenTime Slack Reporter からのテスト送信です。"
        )
    }
}
