package mx.dev1.fintoc.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.dev1.fintoc.R
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sample opens the SDK's own screen, which lives in a different Activity, so it is checked from outside the app.
 */
@RunWith(AndroidJUnit4::class)
class SdkScreenInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(resourceId: Int): String = composeRule.activity.getString(resourceId)

    private fun openTheSdkScreen() {
        composeRule.onNodeWithText(string(R.string.demo_payments_activity_title)).performScrollTo().performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("cs_instrumented_test_token")
        composeRule.onNodeWithText(string(R.string.token_start)).performClick()
        SampleDevice.waitUntil(description = "the SDK screen in the foreground") {
            SampleDevice.topActivity().endsWith("FintocWidgetActivity")
        }
        // Being the top Activity is not enough for a key to reach it: wait until the screen is drawn and readable.
        SampleDevice.waitUntil(description = "the SDK screen drawn") { SampleDevice.screenShows("Fintoc") }
    }

    @Test
    fun theLanguageChosenInTheSampleIsTheOneOfTheSdkScreen() {
        composeRule.onNodeWithText(string(R.string.language_french)).performScrollTo().performClick()

        openTheSdkScreen()

        SampleDevice.waitUntil(description = "the French button of the SDK screen") {
            SampleDevice.screenShows("Fermer")
        }
        SampleDevice.shell("input keyevent KEYCODE_BACK")
    }

    @Test
    fun goingBackFromTheSdkScreenIsShownAsAnExit() {
        openTheSdkScreen()

        SampleDevice.shell("input keyevent KEYCODE_BACK")

        SampleDevice.waitUntil(description = "the sample back in the foreground") {
            SampleDevice.topActivity().endsWith("MainActivity")
        }
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText(string(R.string.token_result_exited)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(string(R.string.token_result_exited)).assertIsDisplayed()
    }

    @Test
    fun theSdkScreenDoesNotShowTheSessionTokenThatWasPasted() {
        openTheSdkScreen()
        // First prove that the screen can be read at all, or the check below would pass for the wrong reason.
        SampleDevice.waitUntil(description = "the title of the SDK screen") { SampleDevice.screenShows("Fintoc") }

        assertFalse(SampleDevice.screenShows("cs_instrumented_test_token"))
        SampleDevice.shell("input keyevent KEYCODE_BACK")
    }
}
