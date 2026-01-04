package jp.co.screentime.slackreporter.presentation.model

import android.content.Context
import android.graphics.drawable.Drawable
import jp.co.screentime.slackreporter.R

/**
 * UI表示用のアプリ利用情報
 *
 * @property packageName パッケージ名
 * @property appName アプリ名（表示名）
 * @property icon アプリアイコン
 * @property durationMinutes 利用時間（分）
 * @property isExcluded 除外対象かどうか
 */
data class UiAppUsage(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val durationMinutes: Int,
    val isExcluded: Boolean
) {
    /**
     * フォーマットされた利用時間
     */
    fun formattedDuration(context: Context): String {
        return when {
            durationMinutes == 0 -> context.getString(R.string.exclusions_no_usage_today_for_app)
            durationMinutes < 1 -> context.getString(R.string.time_format_less_than_minute)
            durationMinutes < 60 -> context.getString(R.string.time_format_minutes, durationMinutes)
            else -> {
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                if (mins > 0) {
                    context.getString(R.string.time_format_hours_minutes, hours, mins)
                } else {
                    // 1時間ちょうどなどの場合
                    context.getString(R.string.time_format_hours_minutes, hours, 0)
                }
            }
        }
    }
}
