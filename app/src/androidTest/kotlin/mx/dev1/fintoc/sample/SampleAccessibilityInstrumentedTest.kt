package mx.dev1.fintoc.sample

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import mx.dev1.fintoc.sample.checkout.HostedCheckoutDemo
import mx.dev1.fintoc.sample.checkout.HostedOpenResult
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.screens.HomeScreen
import mx.dev1.fintoc.sample.ui.screens.HostedCheckoutScreen
import mx.dev1.fintoc.sample.ui.screens.TokenScreen
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google's Accessibility Test Framework over the screens of the sample, on a real device, at the default settings and
 * at 200% font size with 150% display size. Any error fails the test. The Widget itself is a web page of Fintoc's, so
 * it is not part of this check.
 */
@RunWith(AndroidJUnit4::class)
class SampleAccessibilityInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val validator = AccessibilityValidator()
        .setCheckPreset(AccessibilityCheckPreset.LATEST)
        .setRunChecksFromRootView(true)
        .setThrowExceptionForErrors(true)

    @Before
    fun checkEveryInteraction() {
        composeRule.enableAccessibilityChecks(validator)
    }

    private fun assertNoAccessibilityErrors() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { validator.check(composeRule.activity.window.decorView) }
    }

    @Composable
    private fun AtLargestAccessibilitySizes(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * 1.5f, fontScale = 2f),
            content = content,
        )
    }

    @Composable
    private fun Home() = HomeScreen(
        settings = SampleSettings(SampleSettings.PLACEHOLDER_PUBLIC_KEY),
        language = null,
        onLanguageSelected = {},
        onOpen = {},
    )

    @Composable
    private fun Token() = TokenScreen(
        title = "Payments",
        offersSimulatedFailure = true,
        lastResult = null,
        onStart = { _, _ -> },
        onBack = {},
    )

    @Composable
    private fun Hosted() = HostedCheckoutScreen(
        demo = HostedCheckoutDemo(secretValue = "abc123"),
        outcome = FintocHostedCheckoutOutcome.Unrelated,
        onOpenCheckout = { HostedOpenResult.NOT_TRUSTED },
        onNewSecretValue = {},
        onBack = {},
    )

    private fun show(large: Boolean, screen: @Composable () -> Unit) {
        composeRule.setContent {
            FintocSampleTheme {
                if (large) AtLargestAccessibilitySizes(screen) else screen()
            }
        }
    }

    @Test
    fun homePassesTheChecks() {
        show(large = false) { Home() }
        assertNoAccessibilityErrors()
    }

    @Test
    fun homePassesTheChecksAtTheLargestSizes() {
        show(large = true) { Home() }
        assertNoAccessibilityErrors()
    }

    @Test
    fun theTokenScreenPassesTheChecks() {
        show(large = false) { Token() }
        assertNoAccessibilityErrors()
    }

    @Test
    fun theTokenScreenPassesTheChecksAtTheLargestSizes() {
        show(large = true) { Token() }
        assertNoAccessibilityErrors()
    }

    @Test
    fun theHostedCheckoutScreenPassesTheChecks() {
        show(large = false) { Hosted() }
        assertNoAccessibilityErrors()
    }

    @Test
    fun theHostedCheckoutScreenPassesTheChecksAtTheLargestSizes() {
        show(large = true) { Hosted() }
        assertNoAccessibilityErrors()
    }
}
