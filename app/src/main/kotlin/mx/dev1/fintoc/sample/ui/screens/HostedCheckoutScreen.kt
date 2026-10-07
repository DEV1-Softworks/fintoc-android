package mx.dev1.fintoc.sample.ui.screens

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.checkout.HostedCheckoutDemo
import mx.dev1.fintoc.sample.checkout.HostedOpenResult
import mx.dev1.fintoc.sample.ui.components.SampleScaffold
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome

/**
 * The hosted checkout: the two addresses to give Fintoc, a place to paste the `redirect_url` of the session, and what
 * came back to the app.
 *
 * @param outcome What the last address that reached the app meant, or `null` while none has.
 */
@Composable
fun HostedCheckoutScreen(
    demo: HostedCheckoutDemo,
    outcome: FintocHostedCheckoutOutcome?,
    onOpenCheckout: (redirectUrl: String) -> HostedOpenResult,
    onNewSecretValue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var redirectUrl by remember { mutableStateOf("") }
    var openResult by remember { mutableStateOf<HostedOpenResult?>(null) }

    val title = stringResource(R.string.demo_hosted_title)
    SampleScaffold(title = title, onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.hosted_explanation), style = MaterialTheme.typography.bodyMedium)
            AddressRow(label = R.string.hosted_success_label, address = demo.successUrl)
            AddressRow(label = R.string.hosted_cancel_label, address = demo.cancelUrl)
            OutlinedButton(onClick = onNewSecretValue) { Text(text = stringResource(R.string.hosted_new_secret)) }

            OutlinedTextField(
                value = redirectUrl,
                onValueChange = {
                    redirectUrl = it
                    openResult = null
                },
                label = { Text(text = stringResource(R.string.hosted_redirect_label)) },
                isError = openResult == HostedOpenResult.NOT_TRUSTED || openResult == HostedOpenResult.NO_BROWSER,
                supportingText = when (openResult) {
                    HostedOpenResult.NOT_TRUSTED -> {
                        { Text(text = stringResource(R.string.hosted_error_untrusted)) }
                    }
                    HostedOpenResult.NO_BROWSER -> {
                        { Text(text = stringResource(R.string.hosted_error_no_browser)) }
                    }
                    else -> null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { openResult = onOpenCheckout(redirectUrl) }, enabled = redirectUrl.isNotBlank()) {
                Text(text = stringResource(R.string.hosted_open))
            }

            Text(
                text = stringResource(
                    when (outcome) {
                        null -> R.string.hosted_outcome_none
                        FintocHostedCheckoutOutcome.Succeeded -> R.string.hosted_outcome_succeeded
                        FintocHostedCheckoutOutcome.Cancelled -> R.string.hosted_outcome_cancelled
                        FintocHostedCheckoutOutcome.Unrelated -> R.string.hosted_outcome_unrelated
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.hosted_reminder), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** An address you can read, select and copy. */
@Composable
private fun AddressRow(@StringRes label: Int, address: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val labelText = stringResource(label)
    val copyDescription = stringResource(R.string.hosted_copy_description, labelText)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = labelText, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(text = address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            OutlinedButton(
                onClick = {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(labelText, address))) }
                },
                modifier = Modifier.semantics { contentDescription = copyDescription },
            ) {
                Text(text = stringResource(R.string.hosted_copy))
            }
        }
    }
}
