package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocWidgetNavigationPolicyTest {

    private fun decideInMainFrame(url: String) = FintocWidgetNavigationPolicy.decide(url, isMainFrame = true)

    @Test
    fun `redirects are reported with their event whichever frame sends them`() {
        listOf(true, false).forEach { isMainFrame ->
            assertEquals(
                FintocWidgetNavigation.Redirect(FintocWidgetEvent.Exited),
                FintocWidgetNavigationPolicy.decide("fintocwidget://exit", isMainFrame),
            )
        }
    }

    @Test
    fun `redirect the parser ignores is still swallowed without an event`() {
        assertEquals(
            FintocWidgetNavigation.Redirect(event = null),
            decideInMainFrame("fintocwidget://something-new"),
        )
    }

    @Test
    fun `trusted fintoc hosts stay inside the widget`() {
        FintocWidgetNavigationPolicy.TRUSTED_HOSTS.forEach { host ->
            assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("https://$host/widget.html?a=b"))
        }
    }

    @Test
    fun `trusted hosts are matched ignoring case and an explicit default port`() {
        assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("HTTPS://Webview.Fintoc.COM/widget.html"))
        assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("https://wizard.fintoc.com:443/"))
    }

    @Test
    fun `any other https address opens in the system browser`() {
        val voucherUrl = "https://files.example.com/voucher.pdf?token=secret"

        assertEquals(FintocWidgetNavigation.OutsideWidget(voucherUrl), decideInMainFrame(voucherUrl))
    }

    @Test
    fun `addresses that only look like a fintoc host are not trusted`() {
        listOf(
            "https://webview.fintoc.com.evil.example/widget.html",
            "https://evilwebview.fintoc.com/",
            "https://fintoc.com/",
            "https://sub.webview.fintoc.com/",
            "https://webview.fintoc.com@evil.example/widget.html",
            "https://webview.fintoc.com:evil@evil.example/",
            "https://evil.example/https://webview.fintoc.com/",
            "https://webview.fintoc.com./",
            "https://webview.fintoc.com:8443/widget.html",
            "https://user@webview.fintoc.com/widget.html",
        ).forEach { url ->
            assertTrue(url, decideInMainFrame(url) is FintocWidgetNavigation.OutsideWidget)
        }
    }

    @Test
    fun `addresses that hide the host are blocked instead of trusted`() {
        listOf(
            "https://evil.example\\@webview.fintoc.com/",
            "https://webview%2Efintoc.com/",
            "https://webview.fintoc.com /widget.html",
            "https:///webview.fintoc.com/",
            "https://",
            "https:webview.fintoc.com",
        ).forEach { url -> assertEquals(url, FintocWidgetNavigation.Blocked, decideInMainFrame(url)) }
    }

    @Test
    fun `non https schemes are blocked in the main frame`() {
        listOf(
            "http://webview.fintoc.com/widget.html",
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///data/data/app/secret",
            "content://contacts/people",
            "data:text/html,<script>1</script>",
            "market://details?id=app",
            "mailto:support@example.com",
            "tel:123",
            "ftp://webview.fintoc.com/",
            "",
            "   ",
            "not a url",
        ).forEach { url -> assertEquals(url, FintocWidgetNavigation.Blocked, decideInMainFrame(url)) }
    }

    @Test
    fun `blank pages are allowed`() {
        assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("about:blank"))
        assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("ABOUT:BLANK"))
        assertEquals(FintocWidgetNavigation.InsideWidget, decideInMainFrame("about:srcdoc"))
        assertEquals(FintocWidgetNavigation.Blocked, decideInMainFrame("about:config"))
    }

    @Test
    fun `oversized addresses are blocked`() {
        assertEquals(
            FintocWidgetNavigation.Blocked,
            decideInMainFrame("https://webview.fintoc.com/?q=" + "x".repeat(9_000)),
        )
    }

    @Test
    fun `frames inside the page are not interfered with`() {
        listOf(
            "https://webview.fintoc.com/frame",
            "https://other.example/frame",
            "http://other.example/frame",
            "about:blank",
            "data:text/html,hello",
        ).forEach { url ->
            assertEquals(
                url,
                FintocWidgetNavigation.InsideWidget,
                FintocWidgetNavigationPolicy.decide(url, isMainFrame = false),
            )
        }
    }

    @Test
    fun `only absolute https addresses can be opened outside the widget`() {
        assertTrue(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("https://files.example.com/voucher.pdf"))
        assertTrue(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("https://webview.fintoc.com/voucher.pdf"))
        assertFalse(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("http://files.example.com/voucher.pdf"))
        assertFalse(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("intent://x#Intent;end"))
        assertFalse(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("fintocwidget://exit"))
        assertFalse(FintocWidgetNavigationPolicy.isOpenableOutsideWidget("https://"))
        assertFalse(FintocWidgetNavigationPolicy.isOpenableOutsideWidget(""))
    }

    @Test
    fun `only https addresses on a fintoc host are trusted`() {
        FintocWidgetNavigationPolicy.TRUSTED_HOSTS.forEach { host ->
            assertTrue(FintocWidgetNavigationPolicy.isTrustedAddress("https://$host/a/b?c=d"))
        }
        listOf(
            "http://webview.fintoc.com/",
            "https://webview.fintoc.com.evil.example/",
            "https://webview.fintoc.com@evil.example/",
            "https://analytics.example.com/script.js",
            "fintocwidget://exit",
            "",
        ).forEach { url -> assertFalse(url, FintocWidgetNavigationPolicy.isTrustedAddress(url)) }
    }

    @Test
    fun `outside widget hides the address in toString`() {
        val navigation = FintocWidgetNavigation.OutsideWidget("https://files.example.com/voucher.pdf?token=secret")

        assertFalse(navigation.toString().contains("secret"))
        assertFalse(navigation.toString().contains("files.example.com"))
    }
}
