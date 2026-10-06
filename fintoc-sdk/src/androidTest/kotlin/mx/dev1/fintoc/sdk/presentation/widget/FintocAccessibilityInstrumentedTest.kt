package mx.dev1.fintoc.sdk.presentation.widget

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetHostScreen
import mx.dev1.fintoc.sdk.presentation.host.FintocWidgetHostTheme
import mx.dev1.fintoc.sdk.presentation.localization.FintocStrings
import mx.dev1.fintoc.sdk.presentation.localization.resourcesIn
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs Google's Accessibility Test Framework over the screens of the SDK on a real device, at the default settings and
 * at the largest font and display sizes a user can pick, which is where layouts usually break. Any finding of type
 * ERROR fails the test.
 */
@RunWith(AndroidJUnit4::class)
class FintocAccessibilityInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val strings = FintocStrings(targetContext.resourcesIn(FintocLanguage.ENGLISH))

    private val validator = AccessibilityValidator()
        .setCheckPreset(AccessibilityCheckPreset.LATEST)
        .setRunChecksFromRootView(true)
        .setThrowExceptionForErrors(true)

    @Before
    fun checkEveryInteraction() {
        composeRule.enableAccessibilityChecks(validator)
    }

    /** Fails with a description of every error that Google's checks find in what is on screen. */
    private fun assertNoAccessibilityErrors() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { validator.check(composeRule.activity.window.decorView) }
    }

    private fun pageWithScript(script: String): String =
        "data:text/html;charset=utf-8," + Uri.encode("<html><body><h1>Pay</h1><script>$script</script></body></html>")

    @Composable
    private fun AtLargestAccessibilitySizes(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * LARGE_DISPLAY_SCALE, fontScale = LARGE_FONT_SCALE),
            content = content,
        )
    }

    @Test
    fun failureViewPassesTheChecks() {
        composeRule.setContent { FintocWidgetHostTheme { FintocWidgetFailure(strings, onRetry = {}) } }

        assertNoAccessibilityErrors()
    }

    @Test
    fun failureViewPassesTheChecksAtTheLargestFontAndDisplaySizes() {
        composeRule.setContent {
            FintocWidgetHostTheme { AtLargestAccessibilitySizes { FintocWidgetFailure(strings, onRetry = {}) } }
        }

        assertNoAccessibilityErrors()
    }

    @Test
    fun loadingViewPassesTheChecksAndIsAnnouncedToScreenReaders() {
        composeRule.setContent { FintocWidgetHostTheme { FintocWidgetLoading(strings) } }

        assertNoAccessibilityErrors()
        composeRule.onNodeWithContentDescription(strings.get(R.string.fintoc_widget_loading)).assertIsDisplayed()
    }

    @Test
    fun hostScreenPassesTheChecks() {
        composeRule.setContent {
            FintocWidgetHostTheme {
                FintocWidgetHostScreen(hasSucceeded = false, language = FintocLanguage.ENGLISH, onLeave = {}) { modifier ->
                    FintocWidgetWebView(url = pageWithScript(""), onEvent = {}, externalLinkLauncher = {}, modifier = modifier)
                }
            }
        }

        assertNoAccessibilityErrors()
    }

    @Test
    fun hostScreenPassesTheChecksAtTheLargestFontAndDisplaySizes() {
        composeRule.setContent {
            FintocWidgetHostTheme {
                AtLargestAccessibilitySizes {
                    FintocWidgetHostScreen(hasSucceeded = true, language = FintocLanguage.ENGLISH, onLeave = {}) { modifier ->
                        FintocWidgetWebView(url = pageWithScript(""), onEvent = {}, externalLinkLauncher = {}, modifier = modifier)
                    }
                }
            }
        }

        assertNoAccessibilityErrors()
    }

    @Test
    fun theRetryButtonIsClickableAndBigEnoughToTap() {
        composeRule.setContent { FintocWidgetHostTheme { FintocWidgetFailure(strings, onRetry = {}) } }

        val retryButton = composeRule.onNodeWithText(strings.get(R.string.fintoc_widget_error_retry))
        retryButton.assertHasClickAction()

        val touchBounds = retryButton.fetchSemanticsNode().touchBoundsInRoot
        val minimumTouchTargetPixels = MINIMUM_TOUCH_TARGET_DP * targetContext.resources.displayMetrics.density
        assertTrue("Touch height ${touchBounds.height}px", touchBounds.height >= minimumTouchTargetPixels - 1)
        assertTrue("Touch width ${touchBounds.width}px", touchBounds.width >= minimumTouchTargetPixels - 1)
    }

    @Test
    fun theRetryButtonStaysReachableOnASmallScreenAtTheLargestFont() {
        composeRule.setContent {
            FintocWidgetHostTheme {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale = EXTREME_FONT_SCALE)) {
                    Box(Modifier.size(width = 240.dp, height = 220.dp)) { FintocWidgetFailure(strings, onRetry = {}) }
                }
            }
        }

        composeRule.onNodeWithText(strings.get(R.string.fintoc_widget_error_retry)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theFailureMessageIsAnnouncedWhenItAppears() {
        composeRule.setContent { FintocWidgetHostTheme { FintocWidgetFailure(strings, onRetry = {}) } }

        composeRule.onNodeWithText(strings.get(R.string.fintoc_widget_error_message))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
    }

    private companion object {
        const val MINIMUM_TOUCH_TARGET_DP = 48
        const val LARGE_FONT_SCALE = 2f
        const val EXTREME_FONT_SCALE = 3f
        const val LARGE_DISPLAY_SCALE = 1.5f
    }
}
