package mx.dev1.fintoc.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.dev1.fintoc.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(resourceId: Int): String = composeRule.activity.getString(resourceId)

    @Test
    fun appPackageMatchesTheApplicationId() {
        assertEquals("mx.dev1.fintoc", composeRule.activity.packageName)
    }

    @Test
    fun homeScreenShowsTheTitleAndEveryDemo() {
        composeRule.onNodeWithText(string(R.string.sample_title)).assertIsDisplayed()
        listOf(
            R.string.demo_movements_title,
            R.string.demo_payments_options_title,
            R.string.demo_payments_provider_title,
            R.string.demo_payments_activity_title,
            R.string.demo_hosted_title,
        ).forEach { demo -> composeRule.onNodeWithText(string(demo)).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun theBankConnectionDemoOpensTheRealWidgetScreenAndBackReturnsHome() {
        composeRule.onNodeWithText(string(R.string.demo_movements_title)).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.log_heading)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.sample_back)).performClick()
        composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
    }
}
