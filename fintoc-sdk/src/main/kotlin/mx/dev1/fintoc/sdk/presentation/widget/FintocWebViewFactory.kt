package mx.dev1.fintoc.sdk.presentation.widget

import android.content.Context
import android.webkit.WebView

/**
 * Creates the WebView that shows the Widget.
 *
 * Creating one can throw: a device may have no WebView provider installed or enabled, and Android then fails with an
 * exception of its own (`MissingWebViewPackageException`). Going through this seam lets the Widget turn that into a
 * message, and lets tests make it happen on purpose.
 */
internal fun interface FintocWebViewFactory {
    fun create(context: Context): WebView
}

/** The WebView of the platform. */
internal object PlatformWebViewFactory : FintocWebViewFactory {
    override fun create(context: Context): WebView = WebView(context)
}
