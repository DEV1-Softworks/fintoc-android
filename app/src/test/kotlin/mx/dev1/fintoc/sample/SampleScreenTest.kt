package mx.dev1.fintoc.sample

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sample.ui.SampleScreen
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SampleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resourceId)

    @Test
    fun `initialized sdk shows the ready message`() {
        composeRule.setContent { FintocSampleTheme { SampleScreen(isSdkInitialized = true) } }

        composeRule.onNodeWithText(string(R.string.sample_status_initialized)).assertIsDisplayed()
    }

    @Test
    fun `missing initialization shows the warning message`() {
        composeRule.setContent { FintocSampleTheme { SampleScreen(isSdkInitialized = false) } }

        composeRule.onNodeWithText(string(R.string.sample_status_not_initialized)).assertIsDisplayed()
    }

    @Test
    fun `title is exposed to accessibility services as a heading`() {
        composeRule.setContent { FintocSampleTheme { SampleScreen(isSdkInitialized = true) } }

        val titleNode = composeRule.onNodeWithText(string(R.string.sample_title)).fetchSemanticsNode()

        assertTrue(titleNode.config.contains(SemanticsProperties.Heading))
    }

    @Test
    fun `dark theme renders the same content`() {
        composeRule.setContent { FintocSampleTheme(isDarkTheme = true) { SampleScreen(isSdkInitialized = true) } }

        composeRule.onNodeWithText(string(R.string.sample_title)).assertIsDisplayed()
    }
}
