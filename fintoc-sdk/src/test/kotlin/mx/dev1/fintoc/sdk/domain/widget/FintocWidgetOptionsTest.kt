package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocWidgetOptionsTest {

    @Test
    fun `each subtype reports its own product`() {
        assertEquals(FintocProduct.PAYMENTS, FintocWidgetOptions.Payments(sessionToken = "cs_token").product)
        assertEquals(
            FintocProduct.MOVEMENTS,
            FintocWidgetOptions.Movements(holderType = FintocHolderType.INDIVIDUAL).product,
        )
        assertEquals(
            FintocProduct.SUBSCRIPTIONS,
            FintocWidgetOptions.Subscriptions(widgetToken = "wt_token", holderType = FintocHolderType.BUSINESS).product,
        )
    }

    @Test
    fun `payments rejects blank whitespace and secret key tokens`() {
        assertThrows(IllegalArgumentException::class.java) { FintocWidgetOptions.Payments(sessionToken = "") }
        assertThrows(IllegalArgumentException::class.java) { FintocWidgetOptions.Payments(sessionToken = "cs a") }
        assertThrows(IllegalArgumentException::class.java) { FintocWidgetOptions.Payments(sessionToken = "sk_live_x") }
    }

    @Test
    fun `secret key rejection never repeats the token`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            FintocWidgetOptions.Payments(sessionToken = "sk_live_leaked-value")
        }

        assertFalse(failure.message.orEmpty().contains("leaked-value"))
    }

    @Test
    fun `movements defaults to chile with no link and no webhook`() {
        val options = FintocWidgetOptions.Movements(holderType = FintocHolderType.INDIVIDUAL)

        assertEquals(FintocCountry.CHILE, options.country)
        assertEquals(null, options.linkToken)
        assertEquals(null, options.webhookUrl)
    }

    @Test
    fun `movements validates the link token`() {
        assertThrows(IllegalArgumentException::class.java) {
            FintocWidgetOptions.Movements(holderType = FintocHolderType.INDIVIDUAL, linkToken = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FintocWidgetOptions.Movements(holderType = FintocHolderType.INDIVIDUAL, linkToken = "sk_test_x")
        }
    }

    @Test
    fun `movements accepts only absolute https webhook urls`() {
        FintocWidgetOptions.Movements(
            holderType = FintocHolderType.BUSINESS,
            webhookUrl = "https://example.com/fintoc/webhook",
        )

        listOf(
            "http://example.com/webhook",
            "example.com/webhook",
            "/webhook",
            "https://",
            "https:///webhook",
            "https://exa mple.com",
            "javascript:alert(1)",
            "",
        ).forEach { invalidUrl ->
            assertThrows("Expected '$invalidUrl' to be rejected", IllegalArgumentException::class.java) {
                FintocWidgetOptions.Movements(holderType = FintocHolderType.BUSINESS, webhookUrl = invalidUrl)
            }
        }
    }

    @Test
    fun `subscriptions validates the widget token and defaults to chile`() {
        assertThrows(IllegalArgumentException::class.java) {
            FintocWidgetOptions.Subscriptions(widgetToken = "", holderType = FintocHolderType.INDIVIDUAL)
        }

        val options = FintocWidgetOptions.Subscriptions(
            widgetToken = "wt_token",
            holderType = FintocHolderType.INDIVIDUAL,
        )

        assertEquals(FintocCountry.CHILE, options.country)
    }

    @Test
    fun `copy validates the new values too`() {
        val options = FintocWidgetOptions.Payments(sessionToken = "cs_token")

        assertThrows(IllegalArgumentException::class.java) { options.copy(sessionToken = "sk_live_x") }
    }

    @Test
    fun `toString never exposes tokens or webhook urls`() {
        val payments = FintocWidgetOptions.Payments(sessionToken = "cs_session-secret").toString()
        val movements = FintocWidgetOptions.Movements(
            holderType = FintocHolderType.INDIVIDUAL,
            country = FintocCountry.MEXICO,
            linkToken = "link_link-secret",
            webhookUrl = "https://example.com/hook?key=webhook-secret",
        ).toString()
        val movementsWithoutOptionals = FintocWidgetOptions.Movements(holderType = FintocHolderType.BUSINESS).toString()
        val subscriptions = FintocWidgetOptions.Subscriptions(
            widgetToken = "wt_widget-secret",
            holderType = FintocHolderType.BUSINESS,
        ).toString()

        listOf("session-secret", "link-secret", "webhook-secret", "widget-secret").forEach { secret ->
            assertFalse(listOf(payments, movements, subscriptions).any { description -> description.contains(secret) })
        }
        assertTrue(payments.contains("<redacted>"))
        assertTrue(movements.contains("MEXICO"))
        assertTrue(movementsWithoutOptionals.contains("linkToken=null"))
        assertTrue(subscriptions.contains("BUSINESS"))
    }
}
