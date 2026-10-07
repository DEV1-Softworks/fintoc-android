package mx.dev1.fintoc.sdk.presentation.widget

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetNavigationPolicy
import mx.dev1.fintoc.sdk.presentation.localization.rememberFintocStrings

private const val LOG_TAG = "FintocWidget"

/**
 * Shows [url] in a hardened WebView and reports what the page does.
 *
 * A new WebView, and so a new page load, replaces the old one whenever [url] changes or the user taps "try again".
 * While the page is loading a progress indicator covers it, and when the page cannot be loaded the WebView is removed
 * and a failure message takes its place.
 *
 * Some devices cannot create a WebView at all, for example when the WebView provider is disabled or being updated, and
 * Android then throws from the constructor. That never reaches the host app: the user sees a message that says what is
 * missing, with a button to try again.
 *
 * The texts of the loading and failure views follow [language], or the language of the device when it is `null`. Both
 * [onEvent] and [externalLinkLauncher] may change between recompositions without reloading the page.
 *
 * @param webViewFactory Creates the WebView. Tests replace it to simulate a device that cannot.
 */
@Composable
internal fun FintocWidgetWebView(
    url: String,
    onEvent: (FintocWidgetEvent) -> Unit,
    externalLinkLauncher: ExternalLinkLauncher,
    modifier: Modifier = Modifier,
    language: FintocLanguage? = null,
    webViewFactory: FintocWebViewFactory = PlatformWebViewFactory,
) {
    val context = LocalContext.current
    val strings = rememberFintocStrings(language)
    val latestOnEvent by rememberUpdatedState(onEvent)
    val latestExternalLinkLauncher by rememberUpdatedState(externalLinkLauncher)
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loadState by remember(url, attempt) { mutableStateOf(FintocWidgetLoadState.LOADING) }
    val widgetWebView = remember(url, attempt, webViewFactory) {
        FintocWidgetWebViewHolder.create(
            context = context,
            webViewFactory = webViewFactory,
            url = url,
            client = FintocWidgetWebViewClient(
                onEvent = { event -> latestOnEvent(event) },
                externalLinkLauncher = { link -> latestExternalLinkLauncher.open(link) },
                onLoadStateChanged = { newState -> loadState = newState },
            ),
            externalLinkLauncher = { link -> latestExternalLinkLauncher.open(link) },
        )
    }
    val shownState = if (widgetWebView.isAvailable) loadState else FintocWidgetLoadState.UNAVAILABLE

    Box(modifier = modifier) {
        if (shownState == FintocWidgetLoadState.LOADING || shownState == FintocWidgetLoadState.LOADED) {
            // A different holder is a different WebView, and the Compose view must not be reused for it.
            key(widgetWebView) {
                AndroidView(
                    factory = { widgetWebView.requireWebView() },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { widgetWebView.release() },
                )
            }
        }
        when (shownState) {
            FintocWidgetLoadState.LOADING -> FintocWidgetLoading(strings)
            FintocWidgetLoadState.FAILED -> FintocWidgetFailure(strings, onRetry = { attempt += 1 })
            FintocWidgetLoadState.UNAVAILABLE -> FintocWidgetFailure(
                strings = strings,
                onRetry = { attempt += 1 },
                messageId = R.string.fintoc_widget_error_webview_unavailable,
            )
            FintocWidgetLoadState.LOADED -> Unit
        }
    }
}

/**
 * The WebView of one page load, created when the composition first needs it and freed when the composition forgets it.
 *
 * The WebView is created while composing, so a composition that is thrown away before it is applied would leak it:
 * being a [RememberObserver] is what frees it in that case too. [webView] is `null` when the device could not create
 * one.
 */
private class FintocWidgetWebViewHolder private constructor(private val webView: WebView?) : RememberObserver {

    private var isReleased = false

    val isAvailable: Boolean get() = webView != null

    fun requireWebView(): WebView = checkNotNull(webView) { "There is no WebView on this device." }

    /** Safe to call more than once: the Compose view and the composition both free the same WebView. */
    fun release() {
        if (isReleased) return
        isReleased = true
        webView?.release()
    }

    override fun onRemembered() = Unit

    override fun onForgotten() = release()

    override fun onAbandoned() = release()

    companion object {

        /**
         * Creates and starts a WebView, or returns a holder without one when the device cannot.
         *
         * Android fails in several ways, all of them unchecked: `MissingWebViewPackageException` when no WebView
         * provider is installed, enabled or ready, and plain runtime exceptions when the provider is broken. Errors
         * are not caught, because they are not something to recover from. The log names the kind of failure and never
         * its message.
         */
        fun create(
            context: Context,
            webViewFactory: FintocWebViewFactory,
            url: String,
            client: FintocWidgetWebViewClient,
            externalLinkLauncher: ExternalLinkLauncher,
        ): FintocWidgetWebViewHolder {
            var createdWebView: WebView? = null
            return try {
                val webView = webViewFactory.create(context)
                createdWebView = webView
                webView.apply {
                    layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    applyFintocWidgetSettings()
                    webViewClient = client
                    // Files the Widget offers, such as the payment voucher, are downloaded by the browser, never by
                    // the app.
                    setDownloadListener { downloadUrl, _, _, _, _ ->
                        if (FintocWidgetNavigationPolicy.isOpenableOutsideWidget(downloadUrl)) {
                            externalLinkLauncher.open(downloadUrl)
                        }
                    }
                    loadUrl(url)
                }
                FintocWidgetWebViewHolder(webView)
            } catch (failure: RuntimeException) {
                Log.w(LOG_TAG, "The WebView could not be created: ${failure.javaClass.name}.")
                createdWebView?.release()
                FintocWidgetWebViewHolder(webView = null)
            }
        }
    }
}

/** Detaches the callbacks first, so nothing reaches a composition that is already gone, then frees the WebView. */
private fun WebView.release() {
    stopLoading()
    webViewClient = WebViewClient()
    setDownloadListener(null)
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}
