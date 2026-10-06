package mx.dev1.fintoc.sample.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.domain.widget.FintocLinkIntentResult
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resourceId)

    private val starts = mutableListOf<Pair<String, Boolean>>()
    private var wentBack = false

    private fun show(offersSimulatedFailure: Boolean = false, lastResult: FintocWidgetResult? = null) {
        composeRule.setContent {
            FintocSampleTheme {
                TokenScreen(
                    title = "Payments",
                    offersSimulatedFailure = offersSimulatedFailure,
                    lastResult = lastResult,
                    onStart = { token, simulateFailure -> starts += token to simulateFailure },
                    onBack = { wentBack = true },
                )
            }
        }
    }

    private fun enter(token: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(token)
    }

    private fun start() {
        composeRule.onNodeWithText(string(R.string.token_start)).performClick()
    }

    @Test
    fun `it explains where the token comes from and who must never see the secret key`() {
        show()

        composeRule.onNodeWithText(string(R.string.token_explanation)).assertIsDisplayed()
    }

    @Test
    fun `a valid token is started without the spaces around it`() {
        show()

        enter("  cs_123_sec_456 ")
        start()

        assertEquals(listOf("cs_123_sec_456" to false), starts)
    }

    private fun assertRefused(token: String) {
        show()
        if (token.isNotEmpty()) enter(token)

        start()

        composeRule.onNodeWithText(string(R.string.token_invalid)).assertIsDisplayed()
        assertTrue(starts.isEmpty())
    }

    @Test
    fun `nothing starts without a token`() = assertRefused("")

    @Test
    fun `a token with spaces inside is refused with a message`() = assertRefused("cs_123 sec_456")

    @Test
    fun `a secret key is refused with a message`() = assertRefused("sk_live_secret")

    @Test
    fun `the message goes away as soon as the text changes`() {
        show()
        start()
        composeRule.onNodeWithText(string(R.string.token_invalid)).assertIsDisplayed()

        enter("c")

        composeRule.onNodeWithText(string(R.string.token_invalid)).assertDoesNotExist()
    }

    @Test
    fun `the simulated failure switch only appears when it is offered`() {
        show(offersSimulatedFailure = false)

        composeRule.onNodeWithText(string(R.string.token_fail_first)).assertDoesNotExist()
    }

    @Test
    fun `the simulated failure choice is passed along`() {
        show(offersSimulatedFailure = true)

        composeRule.onNodeWithText(string(R.string.token_fail_first)).assertIsDisplayed()
        composeRule.onNode(isToggleable()).performClick()
        enter("cs_1")
        start()

        assertEquals(listOf("cs_1" to true), starts)
    }

    @Test
    fun `the last result of a screen of its own is shown`() {
        show(lastResult = FintocWidgetResult.Succeeded(FintocLinkIntentResult("et_1")))
        composeRule.onNodeWithText(string(R.string.token_result_succeeded)).assertIsDisplayed()
    }

    @Test
    fun `an exit is shown as one`() {
        show(lastResult = FintocWidgetResult.Exited)
        composeRule.onNodeWithText(string(R.string.token_result_exited)).assertIsDisplayed()
    }

    @Test
    fun `no result is shown before there is one`() {
        show(lastResult = null)

        composeRule.onNodeWithText(string(R.string.token_result_succeeded)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.token_result_exited)).assertDoesNotExist()
    }

    @Test
    fun `back goes back`() {
        show()

        composeRule.onNodeWithText(string(R.string.sample_back)).performClick()

        assertTrue(wentBack)
    }
}
