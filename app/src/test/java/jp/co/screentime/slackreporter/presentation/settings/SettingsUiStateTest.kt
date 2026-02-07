package jp.co.screentime.slackreporter.presentation.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsUiStateTest {

    @Test
    fun `isWebhookConfigured - returns false for empty url`() {
        val state = SettingsUiState(webhookUrl = "")
        assertFalse(state.isWebhookConfigured)
    }

    @Test
    fun `isWebhookConfigured - returns false for blank url`() {
        val state = SettingsUiState(webhookUrl = "   ")
        assertFalse(state.isWebhookConfigured)
    }

    @Test
    fun `isWebhookConfigured - returns true for non-blank url`() {
        val state = SettingsUiState(webhookUrl = "https://hooks.slack.com/...")
        assertTrue(state.isWebhookConfigured)
    }

    @Test
    fun `formattedSendTime - formats correctly`() {
        val state = SettingsUiState(sendHour = 9, sendMinute = 5)
        assertEquals("09:05", state.formattedSendTime)
    }

    @Test
    fun `hasUnsavedChanges - returns false when no changes`() {
        val state = SettingsUiState(
            webhookUrl = "url",
            initialWebhookUrl = "url",
            sendEnabled = true,
            initialSendEnabled = true,
            sendHour = 10,
            initialSendHour = 10,
            sendMinute = 30,
            initialSendMinute = 30
        )
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges - returns true when webhookUrl changed`() {
        val state = SettingsUiState(
            webhookUrl = "new_url",
            initialWebhookUrl = "old_url"
        )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges - returns true when sendEnabled changed`() {
        val state = SettingsUiState(
            sendEnabled = true,
            initialSendEnabled = false
        )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges - returns true when time changed`() {
        val state = SettingsUiState(
            sendHour = 11,
            initialSendHour = 10
        )
        assertTrue(state.hasUnsavedChanges)
    }
}
