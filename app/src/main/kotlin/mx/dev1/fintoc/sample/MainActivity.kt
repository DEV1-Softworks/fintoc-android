package mx.dev1.fintoc.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.dev1.fintoc.sample.ui.SampleScreen
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.Fintoc

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FintocSampleTheme {
                SampleScreen(isSdkInitialized = Fintoc.isInitialized)
            }
        }
    }
}
