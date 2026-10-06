package mx.dev1.fintoc.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mx.dev1.fintoc.sample.config.SampleSdk
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.SampleApp
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme

/**
 * The only screen of the sample. It is `singleTask`, so a hosted checkout that comes back through one of the app's
 * return links reaches this same Activity through [onNewIntent] and closes the Custom Tab, instead of opening a second
 * copy of the app. It also handles configuration changes itself, so a Widget survives a rotation.
 */
class MainActivity : ComponentActivity() {

    private val settings = SampleSettings.fromBuildConfig()
    private var returnedUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        returnedUri = returnAddressOf(intent)
        setContent {
            FintocSampleTheme {
                SampleApp(
                    settings = settings,
                    returnedUri = returnedUri,
                    onLanguageSelected = { language -> SampleSdk.initialize(applicationContext, settings, language) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        returnedUri = returnAddressOf(intent)
    }

    /** The address of a return link, or `null` for an ordinary launch. */
    private fun returnAddressOf(intent: Intent?): Uri? = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
}
