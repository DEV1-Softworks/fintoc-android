package mx.dev1.fintoc.sample.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.checkout.HostedCheckoutDemo
import mx.dev1.fintoc.sample.checkout.HostedOpenResult
import mx.dev1.fintoc.sample.config.SampleSdk
import mx.dev1.fintoc.sample.config.SampleSettings
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.domain.checkout.FintocHostedCheckoutOutcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedCheckoutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val demo = HostedCheckoutDemo(secretValue = "abc123")
    private val opened = mutableListOf<String>()
    private var openResult = HostedOpenResult.OPENED
    private var newSecretRequested = false
    private var wentBack = false

    @Before
    fun initializeSdk() {
        SampleSdk.initialize(context, SampleSettings("pk_test_abc123"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    private fun string(resourceId: Int, vararg arguments: Any): String = context.getString(resourceId, *arguments)

    private fun show(outcome: FintocHostedCheckoutOutcome? = null) {
        composeRule.setContent {
            FintocSampleTheme {
                HostedCheckoutScreen(
                    demo = demo,
                    outcome = outcome,
                    onOpenCheckout = { redirectUrl ->
                        opened += redirectUrl
                        openResult
                    },
                    onNewSecretValue = { newSecretRequested = true },
                    onBack = { wentBack = true },
                )
            }
        }
    }

    private fun typeRedirectUrl(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
    }

    private fun clipboardText(): String? =
        context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()

    @Test
    fun `both return addresses are shown to give to fintoc`() {
        show()

        composeRule.onNodeWithText(demo.successUrl).assertIsDisplayed()
        composeRule.onNodeWithText(demo.cancelUrl).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hosted_explanation)).assertIsDisplayed()
    }

    @Test
    fun `an address can be copied`() {
        show()

        composeRule.onNodeWithContentDescription(string(R.string.hosted_copy_description, "success_url")).performClick()
        composeRule.waitForIdle()
        assertEquals(demo.successUrl, clipboardText())

        composeRule.onNodeWithContentDescription(string(R.string.hosted_copy_description, "cancel_url"))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(demo.cancelUrl, clipboardText())
    }

    @Test
    fun `a new secret value can be asked for`() {
        show()

        composeRule.onNodeWithText(string(R.string.hosted_new_secret)).performScrollTo().performClick()

        assertTrue(newSecretRequested)
    }

    @Test
    fun `the checkout cannot be opened without an address`() {
        show()

        composeRule.onNodeWithText(string(R.string.hosted_open)).performScrollTo().assertIsNotEnabled()

        typeRedirectUrl("https://pay.fintoc.com/checkout/cs_1")

        composeRule.onNodeWithText(string(R.string.hosted_open)).assertIsEnabled()
    }

    @Test
    fun `the typed address is what is opened`() {
        show()
        typeRedirectUrl("https://pay.fintoc.com/checkout/cs_1")

        composeRule.onNodeWithText(string(R.string.hosted_open)).performScrollTo().performClick()

        assertEquals(listOf("https://pay.fintoc.com/checkout/cs_1"), opened)
    }

    @Test
    fun `an address the sdk refuses is explained`() {
        openResult = HostedOpenResult.NOT_TRUSTED
        show()
        typeRedirectUrl("https://evil.example/")

        composeRule.onNodeWithText(string(R.string.hosted_open)).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.hosted_error_untrusted)).assertIsDisplayed()
    }

    @Test
    fun `a device with no browser is explained`() {
        openResult = HostedOpenResult.NO_BROWSER
        show()
        typeRedirectUrl("https://pay.fintoc.com/checkout/cs_1")

        composeRule.onNodeWithText(string(R.string.hosted_open)).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.hosted_error_no_browser)).assertIsDisplayed()
    }

    @Test
    fun `the explanation of a failure goes away when the address is edited`() {
        openResult = HostedOpenResult.NOT_TRUSTED
        show()
        typeRedirectUrl("https://evil.example/")
        composeRule.onNodeWithText(string(R.string.hosted_open)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.hosted_error_untrusted)).assertIsDisplayed()

        typeRedirectUrl("x")

        composeRule.onNodeWithText(string(R.string.hosted_error_untrusted)).assertDoesNotExist()
    }

    private fun assertOutcomeShown(outcome: FintocHostedCheckoutOutcome?, message: Int) {
        show(outcome)

        composeRule.onNodeWithText(string(message)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `before anything comes back it says so`() = assertOutcomeShown(null, R.string.hosted_outcome_none)

    @Test
    fun `a success is shown`() =
        assertOutcomeShown(FintocHostedCheckoutOutcome.Succeeded, R.string.hosted_outcome_succeeded)

    @Test
    fun `a cancellation is shown`() =
        assertOutcomeShown(FintocHostedCheckoutOutcome.Cancelled, R.string.hosted_outcome_cancelled)

    @Test
    fun `an address that is not one of ours is shown as such`() =
        assertOutcomeShown(FintocHostedCheckoutOutcome.Unrelated, R.string.hosted_outcome_unrelated)

    @Test
    fun `the reminder that a return is only a hint is always shown`() =
        assertOutcomeShown(null, R.string.hosted_reminder)

    @Test
    fun `back goes back`() {
        show()

        composeRule.onNodeWithText(string(R.string.sample_back)).performClick()

        assertTrue(wentBack)
    }
}
