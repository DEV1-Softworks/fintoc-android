package mx.dev1.fintoc.sample.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.log.SampleEventLog

/** What the Widget has reported so far. It scrolls, and stays small so the Widget keeps most of the screen. */
@Composable
fun EventLogPanel(log: SampleEventLog, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.log_heading),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Column(modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState())) {
            if (log.entries.isEmpty()) {
                Text(text = stringResource(R.string.log_empty), style = MaterialTheme.typography.bodyMedium)
            }
            log.entries.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
