package jp.co.screentime.slackreporter.platform

import android.content.Context
import jp.co.screentime.slackreporter.R
import java.util.concurrent.TimeUnit

/**
 * Usage duration formatter.
 */
object DurationFormatter {
    fun minutesFromMillis(durationMillis: Long): Int {
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    }

    fun formatMinutes(context: Context, minutes: Int): String {
        if (minutes < 1) {
            return context.getString(R.string.time_format_less_than_minute)
        }

        val hours = minutes / 60
        val mins = minutes % 60

        val hoursString = if (hours > 0) {
            context.resources.getQuantityString(R.plurals.hours, hours, hours)
        } else {
            null
        }

        val minutesString = if (mins > 0) {
            context.resources.getQuantityString(R.plurals.minutes, mins, mins)
        } else {
            null
        }

        return when {
            hoursString != null && minutesString != null -> {
                context.getString(R.string.time_format_hours_and_minutes, hoursString, minutesString)
            }
            hoursString != null -> hoursString
            minutesString != null -> minutesString
            else -> context.resources.getQuantityString(R.plurals.minutes, 0, 0)
        }
    }
}
