package mx.dev1.fintoc.sdk.presentation.host

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.domain.widget.FintocLinkIntentResult
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult

/**
 * Opens the Widget in a screen of its own, for apps that do not use Compose. Apps that do should use
 * [mx.dev1.fintoc.sdk.presentation.widget.FintocWidget] instead.
 *
 * With the Activity Result API:
 *
 * ```
 * private val fintocWidget = registerForActivityResult(FintocWidgetContract()) { result ->
 *     when (result) {
 *         is FintocWidgetResult.Succeeded -> checkThePaymentOnYourBackend()
 *         FintocWidgetResult.Exited -> Unit
 *     }
 * }
 *
 * fintocWidget.launch(FintocWidgetOptions.Payments(sessionToken = tokenFromYourBackend))
 * ```
 *
 * Without it, use the two methods of this class directly:
 *
 * ```
 * startActivityForResult(FintocWidgetContract().createIntent(this, options), REQUEST_FINTOC)
 * // and in onActivityResult:
 * val result = FintocWidgetContract().parseResult(resultCode, data)
 * ```
 *
 * Things to know:
 * - Call [Fintoc.initialize] first. [createIntent] fails right away, with a clear message, if you did not.
 * - The screen is private to your app, hides itself from screenshots and the recent apps list, and keeps the Widget
 *   running through rotation and other configuration changes.
 * - After a success the screen stays open, so the user can read the confirmation or download a voucher. Its button then
 *   says "Done" and the result reaches your app when the user taps it or goes back.
 * - Only the end of the flow is reported. To follow every event of the Widget, use the composable.
 * - The screen follows [mx.dev1.fintoc.sdk.FintocConfiguration.language] for its own texts.
 */
public class FintocWidgetContract : ActivityResultContract<FintocWidgetOptions, FintocWidgetResult>() {

    /** @throws IllegalStateException If [Fintoc.initialize] has not been called. */
    override fun createIntent(context: Context, input: FintocWidgetOptions): Intent {
        Fintoc.requireKoin()
        val requestId = FintocWidgetRequests.register(input)
        return Intent(context, FintocWidgetActivity::class.java).putExtra(EXTRA_REQUEST_ID, requestId)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FintocWidgetResult {
        if (resultCode != Activity.RESULT_OK) return FintocWidgetResult.Exited

        val exchangeToken = intent?.getStringExtra(EXTRA_EXCHANGE_TOKEN)
        val linkIntent = if (exchangeToken.isNullOrBlank()) {
            null
        } else {
            FintocLinkIntentResult(exchangeToken = exchangeToken, id = intent.getStringExtra(EXTRA_LINK_INTENT_ID))
        }
        return FintocWidgetResult.Succeeded(linkIntent)
    }

    internal companion object {
        const val EXTRA_REQUEST_ID = "mx.dev1.fintoc.sdk.extra.REQUEST_ID"
        const val EXTRA_EXCHANGE_TOKEN = "mx.dev1.fintoc.sdk.extra.EXCHANGE_TOKEN"
        const val EXTRA_LINK_INTENT_ID = "mx.dev1.fintoc.sdk.extra.LINK_INTENT_ID"

        fun resultIntent(linkIntent: FintocLinkIntentResult?): Intent = Intent().apply {
            if (linkIntent != null) {
                putExtra(EXTRA_EXCHANGE_TOKEN, linkIntent.exchangeToken)
                linkIntent.id?.let { linkIntentId -> putExtra(EXTRA_LINK_INTENT_ID, linkIntentId) }
            }
        }
    }
}
