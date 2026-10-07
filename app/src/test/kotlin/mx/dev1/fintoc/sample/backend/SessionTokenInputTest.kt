package mx.dev1.fintoc.sample.backend

import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokenInputTest {

    @Test
    fun `a pasted token becomes payments options, without the whitespace around it`() {
        assertEquals(
            FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456"),
            parsePaymentsOptions("  cs_123_sec_456\n"),
        )
    }

    @Test
    fun `values that cannot be a session token are refused`() {
        listOf("", "   ", "cs_123 sec_456", "sk_live_secret_key", "sk_test_secret").forEach { pasted ->
            assertNull(pasted, parsePaymentsOptions(pasted))
        }
    }
}
