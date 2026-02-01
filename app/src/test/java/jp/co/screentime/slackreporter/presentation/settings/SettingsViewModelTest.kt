package jp.co.screentime.slackreporter.presentation.settings

import android.content.Context
import io.mockk.*
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.SlackRepository
import jp.co.screentime.slackreporter.domain.model.AppSettings
import jp.co.screentime.slackreporter.workers.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var slackRepository: SlackRepository
    private lateinit var workScheduler: WorkScheduler

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
            every { getString(R.string.settings_webhook_not_set) } returns "Webhook URL is not set"
            every { getString(R.string.settings_webhook_invalid) } returns "Invalid Webhook URL"
            every { getString(R.string.common_unknown_error) } returns "Unknown error"
        }
        settingsRepository = mockk(relaxed = true) {
            every { settingsFlow } returns flowOf(testSettings)
        }
        slackRepository = mockk()
        workScheduler = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期化時に設定がロードされる`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("https://hooks.slack.com/services/xxx", state.webhookUrl)
        assertTrue(state.sendEnabled)
        assertEquals(21, state.sendHour)
        assertEquals(0, state.sendMinute)
    }

    @Test
    fun `Webhook URL変更が反映される`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWebhookUrlChanged("https://hooks.slack.com/services/new")
        
        assertEquals("https://hooks.slack.com/services/new", viewModel.uiState.value.webhookUrl)
        assertNull(viewModel.uiState.value.webhookError)
    }

    @Test
    fun `送信有効フラグの切り替えが反映される`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSendEnabledChanged(false)
        
        assertFalse(viewModel.uiState.value.sendEnabled)
    }

    @Test
    fun `送信時刻の変更が反映される`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSendTimeChanged(22, 30)
        
        assertEquals(22, viewModel.uiState.value.sendHour)
        assertEquals(30, viewModel.uiState.value.sendMinute)
    }

    @Test
    fun `有効な設定で保存が成功する`() = runTest {
        coEvery { settingsRepository.setWebhookUrl(any()) } just Runs
        coEvery { settingsRepository.setSendEnabled(any()) } just Runs
        coEvery { settingsRepository.setSendTime(any(), any()) } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveSettings()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { settingsRepository.setWebhookUrl("https://hooks.slack.com/services/xxx") }
        coVerify { settingsRepository.setSendEnabled(true) }
        coVerify { settingsRepository.setSendTime(21, 0) }
        coVerify { workScheduler.scheduleOrUpdateDailyWorker(21, 0) }
    }

    @Test
    fun `送信無効時はWorkerがキャンセルされる`() = runTest {
        coEvery { settingsRepository.setWebhookUrl(any()) } just Runs
        coEvery { settingsRepository.setSendEnabled(any()) } just Runs
        coEvery { settingsRepository.setSendTime(any(), any()) } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSendEnabledChanged(false)
        viewModel.onSaveSettings()
        advanceUntilIdle()

        coVerify { workScheduler.cancelDailyWorker() }
    }

    @Test
    fun `テスト送信が成功する`() = runTest {
        coEvery { slackRepository.sendTestMessage(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClickTestWebhook()
        advanceUntilIdle()

        assertEquals(TestResult.Success, viewModel.uiState.value.testResult)
        assertFalse(viewModel.uiState.value.isTesting)
    }

    @Test
    fun `テスト送信が失敗する`() = runTest {
        coEvery { slackRepository.sendTestMessage(any()) } returns Result.failure(Exception("Network error"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClickTestWebhook()
        advanceUntilIdle()

        val result = viewModel.uiState.value.testResult
        assertTrue(result is TestResult.Failure)
        assertEquals("Network error", (result as TestResult.Failure).message)
    }

    @Test
    fun `未保存の変更があるかどうかを判定できる`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onWebhookUrlChanged("https://hooks.slack.com/services/new")

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `テスト結果をクリアできる`() = runTest {
        coEvery { slackRepository.sendTestMessage(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClickTestWebhook()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.testResult)

        viewModel.clearTestResult()

        assertNull(viewModel.uiState.value.testResult)
    }

    @Test
    fun `保存済みフラグをクリアできる`() = runTest {
        coEvery { settingsRepository.setWebhookUrl(any()) } just Runs
        coEvery { settingsRepository.setSendEnabled(any()) } just Runs
        coEvery { settingsRepository.setSendTime(any(), any()) } just Runs

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveSettings()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)

        viewModel.clearSavedFlag()

        assertFalse(viewModel.uiState.value.isSaved)
    }

    private fun createViewModel() = SettingsViewModel(
        context = context,
        settingsRepository = settingsRepository,
        slackRepository = slackRepository,
        workScheduler = workScheduler
    )
}
