package mx.dev1.fintoc.sdk.domain.widget

/**
 * Something the Widget reported to the app.
 *
 * None of these events proves that money moved. A user (or a compromised device) can fake what a WebView reports, so
 * confirm payments with Fintoc webhooks on your backend before you fulfil an order.
 */
public sealed interface FintocWidgetEvent {

    /**
     * The flow finished successfully from the Widget's point of view.
     *
     * @property linkIntent Present only when the Widget connected a bank account (`Movements`). It is `null` for
     * payments and subscriptions.
     */
    public data class Succeeded(val linkIntent: FintocLinkIntentResult? = null) : FintocWidgetEvent

    /** The user closed the Widget before finishing the flow. */
    public data object Exited : FintocWidgetEvent

    /**
     * The user did something tracked in the Widget, such as opening the bank selection view.
     *
     * @property name Event name exactly as the Widget sent it. Always set, even when [type] is `null`.
     * @property timestampMillis Unix time in milliseconds at which the Widget emitted the event, when it sent one.
     * @property metadata Every other value the Widget sent, as text. Fintoc documents only `timestamp`, so treat any
     * key you do not recognize as optional. Values the Widget could not serialize (`null`, `undefined` and nested
     * objects) are left out.
     */
    public data class Occurred(
        val name: String,
        val timestampMillis: Long? = null,
        val metadata: Map<String, String> = emptyMap(),
    ) : FintocWidgetEvent {

        /** The documented event this is, or `null` when Fintoc added an event this SDK does not know yet. */
        public val type: FintocWidgetEventType?
            get() = FintocWidgetEventType.fromCode(name)

        /** Lists the metadata keys but not their values, which Fintoc does not document and may be sensitive. */
        override fun toString(): String =
            "Occurred(name=$name, timestampMillis=$timestampMillis, metadataKeys=${metadata.keys})"
    }
}

/**
 * Result of connecting a bank account with the `Movements` product.
 *
 * Send [exchangeToken] to **your backend** right away. Only your backend can exchange it, with its secret key, for
 * the link and its `link_token`. It is short-lived and single purpose, and `toString()` hides it.
 *
 * @property exchangeToken Temporary key that your backend exchanges for the link.
 * @property id Identifier of the link intent, when the Widget sent it.
 */
public data class FintocLinkIntentResult(
    val exchangeToken: String,
    val id: String? = null,
) {
    init {
        require(exchangeToken.isNotBlank()) { "exchangeToken must not be blank." }
    }

    override fun toString(): String = "FintocLinkIntentResult(exchangeToken=<redacted>, id=$id)"
}
