package jp.co.screentime.slackreporter.presentation.exclusions

import jp.co.screentime.slackreporter.presentation.model.UiAppUsage

/**
 * 対象外アプリ画面のUI状態
 */
data class ExclusionsUiState(
    val isLoading: Boolean = true,
    val apps: List<UiAppUsage> = emptyList(),
    val showExcludedOnly: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * フィルタ適用後のアプリ一覧
     */
    val filteredApps: List<UiAppUsage>
        get() = if (showExcludedOnly) {
            apps.filter { it.isExcluded }
        } else {
            apps
        }

    /**
     * 表示するアプリがない
     */
    val isEmpty: Boolean
        get() = filteredApps.isEmpty()

    /**
     * 今日使ったアプリがない（フィルタ関係なく）
     */
    val hasNoUsageToday: Boolean
        get() = apps.isEmpty()

    /**
     * 対象外アプリがない（フィルタON時の空状態）
     */
    val hasNoExcludedApps: Boolean
        get() = showExcludedOnly && apps.none { it.isExcluded }
}
