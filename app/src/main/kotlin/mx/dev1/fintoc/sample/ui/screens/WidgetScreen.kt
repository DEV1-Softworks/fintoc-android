package mx.dev1.fintoc.sample.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.dev1.fintoc.sample.log.SampleEventLog
import mx.dev1.fintoc.sample.ui.components.EventLogPanel
import mx.dev1.fintoc.sample.ui.components.SampleScaffold

/**
 * A Widget with the events it reports underneath. The Widget is [widget], which receives the space it may fill, so the
 * same screen serves every demo.
 */
@Composable
fun WidgetScreen(
    title: String,
    log: SampleEventLog,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    widget: @Composable (Modifier) -> Unit,
) {
    SampleScaffold(title = title, onBack = onBack, modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding)) {
            widget(Modifier.weight(1f).fillMaxWidth())
            EventLogPanel(log = log)
        }
    }
}
