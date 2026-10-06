package mx.dev1.fintoc.sdk.domain.checkout

import java.net.URI
import mx.dev1.fintoc.sdk.domain.widget.PercentDecoding

/**
 * One of the two addresses Fintoc sends the customer back to, as the host app configured it, and the rule that says
 * whether an address that reached the app is that one.
 *
 * An address matches when its scheme, host, port and path are the same, ignoring case where the standard allows it and
 * a trailing slash. Its query and fragment are ignored, except for the query parameters that the configured address
 * itself carries: each of them must come back with the same value. That makes an unguessable value, which the app puts
 * in the address it gives Fintoc, work like OAuth's `state`: another app that fires an intent at the deep link cannot
 * know it.
 */
internal class FintocHostedCheckoutReturnUrl private constructor(private val address: Address) {

    /** What identifies an address. Only the first occurrence of a query parameter counts. */
    private class Address(
        val scheme: String,
        val host: String?,
        val port: Int,
        val path: String,
        val query: Map<String, String>,
    )

    fun accepts(returnedUrl: String): Boolean {
        val returned = parseAddress(returnedUrl, maxLength = MAX_RETURNED_URL_LENGTH) ?: return false
        return accepts(returned)
    }

    /** `true` when some address would be accepted by both, so the two outcomes could not be told apart. */
    fun isIndistinguishableFrom(other: FintocHostedCheckoutReturnUrl): Boolean =
        accepts(other.address) || other.accepts(address)

    private fun accepts(returned: Address): Boolean =
        address.scheme == returned.scheme &&
            address.host == returned.host &&
            address.port == returned.port &&
            address.path == returned.path &&
            address.query.all { (name, value) -> returned.query[name] == value }

    /** The address may hold a secret value, so it is never shown. */
    override fun toString(): String = "FintocHostedCheckoutReturnUrl(<redacted>)"

    companion object {
        private const val HTTPS_SCHEME = "https"
        private const val NO_PORT = -1
        private const val DEFAULT_HTTPS_PORT = 443
        private const val MAX_CONFIGURED_URL_LENGTH = 2_048
        private const val MAX_RETURNED_URL_LENGTH = 8_192

        /** Schemes that are never a place to send a customer back to. `http` is cleartext, and App Links need https. */
        private val REFUSED_SCHEMES = setOf(
            "http", "ftp", "ws", "wss", "javascript", "vbscript", "data", "file", "content", "intent", "blob", "about",
            "fintocwidget",
        )

        /**
         * @throws IllegalArgumentException If [url] is not an absolute `https` address, or a custom scheme of the app.
         * The message names [parameterName] but never repeats [url], which may hold a secret value.
         */
        fun parse(url: String, parameterName: String): FintocHostedCheckoutReturnUrl {
            require(url.isNotBlank()) { "$parameterName must not be blank." }
            require(url.none { character -> character.isWhitespace() || character.isISOControl() }) {
                "$parameterName must not contain whitespace."
            }
            val address = parseAddress(url, maxLength = MAX_CONFIGURED_URL_LENGTH)
            requireNotNull(address) { "$parameterName must be an absolute address with a scheme, such as https://…" }
            require(address.scheme !in REFUSED_SCHEMES) {
                "$parameterName must use https, or a custom scheme of your app. Plain http and browser or file " +
                    "schemes are refused."
            }
            require(address.host != null || address.path.isNotEmpty()) {
                "$parameterName must say where in the app the customer lands: a host or a path."
            }
            return FintocHostedCheckoutReturnUrl(address)
        }

        private fun parseAddress(url: String, maxLength: Int): Address? {
            if (url.length > maxLength) return null
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            if (uri.isOpaque || uri.userInfo != null) return null

            val host = if (scheme == HTTPS_SCHEME) {
                uri.host?.lowercase() ?: return null
            } else {
                // A custom scheme may use a name that is not a valid host, such as one with an underscore.
                (uri.host ?: uri.rawAuthority)?.lowercase()
            }
            val port = if (uri.port == DEFAULT_HTTPS_PORT && scheme == HTTPS_SCHEME) NO_PORT else uri.port
            return Address(
                scheme = scheme,
                host = host,
                port = port,
                path = uri.rawPath.orEmpty().trimEnd('/'),
                query = parseQuery(uri.rawQuery),
            )
        }

        private fun parseQuery(rawQuery: String?): Map<String, String> {
            if (rawQuery.isNullOrEmpty()) return emptyMap()

            val parameters = LinkedHashMap<String, String>()
            for (pair in rawQuery.split('&')) {
                val name = PercentDecoding.decode(pair.substringBefore('='))
                if (name.isEmpty() || name in parameters) continue
                parameters[name] = PercentDecoding.decode(pair.substringAfter('=', missingDelimiterValue = ""))
            }
            return parameters
        }
    }
}
