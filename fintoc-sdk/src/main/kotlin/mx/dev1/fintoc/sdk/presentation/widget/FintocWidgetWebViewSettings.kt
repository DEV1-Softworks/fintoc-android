package mx.dev1.fintoc.sdk.presentation.widget

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Settings for the WebView that shows the Widget.
 *
 * The first group is what the Widget needs and follows Fintoc's WebView guide. The rest closes doors the Widget does
 * not need: local files, content providers, insecure subresources and saved form data. The SDK never exposes a
 * JavaScript interface to the page and never turns on WebView debugging, which stays the host app's decision.
 */
@SuppressLint("SetJavaScriptEnabled") // The Widget is a JavaScript application.
internal fun WebView.applyFintocWidgetSettings() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        useWideViewPort = true
        cacheMode = WebSettings.LOAD_NO_CACHE

        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        setSupportMultipleWindows(false)
        setGeolocationEnabled(false)
        @Suppress("DEPRECATION") // Only does anything below API 26, where it would otherwise save card details.
        saveFormData = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = true
        }
    }
}
