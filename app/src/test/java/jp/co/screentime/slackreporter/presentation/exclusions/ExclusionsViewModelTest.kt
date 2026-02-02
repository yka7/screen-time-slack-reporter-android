package jp.co.screentime.slackreporter.presentation.exclusions

import android.content.Context
import io.mockk.*
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.domain.model.App
import jp.co.screentime.slackreporter.domain.model.AppSettings
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.domain.usecase.GetAllAppsUseCase
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsedAppsUseCase
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExclusionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var getAllAppsUseCase: GetAllAppsUseCase
    private lateinit var getTodayUsedAppsUseCase: GetTodayUsedAppsUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appLabelResolver: AppLabelResolver

    private val testSettings = AppSettings(
        webhookUrl = "https://hooks.slack.com/services/xxx",
        sendEnabled = true,
        sendHour = 21,
        sendMinute = 0,
        excludedPackages = emptySet()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true) {
            every { packageName } returns "jp.co.screentime.slackreporter"
            every { getString(R.string.exclusions_load_failed) } returns "Load failed"
        }
        getAllAppsUseCase = mockk()
        getTodayUsedAppsUseCase = mockk()
        settingsRepository = mockk(relaxed = true) {
            every { settingsFlow } returns flowOf(testSettings)
            every { showExcludedOnlyFlow } returns flowOf(false)
        }
        appLabelResolver = mockk(relaxed = true) {
            every { getAppIcon(any()) } returns null
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期化時にデータがロードされる`() = runTest {
        val mockAllApps = listOf(
            App("com.example.app1", "App 1"),
            App("com.example.app2", "App 2")
        )
        val mockUsage = listOf(
            AppUsage("com.example.app1", 3600000L) // 60 mins
        )

        coEvery { getAllAppsUseCase() } returns mockAllApps
        coEvery { getTodayUsedAppsUseCase() } returns mockUsage

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.apps.size)
        
        // App 1 should have usage
        val app1 = state.apps.find { it.packageName == "com.example.app1" }
        assertNotNull(app1)
        assertEquals(60, app1?.durationMinutes)
        
        // App 2 should have 0 usage
        val app2 = state.apps.find { it.packageName == "com.example.app2" }
        assertNotNull(app2)
        assertEquals(0, app2?.durationMinutes)
    }

    @Test
    fun `自分のパッケージは除外される`() = runTest {
        val myPackage = "jp.co.screentime.slackreporter"
        val mockAllApps = listOf(
            App(myPackage, "My App"),
            App("com.other.app", "Other App")
        )
        
        coEvery { getAllAppsUseCase() } returns mockAllApps
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.apps.size)
        assertEquals("com.other.app", state.apps[0].packageName)
    }

    @Test
    fun `ロードエラー時にエラーメッセージが表示される`() = runTest {
        coEvery { getAllAppsUseCase() } throws RuntimeException("API Error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API Error", state.errorMessage)
        assertTrue(state.apps.isEmpty())
    }

    @Test
    fun `除外設定が反映される`() = runTest {
        val settingsWithExclusion = testSettings.copy(
            excludedPackages = setOf("com.example.app1")
        )
        every { settingsRepository.settingsFlow } returns flowOf(settingsWithExclusion)
        
        val mockAllApps = listOf(
            App("com.example.app1", "App 1")
        )
        coEvery { getAllAppsUseCase() } returns mockAllApps
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val app = state.apps.first()
        assertTrue(app.isExcluded)
    }

    @Test
    fun `除外のみ表示フィルターが機能する`() = runTest {
        every { settingsRepository.showExcludedOnlyFlow } returns flowOf(true)
        
        val mockAllApps = listOf(
            App("com.example.app1", "App 1"),
            App("com.example.app2", "App 2")
        )
        // App 1 is excluded via settings
        val settingsWithExclusion = testSettings.copy(
            excludedPackages = setOf("com.example.app1")
        )
        every { settingsRepository.settingsFlow } returns flowOf(settingsWithExclusion)
        
        coEvery { getAllAppsUseCase() } returns mockAllApps
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showExcludedOnly)
    }

    @Test
    fun `onShowExcludedOnlyChangedがリポジトリを呼び出す`() = runTest {
        coEvery { getAllAppsUseCase() } returns emptyList()
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onShowExcludedOnlyChanged(true)
        advanceUntilIdle()

        coVerify { settingsRepository.setShowExcludedOnly(true) }
    }
    
    @Test
    fun `onShowAllAppsがリポジトリを呼び出す`() = runTest {
        coEvery { getAllAppsUseCase() } returns emptyList()
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onShowAllApps()
        advanceUntilIdle()

        coVerify { settingsRepository.setShowExcludedOnly(false) }
    }

    @Test
    fun `onExcludedChangedがリポジトリを呼び出しStateを更新する`() = runTest {
        val pkg = "com.example.app"
        val mockAllApps = listOf(App(pkg, "App"))
        coEvery { getAllAppsUseCase() } returns mockAllApps
        coEvery { getTodayUsedAppsUseCase() } returns emptyList()
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onExcludedChanged(pkg, true)
        advanceUntilIdle()

        coVerify { settingsRepository.setExcluded(pkg, true) }
        
        // Optimistic update check
        val state = viewModel.uiState.value
        val app = state.apps.find { it.packageName == pkg }
        assertTrue(app?.isExcluded == true)
    }
    
    // Add UiState tests here as well to maintain coverage
    @Test
    fun `ExclusionsUiState - filteredApps logic`() {
        val state = ExclusionsUiState(
            apps = listOf(
                jp.co.screentime.slackreporter.presentation.model.UiAppUsage("p1", "A1", null, 0, false),
                jp.co.screentime.slackreporter.presentation.model.UiAppUsage("p2", "A2", null, 0, true)
            ),
            showExcludedOnly = true
        )
        assertEquals(1, state.filteredApps.size)
        assertEquals("p2", state.filteredApps[0].packageName)
    }

    private fun createViewModel() = ExclusionsViewModel(
        context = context,
        getAllAppsUseCase = getAllAppsUseCase,
        getTodayUsedAppsUseCase = getTodayUsedAppsUseCase,
        settingsRepository = settingsRepository,
        appLabelResolver = appLabelResolver,
        ioDispatcher = testDispatcher
    )
}
