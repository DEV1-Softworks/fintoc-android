package mx.dev1.fintoc.sample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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

    @Test
    fun appPackageMatchesTheApplicationId() {
        assertEquals("mx.dev1.fintoc", composeRule.activity.packageName)
    }

    @Test
    fun sampleScreenReportsAnInitializedSdk() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sample_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.sample_status_initialized))
            .assertIsDisplayed()
    }
}
