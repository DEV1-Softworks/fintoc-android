package mx.dev1.fintoc.sdk.presentation.host

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.presentation.widget.FintocWidget

/**
 * Hosts the Widget for apps that do not use Compose. Open it with [FintocWidgetContract], never directly.
 *
 * The screen is not exported, so no other app can start it. It hides itself from screenshots and the recent apps list,
 * because the Widget asks for bank credentials, and it handles configuration changes itself, so a payment in progress
 * survives a rotation instead of starting over.
 */
internal class FintocWidgetActivity : ComponentActivity() {

    private var requestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val koin = Fintoc.koinOrNull()
        val currentRequestId = intent.getStringExtra(FintocWidgetContract.EXTRA_REQUEST_ID)
        val options = currentRequestId?.let { requestId -> koin?.get<FintocWidgetRequests>()?.find(requestId) }
        if (koin == null || currentRequestId == null || options == null) {
            // Typically the system restored this screen after the process died: the session token is gone, and so
            // may be the initialization of the SDK.
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        requestId = currentRequestId

        val language = koin.get<FintocConfiguration>().language
        setContent {
            FintocWidgetHostTheme {
                var hasSucceeded by remember { mutableStateOf(false) }
                FintocWidgetHostScreen(
                    hasSucceeded = hasSucceeded,
                    language = language,
                    onLeave = ::finish,
                    content = { modifier ->
                        FintocWidget(
                            options = options,
                            onEvent = { event ->
                                when (event) {
                                    is FintocWidgetEvent.Succeeded -> {
                                        // Set now, so that going back after a success also reports it.
                                        val resultIntent = FintocWidgetContract.resultIntent(event.linkIntent)
                                        setResult(Activity.RESULT_OK, resultIntent)
                                        hasSucceeded = true
                                    }
                                    FintocWidgetEvent.Exited -> {
                                        // A success that was already reported must not be turned into a cancellation.
                                        if (!hasSucceeded) setResult(Activity.RESULT_CANCELED)
                                        finish()
                                    }
                                    is FintocWidgetEvent.Occurred -> Unit
                                }
                            },
                            modifier = modifier,
                        )
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        val finishedRequestId = requestId
        if (isFinishing && finishedRequestId != null) {
            Fintoc.koinOrNull()?.get<FintocWidgetRequests>()?.remove(finishedRequestId)
        }
        super.onDestroy()
    }
}
