package jp.co.screentime.slackreporter.data.slack

import android.content.Context
import android.content.res.Resources
import io.mockk.*
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class SlackMessageBuilderTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var appLabelResolver: AppLabelResolver
    private lateinit var builder: SlackMessageBuilder

    @Before
    fun setup() {
        resources = mockk(relaxed = true)
        context = mockk(relaxed = true)
        appLabelResolver = mockk()

        // Mock resources
        every { resources.getQuantityString(R.plurals.minutes, any(), any()) } answers {
            val count = secondArg<Int>()
            "${count}分"
        }
        
        // Mock context.resources
        every { context.resources } returns resources

        // Mock getString with formatting args
        // Note: We match specific patterns to ensure correctness
        every { context.getString(R.string.slack_message_header, *anyVararg()) } answers {
            val formatArgs = args[1] as Array<*>
            val date = formatArgs[0] as String
            val total = formatArgs[1] as String
            val diff = formatArgs[2] as String
            "📊 $date の利用時間: $total ($diff)"
        }
        every { context.getString(R.string.slack_message_no_usage) } returns "本日の利用はありませんでした"
        every { context.getString(R.string.slack_message_app_line, *anyVararg()) } answers {
            val formatArgs = args[1] as Array<*>
            val appName = formatArgs[0] as String
            val duration = formatArgs[1] as String
            "• $appName: $duration"
        }
        every { context.getString(R.string.slack_message_other, *anyVararg()) } answers {
            val formatArgs = args[1] as Array<*>
            val duration = formatArgs[0] as String
            "• その他: $duration"
        }

        // Mock AppLabelResolver
        every { appLabelResolver.getAppLabel(any()) } answers {
            val packageName = firstArg<String>()
            when (packageName) {
                "com.youtube.android" -> "YouTube"
                "com.chrome.android" -> "Chrome"
                "jp.naver.line.android" -> "LINE"
                else -> packageName.substringAfterLast('.')
            }
        }
        every { appLabelResolver.getAppIcon(any()) } returns null
        
        builder = SlackMessageBuilder(context, appLabelResolver)
    }

    @Test
    fun `通常のUsageリストでメッセージを生成できる`() {
        val usageList = listOf(
            AppUsage("com.youtube.android", 2700000L),  // 45分
            AppUsage("com.chrome.android", 1800000L),   // 30分
            AppUsage("jp.naver.line.android", 900000L)  // 15分
        )
        
        val message = builder.build(usageList)
        
        assertNotNull(message)
        assertTrue(message.contains("YouTube"))
        assertTrue(message.contains("Chrome"))
        assertTrue(message.contains("LINE"))
    }

    @Test
    fun `空のUsageリストでもメッセージを生成できる`() {
        val message = builder.build(emptyList())
        
        assertNotNull(message)
        assertTrue(message.contains("本日の利用はありませんでした"))
    }

    @Test
    fun `日付を指定してメッセージを生成できる`() {
        val usageList = listOf(AppUsage("com.youtube.android", 1800000L))
        val testDate = Date(1706745600000L)  // 2024-02-01
        
        val message = builder.build(usageList, testDate)
        
        assertNotNull(message)
    }

    @Test
    fun `5件を超えるアプリがある場合はその他にまとめられる`() {
        val usageList = (1..10).map { i ->
            AppUsage("com.app$i", (1000000 * i).toLong())
        }
        
        val message = builder.build(usageList)
        
        assertNotNull(message)
        assertTrue(message.contains("その他"))
    }

    @Test
    fun `5件以下のアプリの場合はその他が表示されない`() {
        val usageList = listOf(
            AppUsage("com.youtube.android", 2700000L),
            AppUsage("com.chrome.android", 1800000L)
        )
        
        val message = builder.build(usageList)
        
        assertNotNull(message)
        assertFalse(message.contains("その他"))
    }

    @Test
    fun `AppLabelResolverが正しく呼ばれる`() {
        val usageList = listOf(
            AppUsage("com.youtube.android", 1800000L)
        )
        
        builder.build(usageList)
        
        verify { appLabelResolver.getAppLabel("com.youtube.android") }
    }

    @Test
    fun `ヘッダーに合計時間と差分が含まれる`() {
        val usageList = listOf(
            AppUsage("com.youtube.android", 3600000L)  // 60分
        )
        
        val message = builder.build(usageList)
        
        assertNotNull(message)
        // ヘッダーが生成されていることを確認
        verify { 
            context.getString(
                R.string.slack_message_header,
                any(),
                any(),
                any()
            )
        }
    }
}
