package mx.dev1.fintoc.sdk.domain.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocHostedCheckoutReturnUrlTest {

    private fun returnUrl(url: String) = FintocHostedCheckoutReturnUrl.parse(url, parameterName = "successUrl")

    @Test
    fun `the configured address itself is accepted`() {
        assertTrue(returnUrl("https://merchant.com/pay/success").accepts("https://merchant.com/pay/success"))
        assertTrue(returnUrl("myapp://payment/success").accepts("myapp://payment/success"))
    }

    @Test
    fun `case in the scheme and host and a trailing slash do not matter`() {
        val configured = returnUrl("https://Merchant.com/pay/success")

        assertTrue(configured.accepts("HTTPS://merchant.COM/pay/success"))
        assertTrue(configured.accepts("https://merchant.com/pay/success/"))
        assertTrue(returnUrl("https://merchant.com/pay/success/").accepts("https://merchant.com/pay/success"))
        assertTrue(returnUrl("https://merchant.com").accepts("https://merchant.com/"))
    }

    @Test
    fun `the default https port is the same as no port`() {
        assertTrue(returnUrl("https://merchant.com/ok").accepts("https://merchant.com:443/ok"))
        assertTrue(returnUrl("https://merchant.com:443/ok").accepts("https://merchant.com/ok"))
        assertFalse(returnUrl("https://merchant.com/ok").accepts("https://merchant.com:8443/ok"))
    }

    @Test
    fun `the path is case sensitive and must be the same`() {
        val configured = returnUrl("https://merchant.com/pay/success")

        assertFalse(configured.accepts("https://merchant.com/pay/Success"))
        assertFalse(configured.accepts("https://merchant.com/pay/success/extra"))
        assertFalse(configured.accepts("https://merchant.com/pay"))
        assertFalse(configured.accepts("https://merchant.com/other/success"))
    }

    @Test
    fun `another host or scheme is not accepted`() {
        val configured = returnUrl("https://merchant.com/pay/success")

        assertFalse(configured.accepts("https://evil.example/pay/success"))
        assertFalse(configured.accepts("https://merchant.com.evil.example/pay/success"))
        assertFalse(configured.accepts("https://sub.merchant.com/pay/success"))
        assertFalse(configured.accepts("http://merchant.com/pay/success"))
        assertFalse(configured.accepts("myapp://merchant.com/pay/success"))
    }

    @Test
    fun `an address that carries credentials or hides its host is not accepted`() {
        val configured = returnUrl("https://merchant.com/pay/success")

        assertFalse(configured.accepts("https://merchant.com@evil.example/pay/success"))
        assertFalse(configured.accepts("https://user@merchant.com/pay/success"))
        assertFalse(configured.accepts("https://evil.example\\@merchant.com/pay/success"))
    }

    @Test
    fun `the query and fragment are ignored when the configured address has none`() {
        val configured = returnUrl("https://merchant.com/pay/success")

        assertTrue(configured.accepts("https://merchant.com/pay/success?session=cs_1&anything=else"))
        assertTrue(configured.accepts("https://merchant.com/pay/success#fragment"))
    }

    @Test
    fun `a query parameter of the configured address must come back with the same value`() {
        val configured = returnUrl("https://merchant.com/pay/success?n=secret123&order=42")

        assertTrue(configured.accepts("https://merchant.com/pay/success?n=secret123&order=42"))
        assertTrue(configured.accepts("https://merchant.com/pay/success?order=42&extra=1&n=secret123"))
        assertFalse(configured.accepts("https://merchant.com/pay/success?n=guess&order=42"))
        assertFalse(configured.accepts("https://merchant.com/pay/success?order=42"))
        assertFalse(configured.accepts("https://merchant.com/pay/success"))
        assertFalse(configured.accepts("https://merchant.com/pay/success?n=&order=42"))
    }

    @Test
    fun `only the first occurrence of a parameter counts, so a later copy cannot override it`() {
        val configured = returnUrl("https://merchant.com/pay/success?n=secret123")

        assertTrue(configured.accepts("https://merchant.com/pay/success?n=secret123&n=other"))
        assertFalse(configured.accepts("https://merchant.com/pay/success?n=other&n=secret123"))
    }

    @Test
    fun `percent escapes in parameters are compared decoded`() {
        val configured = returnUrl("https://merchant.com/pay/success?n=a%20b%2Fc")

        assertTrue(configured.accepts("https://merchant.com/pay/success?n=a%20b/c"))
        assertTrue(configured.accepts("https://merchant.com/pay/success?n=a%20b%2fc"))
        assertFalse(configured.accepts("https://merchant.com/pay/success?n=a+b%2Fc"))
    }

    @Test
    fun `custom schemes may use names that are not valid hosts`() {
        val configured = returnUrl("myapp://payment_done/success?n=1")

        assertTrue(configured.accepts("myapp://payment_done/success?n=1"))
        assertFalse(configured.accepts("myapp://payment_other/success?n=1"))
    }

    @Test
    fun `input that is not an address is not accepted and never throws`() {
        val configured = returnUrl("https://merchant.com/pay/success")

        listOf("", "  ", "not a url", "https://", "https://merchant.com/pay/success\u0000", "mailto:a@b.c").forEach { url ->
            assertFalse(url, configured.accepts(url))
        }
        assertFalse(configured.accepts("https://merchant.com/pay/success?" + "x".repeat(9_000)))
    }

    @Test
    fun `addresses that cannot send a customer back are refused without being repeated`() {
        listOf(
            "",
            "   ",
            "merchant.com/success",
            "/success",
            "http://merchant.com/success",
            "javascript:alert(1)",
            "intent://x#Intent;end",
            "file:///data/secret",
            "content://x/y",
            "data:text/html,hi",
            "about:blank",
            "fintocwidget://exit",
            "https://merchant.com/ success",
            "myapp:success",
            "myapp://",
            "https://merchant.com@evil.example/success",
            "https://" + "x".repeat(2_100) + ".com/",
        ).forEach { url ->
            val failure = assertThrows(url, IllegalArgumentException::class.java) { returnUrl(url) }
            assertTrue(failure.message.orEmpty().contains("successUrl"))
            if (url.length in 6..60) assertFalse(url, failure.message.orEmpty().contains(url))
        }
    }

    @Test
    fun `the error message never repeats a secret value in the address`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            returnUrl("http://merchant.com/success?n=super-secret-value")
        }

        assertFalse(failure.message.orEmpty().contains("super-secret-value"))
    }

    @Test
    fun `https and custom schemes are accepted`() {
        returnUrl("https://merchant.com/success")
        returnUrl("myapp://payment/success")
        returnUrl("myapp:/payment/success")
        returnUrl("com.merchant.app://success")
    }

    @Test
    fun `two addresses are told apart by host, path or a query parameter`() {
        val success = returnUrl("https://merchant.com/pay/success")

        assertFalse(success.isIndistinguishableFrom(returnUrl("https://merchant.com/pay/cancel")))
        assertFalse(success.isIndistinguishableFrom(returnUrl("https://other.com/pay/success")))
        assertFalse(
            returnUrl("https://merchant.com/pay?status=ok")
                .isIndistinguishableFrom(returnUrl("https://merchant.com/pay?status=cancel")),
        )
    }

    @Test
    fun `an address that one of the two would also accept makes them indistinguishable`() {
        val plain = returnUrl("https://merchant.com/pay/done")

        assertTrue(plain.isIndistinguishableFrom(returnUrl("https://merchant.com/pay/done")))
        assertTrue(plain.isIndistinguishableFrom(returnUrl("https://merchant.com/pay/done/")))
        assertTrue(plain.isIndistinguishableFrom(returnUrl("https://merchant.com/pay/done?status=cancel")))
    }

    @Test
    fun `toString hides the address`() {
        assertEquals(
            "FintocHostedCheckoutReturnUrl(<redacted>)",
            returnUrl("https://merchant.com/pay/success?n=secret123").toString(),
        )
    }
}
