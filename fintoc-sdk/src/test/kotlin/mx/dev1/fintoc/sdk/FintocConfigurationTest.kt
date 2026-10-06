package mx.dev1.fintoc.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocConfigurationTest {

    @Test
    fun `test public key selects the test environment`() {
        val configuration = FintocConfiguration(publicKey = "pk_test_abc123")

        assertEquals(FintocEnvironment.TEST, configuration.environment)
    }

    @Test
    fun `live public key selects the live environment`() {
        val configuration = FintocConfiguration(publicKey = "pk_live_abc123")

        assertEquals(FintocEnvironment.LIVE, configuration.environment)
    }

    @Test
    fun `blank public key is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { FintocConfiguration(publicKey = "   ") }
    }

    @Test
    fun `public key with whitespace is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { FintocConfiguration(publicKey = "pk_test_ab c") }
        assertThrows(IllegalArgumentException::class.java) { FintocConfiguration(publicKey = " pk_test_abc") }
    }

    @Test
    fun `secret key is rejected without repeating it in the message`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            FintocConfiguration(publicKey = "sk_live_super-secret-value")
        }

        assertTrue(failure.message.orEmpty().contains("secret key"))
        assertFalse(failure.message.orEmpty().contains("super-secret-value"))
    }

    @Test
    fun `key without a known prefix is rejected without repeating it in the message`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            FintocConfiguration(publicKey = "token-123456")
        }

        assertTrue(failure.message.orEmpty().contains("pk_test_"))
        assertFalse(failure.message.orEmpty().contains("token-123456"))
    }

    @Test
    fun `language follows the device unless one is chosen`() {
        assertEquals(null, FintocConfiguration(publicKey = "pk_test_abc123").language)
        assertEquals(
            FintocLanguage.FRENCH,
            FintocConfiguration(publicKey = "pk_test_abc123", language = FintocLanguage.FRENCH).language,
        )
    }

    @Test
    fun `language tags match the supported languages`() {
        assertEquals(
            listOf("en", "es", "fr", "pt"),
            FintocLanguage.entries.map { language -> language.languageTag },
        )
    }

    @Test
    fun `toString shows the language but never the public key`() {
        assertTrue(FintocConfiguration(publicKey = "pk_test_abc123").toString().contains("language=automatic"))
        assertTrue(
            FintocConfiguration(publicKey = "pk_test_abc123", language = FintocLanguage.SPANISH)
                .toString()
                .contains("language=SPANISH"),
        )
    }

    @Test
    fun `toString never exposes the public key`() {
        val description = FintocConfiguration(publicKey = "pk_live_visible-nowhere").toString()

        assertFalse(description.contains("visible-nowhere"))
        assertTrue(description.contains("<redacted>"))
        assertTrue(description.contains("LIVE"))
    }
}
