package jp.co.screentime.slackreporter.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.screentime.slackreporter.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知ヘルパークラス
 *
 * 通知チャンネルの作成とエラー通知の表示を担当
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * アプリ起動時に通知チャンネルを作成
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val errorChannel = NotificationChannel(
                CHANNEL_ERROR,
                "エラー通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Slack送信エラー発生時の通知"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(errorChannel)
        }
    }

    /**
     * Slack送信失敗時のエラー通知を表示
     */
    fun showSlackSendFailureNotification(message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("Slack送信に失敗しました")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ERROR = "error_notifications"
        private const val ERROR_NOTIFICATION_ID = 9001
    }
}
