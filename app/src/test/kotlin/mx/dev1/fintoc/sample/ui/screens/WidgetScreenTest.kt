package mx.dev1.fintoc.sample.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.log.SampleEventLog
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resourceId)

    private var wentBack = false

    private fun show(log: SampleEventLog) {
        composeRule.setContent {
            FintocSampleTheme {
                WidgetScreen(title = "A demo", log = log, onBack = { wentBack = true }) { widgetModifier ->
                    Text(text = "the widget", modifier = widgetModifier)
                }
            }
        }
    }

    @Test
    fun `the widget it is given is shown under the title`() {
        show(SampleEventLog())

        composeRule.onNodeWithText("A demo").assertIsDisplayed()
        composeRule.onNodeWithText("the widget").assertIsDisplayed()
    }

    @Test
    fun `without events it says so`() {
        show(SampleEventLog())

        composeRule.onNodeWithText(string(R.string.log_heading)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.log_empty)).assertIsDisplayed()
    }

    @Test
    fun `the events the widget reports are listed`() {
        val log = SampleEventLog(clock = { 0L })
        log.record(FintocWidgetEvent.Occurred("opened"))
        log.record(FintocWidgetEvent.Exited)

        show(log)

        composeRule.onNodeWithText(string(R.string.log_empty)).assertDoesNotExist()
        composeRule.onNodeWithText("Event: opened", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Exited", substring = true).assertIsDisplayed()
    }

    @Test
    fun `back goes back`() {
        show(SampleEventLog())

        composeRule.onNodeWithText(string(R.string.sample_back)).performClick()

        assertTrue(wentBack)
    }
}
