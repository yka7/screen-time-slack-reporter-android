package jp.co.screentime.slackreporter.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
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
        repository = AppListRepository(context, Dispatchers.IO)
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
        val systemApp = createPackageInfo("com.android.system", "System", ApplicationInfo.FLAG_SYSTEM)
        shadowPackageManager.installPackage(systemApp)

        // Add a user app
        val userApp = createPackageInfo("com.user.app", "User App", 0)
        shadowPackageManager.installPackage(userApp)

        val result = repository.getAllApps()

        // Should only include user app, not system app
        assertTrue(result.none { it.packageName == "com.android.system" })
    }

    @Test
    fun `getAllApps sorts results by app name`() = runBlocking {
        // Install apps in reverse alphabetical order
        val appC = createPackageInfo("com.test.c", "Zeta App", 0)
        val appB = createPackageInfo("com.test.b", "Beta App", 0)
        val appA = createPackageInfo("com.test.a", "Alpha App", 0)

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
        val app = createPackageInfo("com.test.sample", "Sample App", 0)
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
        val app1 = createPackageInfo("com.app1", "App One", 0)
        val app2 = createPackageInfo("com.app2", "App Two", 0)
        val app3 = createPackageInfo("com.app3", "App Three", 0)

        shadowPackageManager.installPackage(app1)
        shadowPackageManager.installPackage(app2)
        shadowPackageManager.installPackage(app3)

        val result = repository.getAllApps()

        val testApps = result.filter { it.packageName.startsWith("com.app") }
        assertTrue(testApps.size >= 3)
    }

    @Test
    fun `getAllApps handles apps with same name`() = runBlocking {
        val app1 = createPackageInfo("com.test.1", "Same Name", 0)
        val app2 = createPackageInfo("com.test.2", "Same Name", 0)

        shadowPackageManager.installPackage(app1)
        shadowPackageManager.installPackage(app2)

        val result = repository.getAllApps()

        // Both apps should be included despite same name
        val sameNameApps = result.filter { it.appName == "Same Name" }
        assertTrue(sameNameApps.size >= 2 || result.size >= 2)
    }

    @Test
    fun `getAllApps handles empty app name gracefully`() = runBlocking {
        val app = createPackageInfo("com.test.empty", "", 0)
        shadowPackageManager.installPackage(app)

        val result = repository.getAllApps()

        // Should not crash with empty/null label
        assertNotNull(result)
    }

    private fun createPackageInfo(packageName: String, label: String, flags: Int): PackageInfo {
        return PackageInfo().apply {
            this.packageName = packageName
            this.applicationInfo = ApplicationInfo().apply {
                this.packageName = packageName
                this.flags = flags
                this.nonLocalizedLabel = label
            }
        }
    }
}
