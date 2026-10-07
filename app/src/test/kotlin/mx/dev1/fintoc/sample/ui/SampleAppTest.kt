package mx.dev1.fintoc.sample.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.config.SampleSdk
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocLanguage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SampleAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val languages = mutableListOf<FintocLanguage?>()
    private var returnedUri by mutableStateOf<Uri?>(null)

    @Before
    fun initializeSdk() {
        SampleSdk.initialize(context, SampleSettings("pk_test_abc123"))
        composeRule.setContent {
            FintocSampleTheme {
                SampleApp(
                    settings = SampleSettings("pk_test_abc123"),
                    returnedUri = returnedUri,
                    onLanguageSelected = { language -> languages += language },
                )
            }
        }
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    private fun string(resourceId: Int): String = context.getString(resourceId)

    private fun open(demoTitle: Int) {
        composeRule.onNodeWithText(string(demoTitle)).performScrollTo().performClick()
    }

    private fun back() = composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun enterToken(token: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(token)
    }

    private fun start() {
        composeRule.onNodeWithText(string(R.string.token_start)).performClick()
    }

    private fun findWebView(): WebView? = findWebView(composeRule.activity.window.decorView)

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
    }

    private fun assertOnHome() {
        composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
    }

    private fun successUrlOnScreen(): String =
        composeRule.onNode(hasText("fintocsample://checkout/success?n=", substring = true))
            .fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    private fun cancelUrlOnScreen(): String =
        composeRule.onNode(hasText("fintocsample://checkout/cancel?n=", substring = true))
            .fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    @Test
    fun `it starts on the home screen`() {
        assertOnHome()
        composeRule.onNodeWithText(string(R.string.sample_title)).assertIsDisplayed()
    }

    @Test
    fun `the bank connection demo shows the widget with its events, and back returns home`() {
        open(R.string.demo_movements_title)

        composeRule.onNodeWithText(string(R.string.log_heading)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.log_empty)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.sample_back)).performClick()
        assertOnHome()
    }

    @Test
    fun `the system back button leaves a demo instead of the app`() {
        open(R.string.demo_movements_title)

        back()

        assertOnHome()
    }

    @Test
    fun `a payment starts from a pasted token and back steps out of the widget and then out of the demo`() {
        open(R.string.demo_payments_options_title)
        enterToken("cs_123_sec_456")

        start()
        composeRule.onNodeWithText(string(R.string.log_heading)).assertIsDisplayed()

        back()
        composeRule.onNodeWithText(string(R.string.token_start)).assertIsDisplayed()

        back()
        assertOnHome()
    }

    @Test
    fun `a token that cannot be one does not start the widget`() {
        open(R.string.demo_payments_options_title)
        enterToken("sk_live_secret")

        start()

        composeRule.onNodeWithText(string(R.string.token_invalid)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.log_heading)).assertDoesNotExist()
    }

    @Test
    fun `the token provider demo waits for the simulated backend and then shows the widget`() {
        open(R.string.demo_payments_provider_title)
        enterToken("cs_123_sec_456")
        composeRule.mainClock.autoAdvance = false

        start()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription("Loading Fintoc").assertIsDisplayed()

        assertNull("The widget must wait for the token", findWebView())

        composeRule.mainClock.advanceTimeBy(1_600L)
        assertNotNull("The widget loads once the backend answered", findWebView())
    }

    @Test
    fun `a failing backend shows the retry message of the sdk, and trying again gets the widget`() {
        open(R.string.demo_payments_provider_title)
        enterToken("cs_123_sec_456")
        composeRule.onNode(isToggleable()).performClick()
        composeRule.mainClock.autoAdvance = false

        start()
        composeRule.mainClock.advanceTimeBy(1_600L)
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        assertNull("The widget must not load without a token", findWebView())

        composeRule.onNodeWithText("Try again").performClick()
        composeRule.mainClock.advanceTimeBy(1_600L)
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
        assertNotNull("The second attempt gets the token, so the widget loads", findWebView())
    }

    @Test
    fun `the screen of its own opens for the payment and its result is shown when it ends`() {
        open(R.string.demo_payments_activity_title)
        enterToken("cs_123_sec_456")

        start()

        val request = shadowOf(composeRule.activity).nextStartedActivityForResult
        assertTrue(request.intent.component?.className.orEmpty().endsWith("FintocWidgetActivity"))

        shadowOf(composeRule.activity).receiveResult(request.intent, Activity.RESULT_OK, Intent())
        composeRule.onNodeWithText(string(R.string.token_result_succeeded)).assertIsDisplayed()
    }

    @Test
    fun `leaving the screen of its own is shown as an exit`() {
        open(R.string.demo_payments_activity_title)
        enterToken("cs_123_sec_456")
        start()

        val request = shadowOf(composeRule.activity).nextStartedActivityForResult
        shadowOf(composeRule.activity).receiveResult(request.intent, Activity.RESULT_CANCELED, null)

        composeRule.onNodeWithText(string(R.string.token_result_exited)).assertIsDisplayed()
    }

    @Test
    fun `the success address of the checkout on screen is recognized when it comes back`() {
        open(R.string.demo_hosted_title)
        composeRule.onNodeWithText(string(R.string.hosted_outcome_none)).performScrollTo().assertIsDisplayed()

        returnedUri = Uri.parse(successUrlOnScreen())

        composeRule.onNodeWithText(string(R.string.hosted_outcome_succeeded)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the cancel address of the checkout on screen is recognized when it comes back`() {
        open(R.string.demo_hosted_title)

        returnedUri = Uri.parse(cancelUrlOnScreen())

        composeRule.onNodeWithText(string(R.string.hosted_outcome_cancelled)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `an address that lacks the secret value is not acted on`() {
        open(R.string.demo_hosted_title)

        returnedUri = Uri.parse("fintocsample://checkout/success?n=guess")

        composeRule.onNodeWithText(string(R.string.hosted_outcome_unrelated)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a new secret value changes the addresses and forgets what had come back`() {
        open(R.string.demo_hosted_title)
        val before = successUrlOnScreen()
        returnedUri = Uri.parse(before)
        composeRule.onNodeWithText(string(R.string.hosted_outcome_succeeded)).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.hosted_new_secret)).performScrollTo().performClick()

        assertNotEquals(before, successUrlOnScreen())
        composeRule.onNodeWithText(string(R.string.hosted_outcome_none)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a return that still carries the old secret value is refused after a new one was made`() {
        open(R.string.demo_hosted_title)
        val old = successUrlOnScreen()
        composeRule.onNodeWithText(string(R.string.hosted_new_secret)).performScrollTo().performClick()

        returnedUri = Uri.parse(old)

        composeRule.onNodeWithText(string(R.string.hosted_outcome_unrelated)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a return that arrives while another screen is open takes the user to the checkout`() {
        assertOnHome()

        returnedUri = Uri.parse("fintocsample://checkout/success?n=anything")

        composeRule.onNodeWithText(string(R.string.hosted_reminder)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hosted_outcome_unrelated)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the language picked is reported and stays selected`() {
        composeRule.onNodeWithText(string(R.string.language_french)).performScrollTo().performClick()

        assertEquals(listOf<FintocLanguage?>(FintocLanguage.FRENCH), languages)
        composeRule.onNodeWithText(string(R.string.language_french)).performScrollTo().assertIsSelected()
    }
}
