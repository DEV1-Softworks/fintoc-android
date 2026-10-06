package mx.dev1.fintoc.sdk.domain.checkout

import java.net.URI

/**
 * Decides which addresses may be opened as a hosted checkout.
 *
 * The address normally comes from your backend, but it travels through your app, and a hosted checkout asks the
 * customer for payment details. So only `https` addresses on a `fintoc.com` subdomain are opened, such as
 * `pay.fintoc.com`. Fintoc's own documentation shows hosted pages on more than one subdomain, so the whole domain is
 * trusted rather than a fixed list. The Custom Tab shows the address bar, so the customer can see where they are.
 *
 * The check parses with [URI], which is strict: a host hidden behind a backslash, a percent escape or an `@` does not
 * parse to a Fintoc host, so it fails closed.
 */
internal object FintocHostedCheckoutUrlPolicy {
    private const val HTTPS_SCHEME = "https"
    private const val DEFAULT_HTTPS_PORT = 443
    private const val NO_PORT = -1

    /** Addresses longer than this are refused. Real ones are a few hundred characters at most. */
    private const val MAX_URL_LENGTH = 2_048

    /** With the leading dot: the bare `fintoc.com`, and any other domain that merely ends in the same letters, fail. */
    private const val TRUSTED_SUFFIX = ".fintoc.com"

    fun isTrustedRedirectUrl(url: String): Boolean {
        if (url.length > MAX_URL_LENGTH) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != NO_PORT && uri.port != DEFAULT_HTTPS_PORT) return false

        val host = uri.host?.lowercase() ?: return false
        return host.endsWith(TRUSTED_SUFFIX) && host.length > TRUSTED_SUFFIX.length
    }
}
