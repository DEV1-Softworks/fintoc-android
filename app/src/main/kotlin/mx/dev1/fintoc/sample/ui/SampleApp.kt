package mx.dev1.fintoc.sample.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.backend.SimulatedBackend
import mx.dev1.fintoc.sample.backend.parsePaymentsOptions
import mx.dev1.fintoc.sample.checkout.HostedCheckoutDemo
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.log.SampleEventLog
import mx.dev1.fintoc.sample.ui.screens.HomeScreen
import mx.dev1.fintoc.sample.ui.screens.HostedCheckoutScreen
import mx.dev1.fintoc.sample.ui.screens.TokenScreen
import mx.dev1.fintoc.sample.ui.screens.WidgetScreen
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import mx.dev1.fintoc.sdk.domain.widget.FintocCountry
import mx.dev1.fintoc.sdk.domain.widget.FintocHolderType
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetContract
import mx.dev1.fintoc.sdk.presentation.widget.FintocWidget

/** A payment that was started with a pasted session token. It is only ever kept in memory. */
private class PaymentSession(val sessionToken: String, val simulateFailure: Boolean) {
    override fun toString(): String = "PaymentSession(sessionToken=<redacted>)"
}

/**
 * The whole sample: which screen is showing, and the state the demos share.
 *
 * @param returnedUri The address that last opened the app through one of its return links, for the hosted checkout.
 * @param onLanguageSelected Called when the language of the SDK's own texts is picked. `null` means follow the device.
 */
@Composable
fun SampleApp(
    settings: SampleSettings,
    returnedUri: Uri?,
    onLanguageSelected: (FintocLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(SampleDestination.HOME) }
    var language by rememberSaveable { mutableStateOf<FintocLanguage?>(null) }
    var session by remember { mutableStateOf<PaymentSession?>(null) }
    var activityResult by remember { mutableStateOf<FintocWidgetResult?>(null) }
    var hostedDemo by remember { mutableStateOf(HostedCheckoutDemo()) }
    var hostedOutcome by remember { mutableStateOf<FintocHostedCheckoutOutcome?>(null) }
    val widgetLauncher = rememberLauncherForActivityResult(FintocWidgetContract()) { result ->
        activityResult = result
    }

    // An address that reaches the app is read at once, against the addresses of the checkout on screen.
    LaunchedEffect(returnedUri) {
        if (returnedUri != null) {
            hostedOutcome = hostedDemo.checkout.outcomeOf(returnedUri)
            destination = SampleDestination.HOSTED_CHECKOUT
        }
    }

    BackHandler(enabled = destination != SampleDestination.HOME) {
        if (session != null) {
            session = null
        } else {
            destination = SampleDestination.HOME
        }
    }

    when (destination) {
        SampleDestination.HOME -> HomeScreen(
            settings = settings,
            language = language,
            onLanguageSelected = { selected ->
                language = selected
                onLanguageSelected(selected)
            },
            onOpen = { selected -> destination = selected },
            modifier = modifier,
        )

        SampleDestination.MOVEMENTS -> {
            val log = remember { SampleEventLog() }
            val options = remember { FintocWidgetOptions.Movements(FintocHolderType.INDIVIDUAL, FintocCountry.MEXICO) }
            WidgetScreen(
                title = stringResource(R.string.demo_movements_title),
                log = log,
                onBack = { destination = SampleDestination.HOME },
                modifier = modifier,
            ) { widgetModifier ->
                FintocWidget(options = options, onEvent = log::record, modifier = widgetModifier)
            }
        }

        SampleDestination.PAYMENTS_OPTIONS,
        SampleDestination.PAYMENTS_PROVIDER,
        SampleDestination.PAYMENTS_ACTIVITY,
        -> {
            val title = stringResource(
                when (destination) {
                    SampleDestination.PAYMENTS_PROVIDER -> R.string.demo_payments_provider_title
                    SampleDestination.PAYMENTS_ACTIVITY -> R.string.demo_payments_activity_title
                    else -> R.string.demo_payments_options_title
                },
            )
            val startedSession = session
            if (startedSession == null) {
                TokenScreen(
                    title = title,
                    offersSimulatedFailure = destination == SampleDestination.PAYMENTS_PROVIDER,
                    lastResult = if (destination == SampleDestination.PAYMENTS_ACTIVITY) activityResult else null,
                    onStart = { sessionToken, simulateFailure ->
                        if (destination == SampleDestination.PAYMENTS_ACTIVITY) {
                            activityResult = null
                            parsePaymentsOptions(sessionToken)?.let(widgetLauncher::launch)
                        } else {
                            session = PaymentSession(sessionToken, simulateFailure)
                        }
                    },
                    onBack = { destination = SampleDestination.HOME },
                    modifier = modifier,
                )
            } else {
                PaymentWidget(
                    title = title,
                    session = startedSession,
                    usesProvider = destination == SampleDestination.PAYMENTS_PROVIDER,
                    onBack = { session = null },
                    modifier = modifier,
                )
            }
        }

        SampleDestination.HOSTED_CHECKOUT -> HostedCheckoutScreen(
            demo = hostedDemo,
            outcome = hostedOutcome,
            onOpenCheckout = { redirectUrl -> hostedDemo.open(context, redirectUrl) },
            onNewSecretValue = {
                hostedDemo = HostedCheckoutDemo()
                hostedOutcome = null
            },
            onBack = { destination = SampleDestination.HOME },
            modifier = modifier,
        )
    }
}

@Composable
private fun PaymentWidget(
    title: String,
    session: PaymentSession,
    usesProvider: Boolean,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val log = remember(session) { SampleEventLog() }
    WidgetScreen(title = title, log = log, onBack = onBack, modifier = modifier) { widgetModifier ->
        if (usesProvider) {
            val backend = remember(session) {
                SimulatedBackend(session.sessionToken, failFirstAttempt = session.simulateFailure)
            }
            FintocWidget(
                sessionTokenProvider = { backend.createSessionToken() },
                onEvent = log::record,
                modifier = widgetModifier,
            )
        } else {
            val options = remember(session) { FintocWidgetOptions.Payments(session.sessionToken) }
            FintocWidget(options = options, onEvent = log::record, modifier = widgetModifier)
        }
    }
}
