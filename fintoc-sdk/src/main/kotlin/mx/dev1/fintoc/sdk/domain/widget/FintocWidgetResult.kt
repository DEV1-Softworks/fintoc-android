package mx.dev1.fintoc.sdk.domain.widget

/**
 * How a screen opened with [mx.dev1.fintoc.sdk.presentation.host.FintocWidgetContract] ended.
 *
 * Like every event of the Widget, neither result proves that money moved. Confirm payments with Fintoc webhooks on
 * your backend before you fulfil an order.
 */
public sealed interface FintocWidgetResult {

    /**
     * The flow finished successfully from the Widget's point of view.
     *
     * @property linkIntent Present only when the Widget connected a bank account (`Movements`). Send its
     * `exchangeToken` to your backend right away.
     */
    public data class Succeeded(val linkIntent: FintocLinkIntentResult? = null) : FintocWidgetResult

    /** The user left before finishing: they closed the Widget, tapped "Close" or went back. */
    public data object Exited : FintocWidgetResult
}
