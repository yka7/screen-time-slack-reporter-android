package jp.co.screentime.slackreporter.presentation.exclusions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.screentime.slackreporter.R
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.di.IoDispatcher
import jp.co.screentime.slackreporter.domain.usecase.GetAllAppsUseCase
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsedAppsUseCase
import jp.co.screentime.slackreporter.platform.AppLabelResolver
import jp.co.screentime.slackreporter.presentation.model.UiAppUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExclusionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAllAppsUseCase: GetAllAppsUseCase,
    private val getTodayUsedAppsUseCase: GetTodayUsedAppsUseCase,
    private val settingsRepository: SettingsRepository,
    private val appLabelResolver: AppLabelResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExclusionsUiState())
    val uiState: StateFlow<ExclusionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val allApps = getAllAppsUseCase().filter { it.packageName != context.packageName }
                val usageList = getTodayUsedAppsUseCase()
                val usageMap = usageList.associateBy { it.packageName }

                combine(
                    settingsRepository.settingsFlow,
                    settingsRepository.showExcludedOnlyFlow
                ) { settings, showExcludedOnly ->
                    Pair(settings, showExcludedOnly)
                }.collectLatest { (settings, showExcludedOnly) ->
                    val apps = withContext(ioDispatcher) {
                        allApps.map { app ->
                            val usage = usageMap[app.packageName]
                            UiAppUsage(
                                packageName = app.packageName,
                                appName = app.appName,
                                icon = appLabelResolver.getAppIcon(app.packageName),
                                durationMinutes = usage?.durationMinutes ?: 0,
                                isExcluded = app.packageName in settings.excludedPackages
                            )
                        }.sortedWith(compareByDescending<UiAppUsage> { it.durationMinutes }.thenBy { it.appName })
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = apps,
                            showExcludedOnly = showExcludedOnly,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: context.getString(R.string.exclusions_load_failed)
                    )
                }
            }
        }
    }

    fun onShowExcludedOnlyChanged(showExcludedOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowExcludedOnly(showExcludedOnly)
        }
    }

    fun onExcludedChanged(packageName: String, excluded: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExcluded(packageName, excluded)

            _uiState.update { state ->
                state.copy(
                    apps = state.apps.map { app ->
                        if (app.packageName == packageName) {
                            app.copy(isExcluded = excluded)
                        } else {
                            app
                        }
                    }
                )
            }
        }
    }

    fun onShowAllApps() {
        onShowExcludedOnlyChanged(false)
    }
}
