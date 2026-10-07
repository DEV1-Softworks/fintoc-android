package mx.dev1.fintoc.sdk.presentation.widget

import android.net.http.SslError
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetNavigation
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetNavigationPolicy

/**
 * Connects the WebView to the SDK: it reports the Widget's events, keeps the page from wandering off, and tells the
 * Compose layer when the page cannot be shown.
 *
 * Every callback arrives on the main thread. A failed page is replaced by the SDK's own failure view, because the
 * error page of the WebView prints the failing address, and that address carries the session token.
 */
internal class FintocWidgetWebViewClient(
    private val onEvent: (FintocWidgetEvent) -> Unit,
    private val externalLinkLauncher: ExternalLinkLauncher,
    private val onLoadStateChanged: (FintocWidgetLoadState) -> Unit,
) : WebViewClient() {

    private var hasFailed = false

    /** Called instead of the overload below on Android 6.0 (API 23), which has no frame information. */
    @Deprecated("Replaced by the overload that receives a WebResourceRequest from API 24.")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        handleNavigation(url = url, isMainFrame = true)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        handleNavigation(url = request.url.toString(), isMainFrame = request.isForMainFrame)

    private fun handleNavigation(url: String, isMainFrame: Boolean): Boolean =
        when (val navigation = FintocWidgetNavigationPolicy.decide(url, isMainFrame)) {
            is FintocWidgetNavigation.Redirect -> {
                navigation.event?.let(onEvent)
                true
            }
            FintocWidgetNavigation.InsideWidget -> false
            is FintocWidgetNavigation.OutsideWidget -> {
                externalLinkLauncher.open(navigation.url)
                true
            }
            FintocWidgetNavigation.Blocked -> true
        }

    override fun onPageFinished(view: WebView, url: String?) {
        if (!hasFailed) onLoadStateChanged(FintocWidgetLoadState.LOADED)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) failAndBlankOut(view)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame && errorResponse.statusCode >= HTTP_ERROR_THRESHOLD) failAndBlankOut(view)
    }

    /**
     * Never proceeds past a certificate problem. The WebView does not report a cancelled connection through
     * [onReceivedError]: it just finishes the page. So a problem with a Fintoc host, without which the Widget cannot
     * work, fails the load here. Third-party resources the page embeds are refused and otherwise ignored.
     */
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        if (FintocWidgetNavigationPolicy.isTrustedAddress(error.url)) failAndBlankOut(view)
    }

    /**
     * Without this, the death of the WebView's renderer process takes the host app down with it. The WebView cannot be
     * used again, so it is not touched here: the Compose layer discards it.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        markFailed()
        return true
    }

    private fun failAndBlankOut(view: WebView) {
        if (hasFailed) return
        markFailed()
        view.stopLoading()
        view.loadUrl(BLANK_PAGE)
    }

    private fun markFailed() {
        hasFailed = true
        onLoadStateChanged(FintocWidgetLoadState.FAILED)
    }

    private companion object {
        const val HTTP_ERROR_THRESHOLD = 400
        const val BLANK_PAGE = "about:blank"
    }
}
