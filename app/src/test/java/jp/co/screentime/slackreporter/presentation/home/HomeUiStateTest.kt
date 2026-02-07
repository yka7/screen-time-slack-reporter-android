package jp.co.screentime.slackreporter.presentation.home

import jp.co.screentime.slackreporter.presentation.model.UiAppUsage
import org.junit.Assert.*
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `hasUsage - returns false when totalMinutes is 0 and topApps is empty`() {
        val state = HomeUiState(
            totalMinutes = 0,
            topApps = emptyList()
        )
        assertFalse(state.hasUsage)
    }

    @Test
    fun `hasUsage - returns true when totalMinutes is greater than 0`() {
        val state = HomeUiState(
            totalMinutes = 10,
            topApps = emptyList()
        )
        assertTrue(state.hasUsage)
    }

    @Test
    fun `hasUsage - returns true when topApps is not empty`() {
        val state = HomeUiState(
            totalMinutes = 0,
            topApps = listOf(
                UiAppUsage(
                    packageName = "com.example.app",
                    appName = "Example App",
                    icon = null,
                    durationMinutes = 10,
                    isExcluded = false
                )
            )
        )
        assertTrue(state.hasUsage)
    }

    @Test
    fun `hasUsage - returns true when both conditions are met`() {
        val state = HomeUiState(
            totalMinutes = 10,
            topApps = listOf(
                UiAppUsage(
                    packageName = "com.example.app",
                    appName = "Example App",
                    icon = null,
                    durationMinutes = 10,
                    isExcluded = false
                )
            )
        )
        assertTrue(state.hasUsage)
    }
}
