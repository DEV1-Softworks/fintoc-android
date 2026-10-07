package mx.dev1.fintoc.sample.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.SampleDestination
import mx.dev1.fintoc.sample.ui.components.SampleScaffold
import mx.dev1.fintoc.sdk.FintocLanguage

private class Demo(
    val destination: SampleDestination,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

private val DEMOS = listOf(
    Demo(SampleDestination.MOVEMENTS, R.string.demo_movements_title, R.string.demo_movements_description),
    Demo(
        SampleDestination.PAYMENTS_OPTIONS,
        R.string.demo_payments_options_title,
        R.string.demo_payments_options_description,
    ),
    Demo(
        SampleDestination.PAYMENTS_PROVIDER,
        R.string.demo_payments_provider_title,
        R.string.demo_payments_provider_description,
    ),
    Demo(
        SampleDestination.PAYMENTS_ACTIVITY,
        R.string.demo_payments_activity_title,
        R.string.demo_payments_activity_description,
    ),
    Demo(SampleDestination.HOSTED_CHECKOUT, R.string.demo_hosted_title, R.string.demo_hosted_description),
)

/** The list of demos, and the picker for the language of the SDK's own texts. */
@OptIn(ExperimentalLayoutApi::class) // FlowRow lays the language chips out on as many lines as the screen needs.
@Composable
fun HomeScreen(
    settings: SampleSettings,
    language: FintocLanguage?,
    onLanguageSelected: (FintocLanguage?) -> Unit,
    onOpen: (SampleDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.sample_title)
    SampleScaffold(title = title, onBack = null, modifier = modifier) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)

            if (settings.usesPlaceholderKey) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.home_placeholder_key_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_demos_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            DEMOS.forEach { demo ->
                Card(onClick = { onOpen(demo.destination) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(demo.title), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(demo.description), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                text = stringResource(R.string.language_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.language_note), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip(R.string.language_automatic, selected = language == null) { onLanguageSelected(null) }
                LanguageChip(R.string.language_english, language == FintocLanguage.ENGLISH) {
                    onLanguageSelected(FintocLanguage.ENGLISH)
                }
                LanguageChip(R.string.language_spanish, language == FintocLanguage.SPANISH) {
                    onLanguageSelected(FintocLanguage.SPANISH)
                }
                LanguageChip(R.string.language_french, language == FintocLanguage.FRENCH) {
                    onLanguageSelected(FintocLanguage.FRENCH)
                }
                LanguageChip(R.string.language_portuguese, language == FintocLanguage.PORTUGUESE) {
                    onLanguageSelected(FintocLanguage.PORTUGUESE)
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text = stringResource(label)) })
}
