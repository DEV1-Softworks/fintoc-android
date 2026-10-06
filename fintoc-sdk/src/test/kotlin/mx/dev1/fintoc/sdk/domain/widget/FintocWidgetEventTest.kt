package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocWidgetEventTest {

    @Test
    fun `link intent result rejects a blank exchange token`() {
        assertThrows(IllegalArgumentException::class.java) { FintocLinkIntentResult(exchangeToken = "  ") }
    }

    @Test
    fun `link intent result hides the exchange token in toString`() {
        val description = FintocLinkIntentResult(exchangeToken = "et_super-secret", id = "li_1").toString()

        assertFalse(description.contains("super-secret"))
        assertTrue(description.contains("<redacted>"))
        assertTrue(description.contains("li_1"))
    }

    @Test
    fun `succeeded hides the exchange token through the link intent`() {
        val event = FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_secret-token"))

        val description = event.toString()

        assertFalse(description.contains("secret-token"))
    }

    @Test
    fun `occurred toString lists metadata keys but never their values`() {
        val description = FintocWidgetEvent.Occurred(
            name = "on_error",
            timestampMillis = 5L,
            metadata = mapOf("reason" to "private-detail"),
        ).toString()

        assertFalse(description.contains("private-detail"))
        assertTrue(description.contains("reason"))
        assertTrue(description.contains("on_error"))
    }

    @Test
    fun `occurred type is null for an unknown name and set for a known one`() {
        assertNull(FintocWidgetEvent.Occurred(name = "something_new").type)
        assertEquals(FintocWidgetEventType.PAYMENT_CREATED, FintocWidgetEvent.Occurred(name = "payment_created").type)
    }

    @Test
    fun `event type codes are unique and snake case`() {
        val codes = FintocWidgetEventType.entries.map { type -> type.code }

        assertEquals(codes.size, codes.toSet().size)
        codes.forEach { code -> assertTrue(code, Regex("[a-z]+(_[a-z]+)*").matches(code)) }
    }

    @Test
    fun `all documented events are modeled`() {
        assertEquals(29, FintocWidgetEventType.entries.size)
    }
}
