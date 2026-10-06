package mx.dev1.fintoc.sdk.domain.widget

/**
 * Builds the address the Android WebView loads to show the Fintoc Widget.
 *
 * The Widget reads its whole configuration from the query string, so every value is percent-encoded and parameters
 * always come out in the same order.
 */
internal object FintocWidgetUrlBuilder {
    const val WIDGET_URL = "https://webview.fintoc.com/widget.html"

    fun build(publicKey: String, options: FintocWidgetOptions): String {
        val parameters = buildList {
            add("public_key" to publicKey)
            add("product" to options.product.code)
            addAll(productParameters(options))
        }
        val queryString = parameters.joinToString(separator = "&") { (name, value) ->
            "$name=${PercentEncoding.encode(value)}"
        }
        return "$WIDGET_URL?$queryString"
    }

    private fun productParameters(options: FintocWidgetOptions): List<Pair<String, String>> = when (options) {
        is FintocWidgetOptions.Payments -> listOf(
            "session_token" to options.sessionToken,
        )
        is FintocWidgetOptions.Movements -> listOfNotNull(
            "holder_type" to options.holderType.code,
            "country" to options.country.code,
            options.linkToken?.let { linkToken -> "link_token" to linkToken },
            options.webhookUrl?.let { webhookUrl -> "webhook_url" to webhookUrl },
        )
        is FintocWidgetOptions.Subscriptions -> listOf(
            "holder_type" to options.holderType.code,
            "country" to options.country.code,
            "widget_token" to options.widgetToken,
        )
    }
}
