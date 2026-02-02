package jp.co.screentime.slackreporter.presentation.exclusions

import jp.co.screentime.slackreporter.presentation.model.UiAppUsage
import org.junit.Assert.*
import org.junit.Test

/**
 * ExclusionsUiStateのテスト
 */
class ExclusionsUiStateTest {

    private fun createTestApp(packageName: String, isExcluded: Boolean, durationMinutes: Int = 30) = UiAppUsage(
        packageName = packageName,
        appName = packageName.substringAfterLast('.'),
        icon = null,
        durationMinutes = durationMinutes,
        isExcluded = isExcluded
    )

    @Test
    fun `ExclusionsUiState - デフォルト値が正しい`() {
        val state = ExclusionsUiState()
        
        assertTrue(state.isLoading)
        assertTrue(state.apps.isEmpty())
        assertFalse(state.showExcludedOnly)
        assertNull(state.errorMessage)
    }

    @Test
    fun `ExclusionsUiState - isLoadingをfalseに設定`() {
        val state = ExclusionsUiState(isLoading = false)
        
        assertFalse(state.isLoading)
    }

    @Test
    fun `ExclusionsUiState - showExcludedOnlyをtrueに設定`() {
        val state = ExclusionsUiState(showExcludedOnly = true)
        
        assertTrue(state.showExcludedOnly)
    }

    @Test
    fun `ExclusionsUiState - errorMessageを設定`() {
        val errorMessage = "エラーが発生しました"
        val state = ExclusionsUiState(errorMessage = errorMessage)
        
        assertEquals(errorMessage, state.errorMessage)
    }

    @Test
    fun `filteredApps - showExcludedOnlyがfalseの場合は全アプリを返す`() {
        val apps = listOf(
            createTestApp("com.app1", isExcluded = false),
            createTestApp("com.app2", isExcluded = true),
            createTestApp("com.app3", isExcluded = false)
        )
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = false)
        
        assertEquals(3, state.filteredApps.size)
    }

    @Test
    fun `filteredApps - showExcludedOnlyがtrueの場合は除外アプリのみ返す`() {
        val apps = listOf(
            createTestApp("com.app1", isExcluded = false),
            createTestApp("com.app2", isExcluded = true),
            createTestApp("com.app3", isExcluded = false)
        )
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = true)
        
        assertEquals(1, state.filteredApps.size)
        assertTrue(state.filteredApps.all { it.isExcluded })
    }

    @Test
    fun `isEmpty - フィルタ後のアプリが空の場合はtrue`() {
        val state = ExclusionsUiState(apps = emptyList())
        
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty - フィルタ後のアプリがある場合はfalse`() {
        val apps = listOf(createTestApp("com.app1", isExcluded = false))
        val state = ExclusionsUiState(apps = apps)
        
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty - showExcludedOnlyで除外アプリがない場合はtrue`() {
        val apps = listOf(
            createTestApp("com.app1", isExcluded = false),
            createTestApp("com.app2", isExcluded = false)
        )
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = true)
        
        assertTrue(state.isEmpty)
    }

    @Test
    fun `hasNoUsageToday - アプリリストが空の場合はtrue`() {
        val state = ExclusionsUiState(apps = emptyList())
        
        assertTrue(state.hasNoUsageToday)
    }

    @Test
    fun `hasNoUsageToday - アプリリストがある場合はfalse`() {
        val apps = listOf(createTestApp("com.app1", isExcluded = false))
        val state = ExclusionsUiState(apps = apps)
        
        assertFalse(state.hasNoUsageToday)
    }

    @Test
    fun `hasNoExcludedApps - showExcludedOnlyがtrueで除外アプリがない場合はtrue`() {
        val apps = listOf(
            createTestApp("com.app1", isExcluded = false),
            createTestApp("com.app2", isExcluded = false)
        )
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = true)
        
        assertTrue(state.hasNoExcludedApps)
    }

    @Test
    fun `hasNoExcludedApps - showExcludedOnlyがtrueで除外アプリがある場合はfalse`() {
        val apps = listOf(
            createTestApp("com.app1", isExcluded = false),
            createTestApp("com.app2", isExcluded = true)
        )
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = true)
        
        assertFalse(state.hasNoExcludedApps)
    }

    @Test
    fun `hasNoExcludedApps - showExcludedOnlyがfalseの場合はfalse`() {
        val apps = listOf(createTestApp("com.app1", isExcluded = false))
        val state = ExclusionsUiState(apps = apps, showExcludedOnly = false)
        
        assertFalse(state.hasNoExcludedApps)
    }

    @Test
    fun `ExclusionsUiState - 等価性テスト`() {
        val state1 = ExclusionsUiState(isLoading = false, showExcludedOnly = true)
        val state2 = ExclusionsUiState(isLoading = false, showExcludedOnly = true)
        
        assertEquals(state1, state2)
    }

    @Test
    fun `ExclusionsUiState - 非等価性テスト`() {
        val state1 = ExclusionsUiState(isLoading = true)
        val state2 = ExclusionsUiState(isLoading = false)
        
        assertNotEquals(state1, state2)
    }
}
