package mx.dev1.fintoc.sdk.presentation.host

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.presentation.localization.rememberFintocStrings

/** Colors for the screen that hosts the Widget: the light or dark baseline of Material 3, following the device. */
@Composable
internal fun FintocWidgetHostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

/**
 * The frame around the Widget in [FintocWidgetActivity]: a title and a button to leave, which reads "Done" once the
 * flow has succeeded. The keyboard and the system bars are accounted for, so no field of the Widget ends up under them.
 *
 * The Widget itself is [content], which receives the space that is left.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in the Material 3 version of the BOM.
@Composable
internal fun FintocWidgetHostScreen(
    hasSucceeded: Boolean,
    language: FintocLanguage?,
    onLeave: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val strings = rememberFintocStrings(language)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.get(R.string.fintoc_widget_activity_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                actions = {
                    TextButton(onClick = onLeave) {
                        Text(
                            text = strings.get(
                                if (hasSucceeded) R.string.fintoc_widget_done else R.string.fintoc_widget_close,
                            ),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        content(Modifier.padding(contentPadding).consumeWindowInsets(contentPadding))
    }
}
