package mx.dev1.fintoc.sample.checkout

import android.content.Context
import java.util.UUID
import mx.dev1.fintoc.sdk.presentation.checkout.FintocHostedCheckout

/** What happened when the sample tried to open a hosted checkout. */
enum class HostedOpenResult {
    /** A browser took the customer to the page. */
    OPENED,

    /** The address is not an `https` address on a `fintoc.com` subdomain, so the SDK refused it. */
    NOT_TRUSTED,

    /** Nothing on the device can open the address. */
    NO_BROWSER,
}

/**
 * The addresses Fintoc sends the customer back to, and the SDK helper that reads them.
 *
 * The sample uses a custom scheme (`fintocsample://`) because a verified App Link needs a domain of your own. A real
 * app should prefer an App Link: any other app can claim a custom scheme. The unguessable [secretValue] travels in both
 * addresses, so the SDK only accepts a return that carries it.
 */
class HostedCheckoutDemo(val secretValue: String = UUID.randomUUID().toString()) {

    val successUrl: String = "$SCHEME://$HOST/success?n=$secretValue"
    val cancelUrl: String = "$SCHEME://$HOST/cancel?n=$secretValue"

    val checkout: FintocHostedCheckout = FintocHostedCheckout(successUrl = successUrl, cancelUrl = cancelUrl)

    /** Opens the `redirect_url` of a Checkout Session. The SDK decides whether the address may be opened. */
    fun open(context: Context, redirectUrl: String): HostedOpenResult = try {
        if (checkout.open(context, redirectUrl.trim())) HostedOpenResult.OPENED else HostedOpenResult.NO_BROWSER
    } catch (refused: IllegalArgumentException) {
        HostedOpenResult.NOT_TRUSTED
    }

    companion object {
        const val SCHEME = "fintocsample"
        const val HOST = "checkout"
    }
}
