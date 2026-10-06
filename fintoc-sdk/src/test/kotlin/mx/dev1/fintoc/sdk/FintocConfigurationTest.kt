package mx.dev1.fintoc.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocConfigurationTest {

    @Test
    fun `base url defaults to the production endpoint`() {
        val configuration = FintocConfiguration(authToken = "token")

        assertEquals("https://api.fintoc.com", configuration.baseUrl)
    }

    @Test
    fun `custom base url is preserved`() {
        val configuration = FintocConfiguration(authToken = "token", baseUrl = "https://sandbox.example.com")

        assertEquals("https://sandbox.example.com", configuration.baseUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank auth token is rejected`() {
        FintocConfiguration(authToken = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank base url is rejected`() {
        FintocConfiguration(authToken = "token", baseUrl = "")
    }

    @Test
    fun `toString never exposes the auth token`() {
        val configuration = FintocConfiguration(authToken = "super-secret-token")

        val description = configuration.toString()

        assertFalse(description.contains("super-secret-token"))
        assertTrue(description.contains("<redacted>"))
    }
}
