package mx.dev1.fintoc.sdk.domain.checkout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocHostedCheckoutUrlPolicyTest {

    private fun isTrusted(url: String) = FintocHostedCheckoutUrlPolicy.isTrustedRedirectUrl(url)

    @Test
    fun `hosted pages that fintoc documents are trusted`() {
        listOf(
            "https://pay.fintoc.com/checkout/cs_li5531onlFDi235",
            "https://pay.fintoc.com/payment?checkout_session=cs_li5531onlFDi235",
            "https://checkout.fintoc.com/checkout_abc123",
            "https://cash.fintoc.com/pay/1",
        ).forEach { url -> assertTrue(url, isTrusted(url)) }
    }

    @Test
    fun `any subdomain of fintoc dot com is trusted, in any case, with the default port`() {
        assertTrue(isTrusted("https://a.b.fintoc.com/x"))
        assertTrue(isTrusted("HTTPS://PAY.Fintoc.COM/checkout/cs_1"))
        assertTrue(isTrusted("https://pay.fintoc.com:443/checkout/cs_1"))
    }

    @Test
    fun `addresses that only look like a fintoc host are refused`() {
        listOf(
            "https://fintoc.com/",
            "https://pay.fintoc.com.evil.example/checkout/cs_1",
            "https://evilfintoc.com/checkout/cs_1",
            "https://pay-fintoc.com/checkout/cs_1",
            "https://fintoc.com.evil.example/",
            "https://evil.example/pay.fintoc.com",
            "https://evil.example/?next=https://pay.fintoc.com/",
            "https://.fintoc.com/",
            "https://pay.fintoc.com./checkout/cs_1",
        ).forEach { url -> assertFalse(url, isTrusted(url)) }
    }

    @Test
    fun `addresses that hide the host or carry credentials are refused`() {
        listOf(
            "https://pay.fintoc.com@evil.example/checkout/cs_1",
            "https://user@pay.fintoc.com/checkout/cs_1",
            "https://user:pass@pay.fintoc.com/checkout/cs_1",
            "https://evil.example\\@pay.fintoc.com/",
            "https://pay%2Efintoc.com/checkout/cs_1",
            "https://pay.fintoc.com /checkout/cs_1",
            "https://pay.fintoc.com:8443/checkout/cs_1",
            "https://pay.fintoc.com:evil/checkout/cs_1",
        ).forEach { url -> assertFalse(url, isTrusted(url)) }
    }

    @Test
    fun `anything but https is refused`() {
        listOf(
            "http://pay.fintoc.com/checkout/cs_1",
            "javascript:alert(1)",
            "intent://pay.fintoc.com/#Intent;scheme=https;end",
            "file:///data/data/app/secret",
            "content://pay.fintoc.com/x",
            "data:text/html,<script>1</script>",
            "fintocwidget://exit",
            "//pay.fintoc.com/checkout/cs_1",
            "pay.fintoc.com/checkout/cs_1",
            "",
            "   ",
        ).forEach { url -> assertFalse(url, isTrusted(url)) }
    }

    @Test
    fun `oversized addresses are refused`() {
        assertFalse(isTrusted("https://pay.fintoc.com/checkout/?q=" + "x".repeat(2_100)))
    }
}
