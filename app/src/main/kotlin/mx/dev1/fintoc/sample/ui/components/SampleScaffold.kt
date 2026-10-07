package mx.dev1.fintoc.sample.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import mx.dev1.fintoc.R

/**
 * A title bar with a text "Back" button, and the room that is left. The button has a visible label, not an icon only,
 * so it is easy to find with a screen reader and to tap.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in the Material 3 version of the BOM.
@Composable
fun SampleScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = title, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(text = stringResource(R.string.sample_back)) }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content,
    )
}
