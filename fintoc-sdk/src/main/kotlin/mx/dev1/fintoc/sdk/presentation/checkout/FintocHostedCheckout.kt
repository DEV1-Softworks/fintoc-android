package mx.dev1.fintoc.sdk.presentation.checkout

import android.content.Context
import android.content.Intent
import android.net.Uri
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutReturnUrl
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutUrlPolicy
import org.koin.core.parameter.parametersOf

/**
 * Sends the customer to a Fintoc-hosted checkout page in a Custom Tab, and tells what came back.
 *
 * Your backend creates a Checkout Session with a `success_url` and a `cancel_url`, and Fintoc answers with a
 * `redirect_url` (such as `https://pay.fintoc.com/checkout/cs_…`). Give this class the same two addresses, open the
 * `redirect_url`, and Fintoc sends the customer back to one of them when they are done:
 *
 * ```
 * val nonce = UUID.randomUUID().toString() // keep it with the order, and send these addresses to your backend
 * val checkout = FintocHostedCheckout(
 *     successUrl = "https://merchant.com/pay/success?n=$nonce",
 *     cancelUrl = "https://merchant.com/pay/cancel?n=$nonce",
 * )
 *
 * checkout.open(this, redirectUrlFromYourBackend)
 *
 * // In the Activity that receives your addresses, in onCreate and in onNewIntent:
 * when (checkout.outcomeOf(intent)) {
 *     FintocHostedCheckoutOutcome.Succeeded -> showThatTheOrderIsBeingConfirmed()
 *     FintocHostedCheckoutOutcome.Cancelled -> showThatThePaymentWasCancelled()
 *     FintocHostedCheckoutOutcome.Unrelated -> Unit
 * }
 * ```
 *
 * Things to know:
 * - **A returned address is a hint, never proof.** Any app on the device can open your deep link, and the customer may
 *   close the tab and never come back. Confirm the payment with Fintoc webhooks on your backend before you fulfil an
 *   order, and refresh the status from your backend when your screen resumes.
 * - **Put an unguessable value in your own addresses**, like the `n` above. Each query parameter that your addresses
 *   carry must come back with the same value to count, so another app cannot fake a return without knowing it.
 * - Prefer verified Android App Links (`https` addresses of your own domain) to custom schemes. Any other app can claim
 *   a custom scheme, and would then receive the redirect, secret value included. A verified App Link cannot be claimed.
 * - Declare the Activity that receives the addresses with `launchMode="singleTask"`. Then coming back delivers the
 *   address to the Activity that is already open, through `onNewIntent`, and closes the Custom Tab. With the default
 *   launch mode you would get a second copy of your Activity on top of the first.
 * - Call [Fintoc.initialize] before [open]. [outcomeOf] needs nothing, so it also works when the system restores your
 *   Activity in a new process.
 *
 * @param successUrl The `success_url` you give Fintoc.
 * @param cancelUrl The `cancel_url` you give Fintoc.
 * @throws IllegalArgumentException If an address is not an absolute `https` address or a custom scheme of your app, or
 * if the two cannot be told apart. The message never repeats the address, which may hold your secret value.
 */
public class FintocHostedCheckout(successUrl: String, cancelUrl: String) {

    private val success = FintocHostedCheckoutReturnUrl.parse(successUrl, parameterName = "successUrl")
    private val cancel = FintocHostedCheckoutReturnUrl.parse(cancelUrl, parameterName = "cancelUrl")

    init {
        require(!success.isIndistinguishableFrom(cancel)) {
            "successUrl and cancelUrl cannot be told apart. They must differ in host, in path, or in a query " +
                "parameter that one of them carries."
        }
    }

    /**
     * Opens [redirectUrl] in a Custom Tab, or in the browser when it has no Custom Tabs.
     *
     * @param context The screen the customer is on. Any context works, an Activity is best.
     * @param redirectUrl The `redirect_url` of the Checkout Session. Only `https` addresses on a `fintoc.com`
     * subdomain are opened.
     * @return `false` when no app on the device can open it. Show your own message then.
     * @throws IllegalArgumentException If [redirectUrl] is not an `https` address on a `fintoc.com` subdomain. The
     * message never repeats it.
     * @throws IllegalStateException If [Fintoc.initialize] has not been called.
     */
    public fun open(context: Context, redirectUrl: String): Boolean {
        require(FintocHostedCheckoutUrlPolicy.isTrustedRedirectUrl(redirectUrl)) {
            "redirectUrl must be an https address on a fintoc.com subdomain, such as the redirect_url of a " +
                "Checkout Session."
        }
        return Fintoc.requireKoin().get<CustomTabLauncher> { parametersOf(context) }.launch(redirectUrl)
    }

    /** What the address that opened your app means. See [FintocHostedCheckoutOutcome]. */
    public fun outcomeOf(returnedUri: Uri?): FintocHostedCheckoutOutcome = outcomeOfUrl(returnedUri?.toString())

    /** What the address of this [intent] means, or [FintocHostedCheckoutOutcome.Unrelated] when it carries none. */
    public fun outcomeOf(intent: Intent?): FintocHostedCheckoutOutcome = outcomeOf(intent?.data)

    internal fun outcomeOfUrl(returnedUrl: String?): FintocHostedCheckoutOutcome {
        if (returnedUrl == null) return FintocHostedCheckoutOutcome.Unrelated

        val isSuccess = success.accepts(returnedUrl)
        val isCancel = cancel.accepts(returnedUrl)
        return when {
            // An address that satisfies both is ambiguous, so it is not acted on.
            isSuccess && !isCancel -> FintocHostedCheckoutOutcome.Succeeded
            isCancel && !isSuccess -> FintocHostedCheckoutOutcome.Cancelled
            else -> FintocHostedCheckoutOutcome.Unrelated
        }
    }

    /** Your addresses may hold a secret value, so they are never shown. */
    override fun toString(): String = "FintocHostedCheckout(successUrl=<redacted>, cancelUrl=<redacted>)"
}
