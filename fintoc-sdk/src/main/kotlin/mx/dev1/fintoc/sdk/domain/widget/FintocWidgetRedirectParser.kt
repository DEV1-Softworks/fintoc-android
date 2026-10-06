package mx.dev1.fintoc.sdk.domain.widget

/**
 * Turns the `fintocwidget://` redirects of the Widget into [FintocWidgetEvent]s.
 *
 * - `fintocwidget://succeeded` becomes [FintocWidgetEvent.Succeeded]. For Movements it carries
 *   `?object=link_intent&exchange_token=…&id=…`.
 * - `fintocwidget://exit` becomes [FintocWidgetEvent.Exited].
 * - `fintocwidget://event/{name}?timestamp=…&…` becomes [FintocWidgetEvent.Occurred].
 *
 * The parser works on the raw text and never throws. Anything unexpected (wrong scheme, unknown action, a missing or
 * odd event name, an oversized redirect) yields `null` instead, so the page can never crash the host app.
 */
internal object FintocWidgetRedirectParser {
    private const val SCHEME_PREFIX = "fintocwidget://"
    private const val ACTION_SUCCEEDED = "succeeded"
    private const val ACTION_EXIT = "exit"
    private const val ACTION_EVENT = "event"
    private const val LINK_INTENT_OBJECT = "link_intent"
    private const val TIMESTAMP_PARAMETER = "timestamp"
    private val EVENT_NAME_PATTERN = Regex("[A-Za-z0-9_.-]{1,64}")

    /** The Widget writes `null`, `undefined` and nested objects as these literal words, which carry no information. */
    private val UNINFORMATIVE_VALUES = setOf("null", "undefined", "[object Object]")

    /** Redirects longer than this are ignored. Real ones are a few hundred characters at most. */
    private const val MAX_REDIRECT_LENGTH = 8_192

    /** Parameters beyond this count are ignored. The Widget sends a handful. */
    private const val MAX_PARAMETERS = 64

    /** `true` for any `fintocwidget://` address, even one [parse] ignores, so the WebView never navigates to it. */
    fun isRedirect(rawUrl: String): Boolean = rawUrl.startsWith(SCHEME_PREFIX, ignoreCase = true)

    fun parse(rawUrl: String): FintocWidgetEvent? {
        if (!isRedirect(rawUrl) || rawUrl.length > MAX_REDIRECT_LENGTH) return null

        val withoutFragment = rawUrl.substringBefore('#')
        val target = withoutFragment.substring(SCHEME_PREFIX.length).substringBefore('?')
        val parameters = parseParameters(withoutFragment.substringAfter('?', missingDelimiterValue = ""))
        val action = target.substringBefore('/').lowercase()

        return when (action) {
            ACTION_SUCCEEDED -> parseSucceeded(parameters)
            ACTION_EXIT -> FintocWidgetEvent.Exited
            ACTION_EVENT -> parseOccurred(
                eventPath = target.substringAfter('/', missingDelimiterValue = ""),
                parameters = parameters,
            )
            else -> null
        }
    }

    private fun parseSucceeded(parameters: Map<String, String>): FintocWidgetEvent.Succeeded {
        val exchangeToken = parameters["exchange_token"]
        val linkIntent = if (parameters["object"] == LINK_INTENT_OBJECT && !exchangeToken.isNullOrBlank()) {
            FintocLinkIntentResult(exchangeToken = exchangeToken, id = parameters["id"]?.takeIf { it.isNotBlank() })
        } else {
            null
        }
        return FintocWidgetEvent.Succeeded(linkIntent)
    }

    private fun parseOccurred(eventPath: String, parameters: Map<String, String>): FintocWidgetEvent.Occurred? {
        val name = PercentDecoding.decode(eventPath)
        if (!EVENT_NAME_PATTERN.matches(name)) return null

        return FintocWidgetEvent.Occurred(
            name = name,
            timestampMillis = parameters[TIMESTAMP_PARAMETER]?.toLongOrNull(),
            metadata = parameters - TIMESTAMP_PARAMETER,
        )
    }

    /**
     * The first occurrence of a key wins, so a later duplicate (for example from an unencoded `&` inside a value)
     * cannot override what the Widget sent first, even when that first value was dropped as uninformative.
     */
    private fun parseParameters(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()

        val parameters = LinkedHashMap<String, String>()
        val seenKeys = HashSet<String>()
        for (pair in queryString.split('&')) {
            if (seenKeys.size >= MAX_PARAMETERS) break
            val key = PercentDecoding.decode(pair.substringBefore('='))
            if (key.isEmpty() || !seenKeys.add(key)) continue
            val value = PercentDecoding.decode(pair.substringAfter('=', missingDelimiterValue = ""))
            if (value !in UNINFORMATIVE_VALUES) parameters[key] = value
        }
        return parameters
    }
}
