package mx.dev1.fintoc.sdk.presentation.widget

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetNavigationPolicy

/**
 * Shows [url] in a hardened WebView and reports what the page does.
 *
 * A new WebView, and so a new page load, replaces the old one whenever [url] changes or the user taps "try again".
 * While the page is loading a progress indicator covers it, and when the page cannot be loaded the WebView is removed
 * and a failure message takes its place.
 *
 * Both [onEvent] and [externalLinkLauncher] may change between recompositions without reloading the page.
 */
@Composable
internal fun FintocWidgetWebView(
    url: String,
    onEvent: (FintocWidgetEvent) -> Unit,
    modifier: Modifier = Modifier,
    externalLinkLauncher: ExternalLinkLauncher = AndroidExternalLinkLauncher(LocalContext.current),
) {
    val latestOnEvent by rememberUpdatedState(onEvent)
    val latestExternalLinkLauncher by rememberUpdatedState(externalLinkLauncher)
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loadState by remember(url, attempt) { mutableStateOf(FintocWidgetLoadState.LOADING) }

    Box(modifier = modifier) {
        if (loadState != FintocWidgetLoadState.FAILED) {
            key(url, attempt) {
                AndroidView(
                    factory = { context ->
                        createFintocWidgetWebView(
                            context = context,
                            url = url,
                            client = FintocWidgetWebViewClient(
                                onEvent = { event -> latestOnEvent(event) },
                                externalLinkLauncher = { link -> latestExternalLinkLauncher.open(link) },
                                onLoadStateChanged = { newState -> loadState = newState },
                            ),
                            externalLinkLauncher = { link -> latestExternalLinkLauncher.open(link) },
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webView -> webView.release() },
                )
            }
        }
        when (loadState) {
            FintocWidgetLoadState.LOADING -> FintocWidgetLoading()
            FintocWidgetLoadState.FAILED -> FintocWidgetFailure(onRetry = { attempt += 1 })
            FintocWidgetLoadState.LOADED -> Unit
        }
    }
}

private fun createFintocWidgetWebView(
    context: Context,
    url: String,
    client: FintocWidgetWebViewClient,
    externalLinkLauncher: ExternalLinkLauncher,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    applyFintocWidgetSettings()
    webViewClient = client
    // Files the Widget offers, such as the payment voucher, are downloaded by the browser, never by the app.
    setDownloadListener { downloadUrl, _, _, _, _ ->
        if (FintocWidgetNavigationPolicy.isOpenableOutsideWidget(downloadUrl)) externalLinkLauncher.open(downloadUrl)
    }
    loadUrl(url)
}

/** Detaches the callbacks first, so nothing reaches a composition that is already gone, then frees the WebView. */
private fun WebView.release() {
    stopLoading()
    webViewClient = WebViewClient()
    setDownloadListener(null)
    (parent as? ViewGroup)?.removeView(this)
    destroy()
}
