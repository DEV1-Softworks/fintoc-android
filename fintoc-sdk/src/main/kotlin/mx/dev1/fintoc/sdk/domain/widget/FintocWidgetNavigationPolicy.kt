package mx.dev1.fintoc.sdk.domain.widget

import java.net.URI

/**
 * Decides where each address the Widget navigates to ends up, so a compromised or misbehaving page cannot turn the
 * WebView into a general-purpose browser inside the host app.
 *
 * - `fintocwidget://…` redirects are reported to the app and never opened.
 * - In the main frame, `https` addresses on a Fintoc host stay in the WebView. Any other `https` address, such as the
 *   payment voucher link that Payment Initiation shows, opens in the system browser. Everything else (`http`,
 *   `javascript:`, `intent:`, `file:`, `data:`, …) is blocked.
 * - Frames inside the page are left alone, apart from the redirects above: they cannot reach the app and the Widget
 *   may legitimately embed content.
 *
 * The check parses the address with [URI], which is strict: a host hidden behind a backslash, a percent escape or an
 * odd authority does not parse to a Fintoc host, so it fails closed.
 */
internal object FintocWidgetNavigationPolicy {
    private const val HTTPS_SCHEME = "https"
    private const val DEFAULT_HTTPS_PORT = 443
    private const val NO_PORT = -1

    /** Addresses longer than this are blocked. Real ones are a few hundred characters at most. */
    private const val MAX_URL_LENGTH = 8_192

    private val BLANK_PAGES = setOf("about:blank", "about:srcdoc")

    /** Hosts that serve the Widget: its page, its wizard and the Fintoc script. Exact matches only, no subdomains. */
    val TRUSTED_HOSTS: Set<String> = setOf("webview.fintoc.com", "wizard.fintoc.com", "js.fintoc.com")

    fun decide(url: String, isMainFrame: Boolean): FintocWidgetNavigation {
        if (FintocWidgetRedirectParser.isRedirect(url)) {
            return FintocWidgetNavigation.Redirect(FintocWidgetRedirectParser.parse(url))
        }
        if (!isMainFrame) return FintocWidgetNavigation.InsideWidget
        if (url.lowercase() in BLANK_PAGES) return FintocWidgetNavigation.InsideWidget

        val address = parseHttpsAddress(url) ?: return FintocWidgetNavigation.Blocked
        return if (address.isTrusted()) {
            FintocWidgetNavigation.InsideWidget
        } else {
            FintocWidgetNavigation.OutsideWidget(url)
        }
    }

    /** `true` when [url] is an `https` address on one of the [TRUSTED_HOSTS], the ones the Widget cannot work without. */
    fun isTrustedAddress(url: String): Boolean = parseHttpsAddress(url)?.isTrusted() == true

    /** `true` for an address that is safe to hand to the system browser: absolute `https` with a real host. */
    fun isOpenableOutsideWidget(url: String): Boolean = parseHttpsAddress(url) != null

    private class HttpsAddress(val host: String, val port: Int, val hasUserInfo: Boolean) {
        fun isTrusted(): Boolean =
            host in TRUSTED_HOSTS && (port == NO_PORT || port == DEFAULT_HTTPS_PORT) && !hasUserInfo
    }

    private fun parseHttpsAddress(url: String): HttpsAddress? {
        if (url.length > MAX_URL_LENGTH) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true)) return null
        val host = uri.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return HttpsAddress(host = host, port = uri.port, hasUserInfo = uri.userInfo != null)
    }
}
