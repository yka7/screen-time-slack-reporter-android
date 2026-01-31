package jp.co.screentime.slackreporter.data.slack

import org.junit.Assert.*
import org.junit.Test

class SlackWebhookValidatorTest {
    
    @Test
    fun `validate returns success for valid Slack webhook URL`() {
        val validUrl = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"
        
        val result = SlackWebhookValidator.validate(validUrl)
        
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }
    
    @Test
    fun `validate returns failure for empty URL`() {
        val result = SlackWebhookValidator.validate("")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate returns failure for non-Slack URL`() {
        val result = SlackWebhookValidator.validate("https://example.com/webhook")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate returns failure for invalid URL format`() {
        val result = SlackWebhookValidator.validate("not-a-url")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate returns failure for HTTP URL`() {
        val result = SlackWebhookValidator.validate("http://hooks.slack.com/services/xxx")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate returns failure for URL with wrong path`() {
        val result = SlackWebhookValidator.validate("https://hooks.slack.com/wrong/path")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate trims whitespace from URL`() {
        val validUrl = "  https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX  "
        
        val result = SlackWebhookValidator.validate(validUrl)
        
        assertTrue(result.isSuccess)
    }
}
