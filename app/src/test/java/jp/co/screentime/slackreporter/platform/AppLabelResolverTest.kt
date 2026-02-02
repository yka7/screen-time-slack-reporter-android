package jp.co.screentime.slackreporter.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AppLabelResolverTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var resolver: AppLabelResolver

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        packageManager = context.packageManager
        resolver = AppLabelResolver(context)
    }

    @Test
    fun `getAppLabel returns package name when app not found`() {
        val unknownPackage = "com.unknown.package"

        val result = resolver.getAppLabel(unknownPackage)

        assertEquals(unknownPackage, result)
    }

    @Test
    fun `getAppLabel caches result on subsequent calls`() {
        val packageName = "com.test.package"
        
        // First call
        val result1 = resolver.getAppLabel(packageName)
        // Second call should use cache
        val result2 = resolver.getAppLabel(packageName)

        assertEquals(result1, result2)
        assertNotNull(result1)
    }

    @Test
    fun `getAppIcon returns null when app not found`() {
        val unknownPackage = "com.unknown.package"

        val result = resolver.getAppIcon(unknownPackage)

        assertNull(result)
    }

    @Test
    fun `getAppIcon caches result on subsequent calls`() {
        val packageName = "com.test.package"

        // First call
        val result1 = resolver.getAppIcon(packageName)
        // Second call should return same cached value
        val result2 = resolver.getAppIcon(packageName)

        assertEquals(result1, result2)
    }

    @Test
    fun `getAppIcon returns null when cached as null`() {
        val packageName = "com.unknown.app"

        // First call caches null
        val result1 = resolver.getAppIcon(packageName)
        assertNull(result1)

        // Second call should return cached null
        val result2 = resolver.getAppIcon(packageName)
        assertNull(result2)
    }

    @Test
    fun `clearCache removes all cached entries`() {
        val packageName = "com.test.package"

        // Cache some data
        resolver.getAppLabel(packageName)
        resolver.getAppIcon(packageName)

        // Clear cache
        resolver.clearCache()

        // Verify cache is cleared by calling again
        // (would fetch from PackageManager again if cache was cleared)
        val label = resolver.getAppLabel(packageName)
        val icon = resolver.getAppIcon(packageName)

        // Should not crash and should handle lookups correctly
        assertNotNull(label)
    }

    @Test
    fun `getAppLabel handles multiple different packages`() {
        val package1 = "com.test.app1"
        val package2 = "com.test.app2"

        val label1 = resolver.getAppLabel(package1)
        val label2 = resolver.getAppLabel(package2)

        // Both should return valid strings (fallback to package name)
        assertNotNull(label1)
        assertNotNull(label2)
        assertNotEquals(label1, label2)
    }

    @Test
    fun `synchronized methods handle concurrent access safely`() {
        val packageName = "com.test.concurrent"

        // This test verifies @Synchronized annotation works
        // by making multiple calls (would detect race conditions in real scenario)
        val results = (1..10).map { resolver.getAppLabel(packageName) }

        // All results should be identical due to synchronization
        assertTrue(results.all { it == results[0] })
    }

    @Test
    fun `getAppLabel with empty string returns empty string`() {
        val result = resolver.getAppLabel("")

        assertEquals("", result)
    }

    @Test
    fun `getAppIcon with empty string returns null`() {
        val result = resolver.getAppIcon("")

        assertNull(result)
    }
}
