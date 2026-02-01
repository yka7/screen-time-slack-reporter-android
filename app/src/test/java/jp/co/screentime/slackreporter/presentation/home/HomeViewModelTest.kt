package jp.co.screentime.slackreporter.presentation.home

import android.content.Context
import io.mockk.*
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.domain.model.AppSettings
import jp.co.screentime.slackreporter.domain.model.AppUsage
import jp.co.screentime.slackreporter.domain.model.SendResult
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsageUseCase
import jp.co.screentime.slackreporter.domain.usecase.SendDailyReportUseCase
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var getTodayUsageUseCase: GetTodayUsageUseCase
    private lateinit var sendDailyReportUseCase: SendDailyReportUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var usageRepository: UsageRepository
    private lateinit var appLabelResolver: AppLabelResolver

    private val testSettings = AppSettings(
        webhookUrl = "https://hooks.slack.com/services/xxx",
        sendEnabled = true,
        sendHour = 21,
        sendMinute = 0,
        excludedPackages = emptySet()
    )

    private val testSendResult = SendResult(
        status = SendStatus.NOT_SENT,
        lastSentEpochMillis = null,
        errorMessage = null
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true) {
            every { packageName } returns "jp.co.screentime.slackreporter"
        }
        getTodayUsageUseCase = mockk()
        sendDailyReportUseCase = mockk()
        settingsRepository = mockk {
            every { settingsFlow } returns flowOf(testSettings)
            every { sendResultFlow } returns flowOf(testSendResult)
        }
        usageRepository = mockk()
        appLabelResolver = mockk {
            every { getAppLabel(any()) } answers { firstArg<String>().substringAfterLast('.') }
            every { getAppIcon(any()) } returns null
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Usage Access権限がない場合はhasUsageAccess=falseになる`() = runTest {
        every { usageRepository.isUsageAccessGranted() } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasUsageAccess)
        assertFalse(state.isLoading)
    }

    @Test
    fun `Usage Access権限がある場合はデータをロードする`() = runTest {
        val mockUsage = listOf(
            AppUsage("com.youtube.android", 1800000L),
            AppUsage("com.chrome.android", 900000L)
        )
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { getTodayUsageUseCase() } returns mockUsage

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasUsageAccess)
        assertFalse(state.isLoading)
        assertEquals(2, state.topApps.size)
        assertEquals(45, state.totalMinutes) // 1800000 + 900000 = 2700000ms = 45min
    }

    @Test
    fun `手動送信が成功する`() = runTest {
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { getTodayUsageUseCase() } returns emptyList()
        coEvery { sendDailyReportUseCase() } returns SendResult(
            status = SendStatus.SUCCESS,
            lastSentEpochMillis = System.currentTimeMillis(),
            errorMessage = null
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClickSendNow()
        advanceUntilIdle()

        coVerify { sendDailyReportUseCase() }
        assertEquals(SendStatus.SUCCESS, viewModel.uiState.value.sendStatus)
        assertNull(viewModel.uiState.value.sendError)
    }

    @Test
    fun `手動送信が失敗する`() = runTest {
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { getTodayUsageUseCase() } returns emptyList()
        coEvery { sendDailyReportUseCase() } returns SendResult(
            status = SendStatus.FAILED,
            lastSentEpochMillis = null,
            errorMessage = "Network error"
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClickSendNow()
        advanceUntilIdle()

        assertEquals(SendStatus.FAILED, viewModel.uiState.value.sendStatus)
        assertEquals("Network error", viewModel.uiState.value.sendError)
    }

    @Test
    fun `除外アプリがフィルタリングされる`() = runTest {
        val settingsWithExclusion = testSettings.copy(
            excludedPackages = setOf("com.excluded.app")
        )
        every { settingsRepository.settingsFlow } returns flowOf(settingsWithExclusion)

        val mockUsage = listOf(
            AppUsage("com.youtube.android", 1800000L),
            AppUsage("com.excluded.app", 900000L)
        )
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { getTodayUsageUseCase() } returns mockUsage

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.topApps.size)
        assertEquals("com.youtube.android", state.topApps[0].packageName)
    }

    @Test
    fun `onUsageAccessGrantedでデータがリロードされる`() = runTest {
        every { usageRepository.isUsageAccessGranted() } returns true
        coEvery { getTodayUsageUseCase() } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUsageAccessGranted()
        advanceUntilIdle()

        coVerify(atLeast = 2) { getTodayUsageUseCase() }
    }

    private fun createViewModel() = HomeViewModel(
        context = context,
        getTodayUsageUseCase = getTodayUsageUseCase,
        sendDailyReportUseCase = sendDailyReportUseCase,
        settingsRepository = settingsRepository,
        usageRepository = usageRepository,
        appLabelResolver = appLabelResolver,
        ioDispatcher = testDispatcher
    )
}
