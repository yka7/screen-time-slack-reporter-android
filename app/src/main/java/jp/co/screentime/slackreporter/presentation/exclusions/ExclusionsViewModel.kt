package jp.co.screentime.slackreporter.presentation.exclusions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.screentime.slackreporter.data.repository.SettingsRepository
import jp.co.screentime.slackreporter.domain.usecase.GetAllAppsUseCase
import jp.co.screentime.slackreporter.domain.usecase.GetTodayUsedAppsUseCase
import jp.co.screentime.slackreporter.presentation.model.UiAppUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExclusionsViewModel @Inject constructor(
    private val getAllAppsUseCase: GetAllAppsUseCase,
    private val getTodayUsedAppsUseCase: GetTodayUsedAppsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExclusionsUiState())
    val uiState: StateFlow<ExclusionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val allApps = getAllAppsUseCase()
                val usageList = getTodayUsedAppsUseCase()
                val usageMap = usageList.associateBy { it.packageName }

                combine(
                    settingsRepository.settingsFlow,
                    settingsRepository.showExcludedOnlyFlow
                ) { settings, showExcludedOnly ->
                    val apps = allApps.map { app ->
                        val usage = usageMap[app.packageName]
                        UiAppUsage(
                            packageName = app.packageName,
                            appName = app.appName,
                            icon = app.icon,
                            durationMinutes = usage?.durationMinutes ?: 0,
                            isExcluded = app.packageName in settings.excludedPackages
                        )
                    }.sortedWith(compareByDescending<UiAppUsage> { it.durationMinutes }.thenBy { it.appName })

                    Pair(apps, showExcludedOnly)
                }.collect { (apps, showExcludedOnly) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = apps,
                            showExcludedOnly = showExcludedOnly
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
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
