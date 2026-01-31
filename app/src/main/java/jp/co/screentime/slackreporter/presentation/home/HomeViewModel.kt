package jp.co.screentime.slackreporter.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.di.IoDispatcher
import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsageUseCase
import jp.co.screentime.slackreporter.domain.usecase.SendDailyReportUseCase
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import jp.co.screentime.slackreporter.presentation.model.UiAppUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getTodayUsageUseCase: GetTodayUsageUseCase,
    private val sendDailyReportUseCase: SendDailyReportUseCase,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val appLabelResolver: AppLabelResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val TOP_APPS_COUNT = 5
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
        observeSendResult()
    }

    /**
     * データを読み込む
     */
    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Usage Access権限チェック
            val hasAccess = usageRepository.isUsageAccessGranted()
            if (!hasAccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasUsageAccess = false
                    )
                }
                return@launch
            }

            try {
                // 除外適用 - settingsの変更を監視し、毎回最新のusageListを取得
                settingsRepository.settingsFlow.collectLatest { currentSettings ->
                    // Bug fix: usageListを毎回取得して最新データを反映
                    val usageList = getTodayUsageUseCase()
                    
                    val (topApps, totalDurationMillis, otherDurationMillis) = withContext(ioDispatcher) {
                        val filteredUsage = usageList.filter { usage ->
                            usage.packageName !in currentSettings.excludedPackages &&
                                usage.packageName != context.packageName
                        }

                        val topApps = filteredUsage.take(TOP_APPS_COUNT).map { usage ->
                            UiAppUsage(
                                packageName = usage.packageName,
                                appName = appLabelResolver.getAppLabel(usage.packageName),
                                icon = appLabelResolver.getAppIcon(usage.packageName),
                                durationMinutes = usage.durationMinutes,
                                isExcluded = false
                            )
                        }

                        val otherDurationMillis = filteredUsage
                            .drop(TOP_APPS_COUNT)
                            .sumOf { it.durationMillis }

                        val totalDurationMillis = filteredUsage.sumOf { it.durationMillis }

                        Triple(topApps, totalDurationMillis, otherDurationMillis)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasUsageAccess = true,
                            totalMinutes = millisToMinutes(totalDurationMillis),
                            topApps = topApps,
                            otherMinutes = millisToMinutes(otherDurationMillis)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sendError = e.message
                    )
                }
            }
        }
    }

    /**
     * 送信結果を監視
     */
    private fun observeSendResult() {
        viewModelScope.launch {
            settingsRepository.sendResultFlow.collect { result ->
                val formattedTime = result.lastSentEpochMillis?.let { epochMillis ->
                    val dateFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
                    dateFormat.format(Date(epochMillis))
                }

                _uiState.update {
                    it.copy(
                        sendStatus = result.status,
                        lastSentTimeFormatted = formattedTime,
                        sendError = result.errorMessage
                    )
                }
            }
        }
    }

    /**
     * 今すぐ送信
     */
    fun onClickSendNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, sendError = null) }

            val result = sendDailyReportUseCase()

            _uiState.update {
                it.copy(
                    isSending = false,
                    sendStatus = result.status,
                    sendError = result.errorMessage
                )
            }
        }
    }

    /**
     * Usage Access権限を更新した後にリロード
     */
    fun onUsageAccessGranted() {
        loadData()
    }

    private fun millisToMinutes(durationMillis: Long): Int {
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    }
}
