package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FintocWidgetUrlBuilderTest {

    private val publicKey = "pk_test_abc123"

    @Test
    fun `payments url carries only the session token`() {
        val url = FintocWidgetUrlBuilder.build(publicKey, FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456"))

        assertEquals(
            "https://webview.fintoc.com/widget.html" +
                "?public_key=pk_test_abc123&product=payments&session_token=cs_123_sec_456",
            url,
        )
    }

    @Test
    fun `payments url never sends holder type or country`() {
        val url = FintocWidgetUrlBuilder.build(publicKey, FintocWidgetOptions.Payments(sessionToken = "cs_token"))

        assertFalse(url.contains("holder_type"))
        assertFalse(url.contains("country"))
    }

    @Test
    fun `movements url without optionals sends holder type and country`() {
        val options = FintocWidgetOptions.Movements(
            holderType = FintocHolderType.INDIVIDUAL,
            country = FintocCountry.MEXICO,
        )

        assertEquals(
            "https://webview.fintoc.com/widget.html" +
                "?public_key=pk_test_abc123&product=movements&holder_type=individual&country=mx",
            FintocWidgetUrlBuilder.build(publicKey, options),
        )
    }

    @Test
    fun `movements url includes link token and webhook url in a fixed order`() {
        val options = FintocWidgetOptions.Movements(
            holderType = FintocHolderType.BUSINESS,
            linkToken = "link_123_token_456",
            webhookUrl = "https://example.com/fintoc/webhook?source=app&v=1",
        )

        assertEquals(
            "https://webview.fintoc.com/widget.html" +
                "?public_key=pk_test_abc123&product=movements&holder_type=business&country=cl" +
                "&link_token=link_123_token_456" +
                "&webhook_url=https%3A%2F%2Fexample.com%2Ffintoc%2Fwebhook%3Fsource%3Dapp%26v%3D1",
            FintocWidgetUrlBuilder.build(publicKey, options),
        )
    }

    @Test
    fun `subscriptions url sends the widget token`() {
        val options = FintocWidgetOptions.Subscriptions(
            widgetToken = "wt_123",
            holderType = FintocHolderType.INDIVIDUAL,
            country = FintocCountry.MEXICO,
        )

        assertEquals(
            "https://webview.fintoc.com/widget.html" +
                "?public_key=pk_test_abc123&product=subscriptions&holder_type=individual&country=mx" +
                "&widget_token=wt_123",
            FintocWidgetUrlBuilder.build(publicKey, options),
        )
    }

    @Test
    fun `token values cannot inject extra parameters`() {
        val url = FintocWidgetUrlBuilder.build(
            publicKey,
            FintocWidgetOptions.Payments(sessionToken = "cs_1&public_key=pk_live_attacker#frag"),
        )

        assertEquals(
            "https://webview.fintoc.com/widget.html" +
                "?public_key=pk_test_abc123&product=payments&session_token=cs_1%26public_key%3Dpk_live_attacker%23frag",
            url,
        )
    }

    @Test
    fun `widget url constant targets the fintoc webview host over https`() {
        assertEquals("https://webview.fintoc.com/widget.html", FintocWidgetUrlBuilder.WIDGET_URL)
    }
}
