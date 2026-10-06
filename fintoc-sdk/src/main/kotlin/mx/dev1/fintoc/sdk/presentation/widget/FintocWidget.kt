package mx.dev1.fintoc.sdk.presentation.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetUrlBuilder
import mx.dev1.fintoc.sdk.presentation.localization.rememberFintocStrings
import org.koin.core.parameter.parametersOf

/**
 * Shows the Fintoc Widget, configured by [options], and reports what happens inside it through [onEvent].
 *
 * Call [Fintoc.initialize] first: the Widget uses the public key you configured there. The composable fills the space
 * it is given, so size it with [modifier].
 *
 * ```
 * FintocWidget(
 *     options = FintocWidgetOptions.Payments(sessionToken = sessionTokenFromYourBackend),
 *     onEvent = { event ->
 *         when (event) {
 *             is FintocWidgetEvent.Succeeded -> showReceipt()
 *             FintocWidgetEvent.Exited -> closeScreen()
 *             is FintocWidgetEvent.Occurred -> Unit
 *         }
 *     },
 *     modifier = Modifier.fillMaxSize(),
 * )
 * ```
 *
 * Things to know:
 * - [onEvent] runs on the main thread. **An event is not proof of payment**: confirm payments with Fintoc webhooks on
 *   your backend before you fulfil an order.
 * - The Widget loads again, from the start, when [options] change, and when the screen is recreated. To keep a payment
 *   in progress through rotation, let your activity handle the configuration change itself
 *   (`android:configChanges="orientation|screenSize|keyboardHidden"`).
 * - Links that leave Fintoc, such as the payment voucher, open in the browser. The Widget never browses elsewhere.
 * - If the Widget cannot be loaded, it is replaced by a message with a button to try again.
 *
 * @param options What the Widget should do. Each product validates its own data.
 * @param onEvent Receives every event the Widget reports.
 * @param modifier Layout of the Widget.
 * @throws IllegalStateException If [Fintoc.initialize] has not been called.
 */
@Composable
public fun FintocWidget(
    options: FintocWidgetOptions,
    onEvent: (FintocWidgetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val koin = remember { Fintoc.requireKoin() }
    val configuration = remember(koin) { koin.get<FintocConfiguration>() }
    val externalLinkLauncher = remember(koin, context) { koin.get<ExternalLinkLauncher> { parametersOf(context) } }
    val widgetUrl = remember(configuration, options) { FintocWidgetUrlBuilder.build(configuration.publicKey, options) }

    FintocWidgetWebView(
        url = widgetUrl,
        onEvent = onEvent,
        externalLinkLauncher = externalLinkLauncher,
        modifier = modifier,
        language = configuration.language,
    )
}

/**
 * Shows the Fintoc Widget for a payment, asking your backend for the session token when it is needed.
 *
 * Use this when the token is not at hand: Fintoc session tokens are created by your backend, belong to one payment
 * attempt, and cannot be reused. [sessionTokenProvider] runs once each time the composable enters the composition, and
 * again every time the user taps "try again" after a failure, so each attempt gets a fresh token. A progress indicator
 * covers the wait.
 *
 * ```
 * FintocWidget(
 *     sessionTokenProvider = { checkoutApi.createSessionToken(orderId) },
 *     onEvent = { event -> handle(event) },
 * )
 * ```
 *
 * Throw from [sessionTokenProvider] when your backend fails: the user sees a message with a button to try again. Handle
 * and log the failure inside the provider if you want to know about it. Returning a blank token, or a secret key
 * (`sk_…`), counts as a failure too.
 *
 * Everything said for the other [FintocWidget] overload applies here as well.
 *
 * @param sessionTokenProvider Gets a new session token from your backend, with the `suspend` call of your choice.
 * @param onEvent Receives every event the Widget reports.
 * @param modifier Layout of the Widget.
 * @throws IllegalStateException If [Fintoc.initialize] has not been called.
 */
@Composable
public fun FintocWidget(
    sessionTokenProvider: suspend () -> String,
    onEvent: (FintocWidgetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Read first, so a missing initialization fails before the provider calls your backend.
    val configuration = remember { Fintoc.requireKoin().get<FintocConfiguration>() }
    val strings = rememberFintocStrings(configuration.language)
    val latestSessionTokenProvider by rememberUpdatedState(sessionTokenProvider)
    var attempt by remember { mutableIntStateOf(0) }
    var tokenState by remember { mutableStateOf<FintocSessionTokenState>(FintocSessionTokenState.Loading) }

    LaunchedEffect(attempt) {
        tokenState = FintocSessionTokenState.Loading
        tokenState = loadPaymentsOptions { latestSessionTokenProvider() }
    }

    when (val state = tokenState) {
        FintocSessionTokenState.Loading -> Box(modifier = modifier) { FintocWidgetLoading(strings) }
        FintocSessionTokenState.Failed -> Box(modifier = modifier) {
            FintocWidgetFailure(strings, onRetry = { attempt += 1 })
        }
        is FintocSessionTokenState.Ready -> {
            FintocWidget(options = state.options, onEvent = onEvent, modifier = modifier)
        }
    }
}
