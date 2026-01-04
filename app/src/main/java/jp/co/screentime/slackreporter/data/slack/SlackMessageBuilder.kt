package jp.co.screentime.slackreporter.data.slack

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import jp.co.screentime.slackreporter.platform.DurationFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Builds Slack message text for daily usage reports.
 */
class SlackMessageBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLabelResolver: AppLabelResolver
) {
    companion object {
        private const val TOP_APPS_COUNT = 5
        private const val TARGET_MINUTES = 30
    }

    fun build(usageList: List<AppUsage>, date: Date = Date()): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.JAPAN)
        val dateString = dateFormat.format(date)

        val totalMinutes = DurationFormatter.minutesFromMillis(
            usageList.sumOf { it.durationMillis }
        )
        val diffMinutes = totalMinutes - TARGET_MINUTES
        val diffAbs = kotlin.math.abs(diffMinutes)
        val diffBase = context.resources.getQuantityString(R.plurals.minutes, diffAbs, diffAbs)
        val diffString = if (diffMinutes >= 0) "+$diffBase" else "-$diffBase"
        val totalTimeString = DurationFormatter.formatMinutes(context, totalMinutes)

        val sb = StringBuilder()
        sb.appendLine(
            context.getString(
                R.string.slack_message_header,
                dateString,
                totalTimeString,
                diffString
            )
        )
        sb.appendLine()

        if (usageList.isEmpty()) {
            sb.appendLine(context.getString(R.string.slack_message_no_usage))
            return sb.toString()
        }

        val topApps = usageList.take(TOP_APPS_COUNT)
        val otherApps = usageList.drop(TOP_APPS_COUNT)

        topApps.forEach { usage ->
            val appName = appLabelResolver.getAppLabel(usage.packageName)
            val durationMinutes = DurationFormatter.minutesFromMillis(usage.durationMillis)
            val durationString = DurationFormatter.formatMinutes(context, durationMinutes)
            sb.appendLine(
                context.getString(R.string.slack_message_app_line, appName, durationString)
            )
        }

        if (otherApps.isNotEmpty()) {
            val otherMinutes = DurationFormatter.minutesFromMillis(
                otherApps.sumOf { it.durationMillis }
            )
            val otherDurationString = DurationFormatter.formatMinutes(context, otherMinutes)
            sb.appendLine(context.getString(R.string.slack_message_other, otherDurationString))
        }

        return sb.toString()
    }
}
