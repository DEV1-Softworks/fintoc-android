package mx.dev1.fintoc.sdk.presentation.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.presentation.localization.FintocStrings

/** Shown while the page, or the session token that the page needs, is on its way. */
@Composable
internal fun FintocWidgetLoading(strings: FintocStrings, modifier: Modifier = Modifier) {
    val loadingDescription = strings.get(R.string.fintoc_widget_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = loadingDescription
                liveRegion = LiveRegionMode.Polite
            },
        )
    }
}

/**
 * Shown instead of the Widget when it cannot be displayed, with a way to try again.
 *
 * It scrolls, so that at the largest font and display sizes the message and the button stay reachable on a small
 * screen instead of being cut off.
 *
 * @param messageId What went wrong. The default is a page that could not be loaded.
 */
@Composable
internal fun FintocWidgetFailure(
    strings: FintocStrings,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageId: Int = R.string.fintoc_widget_error_message,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.get(messageId),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Button(onClick = onRetry) {
                Text(text = strings.get(R.string.fintoc_widget_error_retry))
            }
        }
    }
}
