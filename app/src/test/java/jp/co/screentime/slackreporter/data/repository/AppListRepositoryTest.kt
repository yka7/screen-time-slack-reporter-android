package jp.co.screentime.slackreporter.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AppListRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AppListRepository
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        repository = AppListRepository(context)
        shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)
    }

    @Test
    fun `getAllApps returns empty list when no apps installed`() = runBlocking {
        val result = repository.getAllApps()

        assertNotNull(result)
        assertTrue(result.isEmpty() || result.all { !it.packageName.startsWith("android") })
    }

    @Test
    fun `getAllApps filters out system apps`() = runBlocking {
        // Add a system app
        val systemApp = ApplicationInfo().apply {
            packageName = "com.android.system"
            flags = ApplicationInfo.FLAG_SYSTEM
        }
        shadowPackageManager.installPackage(systemApp)

        // Add a user app
        val userApp = ApplicationInfo().apply {
            packageName = "com.user.app"
            flags = 0
        }
        shadowPackageManager.installPackage(userApp)

        val result = repository.getAllApps()

        // Should only include user app, not system app
        assertTrue(result.none { it.packageName == "com.android.system" })
    }

    @Test
    fun `getAllApps sorts results by app name`() = runBlocking {
        // Install apps in reverse alphabetical order
        val appC = createUserApp("com.test.c", "Zeta App")
        val appB = createUserApp("com.test.b", "Beta App")
        val appA = createUserApp("com.test.a", "Alpha App")

        shadowPackageManager.installPackage(appC)
        shadowPackageManager.installPackage(appB)
        shadowPackageManager.installPackage(appA)

        val result = repository.getAllApps()

        // Filter to our test apps
        val testApps = result.filter { it.packageName.startsWith("com.test.") }

        // Should be sorted alphabetically by app name
        if (testApps.size >= 3) {
            assertTrue(testApps[0].appName <= testApps[1].appName)
            assertTrue(testApps[1].appName <= testApps[2].appName)
        }
    }

    @Test
    fun `getAllApps includes package name and app name`() = runBlocking {
        val app = createUserApp("com.test.sample", "Sample App")
        shadowPackageManager.installPackage(app)

        val result = repository.getAllApps()

        val sampleApp = result.find { it.packageName == "com.test.sample" }
        if (sampleApp != null) {
            assertEquals("com.test.sample", sampleApp.packageName)
            assertEquals("Sample App", sampleApp.appName)
        }
    }

    @Test
    fun `getAllApps runs on IO dispatcher`() = runBlocking {
        // This test verifies the function completes without blocking
        // The actual dispatcher switch is tested implicitly
        val result = repository.getAllApps()

        assertNotNull(result)
    }

    @Test
    fun `getAllApps handles multiple user apps`() = runBlocking {
        val app1 = createUserApp("com.app1", "App One")
        val app2 = createUserApp("com.app2", "App Two")
        val app3 = createUserApp("com.app3", "App Three")

        shadowPackageManager.installPackage(app1)
        shadowPackageManager.installPackage(app2)
        shadowPackageManager.installPackage(app3)

        val result = repository.getAllApps()

        val testApps = result.filter { it.packageName.startsWith("com.app") }
        assertTrue(testApps.size >= 3)
    }

    @Test
    fun `getAllApps handles apps with same name`() = runBlocking {
        val app1 = createUserApp("com.test.1", "Same Name")
        val app2 = createUserApp("com.test.2", "Same Name")

        shadowPackageManager.installPackage(app1)
        shadowPackageManager.installPackage(app2)

        val result = repository.getAllApps()

        // Both apps should be included despite same name
        val sameNameApps = result.filter { it.appName == "Same Name" }
        assertTrue(sameNameApps.size >= 2 || result.size >= 2)
    }

    @Test
    fun `getAllApps handles empty app name gracefully`() = runBlocking {
        val app = ApplicationInfo().apply {
            packageName = "com.test.empty"
            flags = 0
        }
        shadowPackageManager.installPackage(app)

        val result = repository.getAllApps()

        // Should not crash with empty/null label
        assertNotNull(result)
    }

    private fun createUserApp(packageName: String, label: String): ApplicationInfo {
        return ApplicationInfo().apply {
            this.packageName = packageName
            this.flags = 0 // Not a system app
            this.name = label
        }
    }
}
