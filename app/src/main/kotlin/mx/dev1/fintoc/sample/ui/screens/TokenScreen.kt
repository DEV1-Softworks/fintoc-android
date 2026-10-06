package mx.dev1.fintoc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.backend.parsePaymentsOptions
import mx.dev1.fintoc.sample.ui.components.SampleScaffold
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult

/**
 * Asks for the session token of a payment, which only your backend can create. Used by the three payment demos.
 *
 * The token is kept in memory only, never in saved state.
 *
 * @param offersSimulatedFailure Shows the switch that makes the simulated backend fail once, for the provider demo.
 * @param lastResult How the last screen of its own ended, for the Activity demo.
 */
@Composable
fun TokenScreen(
    title: String,
    offersSimulatedFailure: Boolean,
    lastResult: FintocWidgetResult?,
    onStart: (sessionToken: String, simulateFailure: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pastedToken by remember { mutableStateOf("") }
    var simulateFailure by remember { mutableStateOf(false) }
    var showInvalid by remember { mutableStateOf(false) }

    SampleScaffold(title = title, onBack = onBack, modifier = modifier) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.token_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.token_explanation), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = pastedToken,
                onValueChange = {
                    pastedToken = it
                    showInvalid = false
                },
                label = { Text(text = stringResource(R.string.token_field_label)) },
                isError = showInvalid,
                supportingText = if (showInvalid) {
                    { Text(text = stringResource(R.string.token_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (offersSimulatedFailure) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(checked = simulateFailure, onCheckedChange = { simulateFailure = it })
                    Text(
                        text = stringResource(R.string.token_fail_first),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Button(
                onClick = {
                    val options = parsePaymentsOptions(pastedToken)
                    if (options == null) showInvalid = true else onStart(pastedToken.trim(), simulateFailure)
                },
            ) {
                Text(text = stringResource(R.string.token_start))
            }
            when (lastResult) {
                is FintocWidgetResult.Succeeded -> Text(text = stringResource(R.string.token_result_succeeded))
                FintocWidgetResult.Exited -> Text(text = stringResource(R.string.token_result_exited))
                null -> Unit
            }
        }
    }
}
