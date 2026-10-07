package mx.dev1.fintoc.sdk.presentation.widget

import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocWidgetWebViewClientTest {

    private val events = mutableListOf<FintocWidgetEvent>()
    private val openedLinks = mutableListOf<String>()
    private val loadStates = mutableListOf<FintocWidgetLoadState>()
    private val webView: WebView = mock()

    private val client = FintocWidgetWebViewClient(
        onEvent = { event -> events += event },
        externalLinkLauncher = { link -> openedLinks += link },
        onLoadStateChanged = { state -> loadStates += state },
    )

    private fun request(url: String, isMainFrame: Boolean = true): WebResourceRequest = mock {
        on { this.url } doReturn Uri.parse(url)
        on { isForMainFrame } doReturn isMainFrame
    }

    @Test
    fun `a succeeded redirect is reported and never opened`() {
        val handled = client.shouldOverrideUrlLoading(webView, request("fintocwidget://succeeded"))

        assertTrue(handled)
        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Succeeded()), events)
        assertTrue(openedLinks.isEmpty())
    }

    @Test
    fun `an exit redirect is reported`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, request("fintocwidget://exit")))

        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Exited), events)
    }

    @Test
    fun `an event redirect from a frame inside the page is reported too`() {
        val handled = client.shouldOverrideUrlLoading(
            webView,
            request("fintocwidget://event/opened?timestamp=7", isMainFrame = false),
        )

        assertTrue(handled)
        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Occurred("opened", 7L)), events)
    }

    @Test
    fun `a redirect the parser ignores is swallowed without an event`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, request("fintocwidget://something-new")))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `fintoc hosts load inside the webview`() {
        assertFalse(client.shouldOverrideUrlLoading(webView, request("https://wizard.fintoc.com/flow")))

        assertTrue(events.isEmpty())
        assertTrue(openedLinks.isEmpty())
    }

    @Test
    fun `other https addresses open outside and stay out of the webview`() {
        val voucherUrl = "https://files.example.com/voucher.pdf"

        assertTrue(client.shouldOverrideUrlLoading(webView, request(voucherUrl)))

        assertEquals(listOf(voucherUrl), openedLinks)
    }

    @Test
    fun `unsafe addresses in the main frame are blocked`() {
        listOf("http://webview.fintoc.com/", "intent://x#Intent;end", "javascript:alert(1)", "file:///etc/hosts")
            .forEach { url -> assertTrue(url, client.shouldOverrideUrlLoading(webView, request(url))) }

        assertTrue(events.isEmpty())
        assertTrue(openedLinks.isEmpty())
    }

    @Test
    fun `frames inside the page load whatever they ask for`() {
        assertFalse(
            client.shouldOverrideUrlLoading(webView, request("https://other.example/frame", isMainFrame = false)),
        )
        assertTrue(openedLinks.isEmpty())
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the string overload used on android 6 applies the same policy`() {
        assertTrue(client.shouldOverrideUrlLoading(webView, "fintocwidget://exit"))
        assertTrue(client.shouldOverrideUrlLoading(webView, "https://files.example.com/voucher.pdf"))
        assertTrue(client.shouldOverrideUrlLoading(webView, "http://insecure.example/"))
        assertFalse(client.shouldOverrideUrlLoading(webView, "https://webview.fintoc.com/widget.html"))

        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Exited), events)
        assertEquals(listOf("https://files.example.com/voucher.pdf"), openedLinks)
    }

    @Test
    fun `a finished page reports loaded`() {
        client.onPageFinished(webView, "https://webview.fintoc.com/widget.html")

        assertEquals(listOf(FintocWidgetLoadState.LOADED), loadStates)
    }

    @Test
    fun `a main frame error fails the load and replaces the error page`() {
        client.onReceivedError(webView, request("https://webview.fintoc.com/widget.html"), mock<WebResourceError>())

        assertEquals(listOf(FintocWidgetLoadState.FAILED), loadStates)
        verify(webView).stopLoading()
        verify(webView).loadUrl("about:blank")
    }

    @Test
    fun `an error in a resource of the page is ignored`() {
        client.onReceivedError(webView, request("https://cdn.example/logo.png", isMainFrame = false), mock())

        assertTrue(loadStates.isEmpty())
        verifyNoInteractions(webView)
    }

    @Test
    fun `a main frame http error fails the load`() {
        val serverError: WebResourceResponse = mock { on { statusCode } doReturn 503 }

        client.onReceivedHttpError(webView, request("https://webview.fintoc.com/widget.html"), serverError)

        assertEquals(listOf(FintocWidgetLoadState.FAILED), loadStates)
    }

    @Test
    fun `http errors below 400 and in resources are ignored`() {
        val redirect: WebResourceResponse = mock { on { statusCode } doReturn 399 }
        val notFound: WebResourceResponse = mock { on { statusCode } doReturn 404 }

        client.onReceivedHttpError(webView, request("https://webview.fintoc.com/"), redirect)
        client.onReceivedHttpError(webView, request("https://cdn.example/logo.png", isMainFrame = false), notFound)

        assertTrue(loadStates.isEmpty())
    }

    @Test
    fun `once failed the page never reports loaded and is blanked only once`() {
        val mainFrame = request("https://webview.fintoc.com/widget.html")

        client.onReceivedError(webView, mainFrame, mock())
        client.onReceivedError(webView, mainFrame, mock())
        client.onPageFinished(webView, "about:blank")

        assertEquals(listOf(FintocWidgetLoadState.FAILED), loadStates)
        verify(webView).loadUrl("about:blank")
    }

    private fun certificateProblemAt(url: String): SslError = mock { on { this.url } doReturn url }

    @Test
    fun `a certificate problem on a fintoc host fails the load and is never accepted`() {
        val handler: SslErrorHandler = mock()

        client.onReceivedSslError(webView, handler, certificateProblemAt("https://webview.fintoc.com/widget.html"))

        verify(handler).cancel()
        verify(handler, never()).proceed()
        assertEquals(listOf(FintocWidgetLoadState.FAILED), loadStates)
        verify(webView).loadUrl("about:blank")
    }

    @Test
    fun `a certificate problem in a third party resource is refused without failing the load`() {
        val handler: SslErrorHandler = mock()

        client.onReceivedSslError(webView, handler, certificateProblemAt("https://analytics.example.com/script.js"))

        verify(handler).cancel()
        verify(handler, never()).proceed()
        assertTrue(loadStates.isEmpty())
        verifyNoInteractions(webView)
    }

    @Test
    fun `a crashed renderer fails the load without touching the dead webview`() {
        val handled = client.onRenderProcessGone(webView, mock<RenderProcessGoneDetail>())

        assertTrue(handled)
        assertEquals(listOf(FintocWidgetLoadState.FAILED), loadStates)
        verifyNoInteractions(webView)
    }
}
