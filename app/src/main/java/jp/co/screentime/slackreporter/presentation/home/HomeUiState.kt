package jp.co.screentime.slackreporter.presentation.home

import jp.co.screentime.slackreporter.domain.model.SendStatus
import jp.co.screentime.slackreporter.presentation.model.UiAppUsage

/**
 * ホーム画面のUI状態
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = true,
    val totalMinutes: Int = 0,
    val topApps: List<UiAppUsage> = emptyList(),
    val otherMinutes: Int = 0,
    val sendStatus: SendStatus = SendStatus.NOT_SENT,
    val lastSentTimeFormatted: String? = null,
    val isSending: Boolean = false,
    val sendError: String? = null
) {
    /**
     * 利用があるかどうか
     */
    val hasUsage: Boolean
        get() = totalMinutes > 0 || topApps.isNotEmpty()
}
