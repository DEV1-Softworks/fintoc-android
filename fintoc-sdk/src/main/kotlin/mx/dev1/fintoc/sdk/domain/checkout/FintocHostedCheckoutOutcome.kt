package mx.dev1.fintoc.sdk.domain.checkout

/**
 * What an address that reached your app means for a hosted checkout, as decided by
 * [mx.dev1.fintoc.sdk.presentation.checkout.FintocHostedCheckout.outcomeOf].
 *
 * **None of these is proof of anything.** Any app on the device can open your deep link, so [Succeeded] only says that
 * someone opened your success address. Confirm the payment with Fintoc webhooks on your backend before you fulfil an
 * order, as Fintoc itself advises: the customer may also close the tab and never reach your `success_url`.
 */
public sealed interface FintocHostedCheckoutOutcome {

    /** The address is your `successUrl`: Fintoc sent the customer back after what it saw as a successful payment. */
    public data object Succeeded : FintocHostedCheckoutOutcome

    /** The address is your `cancelUrl`: the customer left the payment and came back. */
    public data object Cancelled : FintocHostedCheckoutOutcome

    /** The address is neither of your two, or there was none. It is not something to react to. */
    public data object Unrelated : FintocHostedCheckoutOutcome
}
