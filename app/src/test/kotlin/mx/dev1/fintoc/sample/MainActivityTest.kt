package mx.dev1.fintoc.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.R
import mx.dev1.fintoc.sdk.Fintoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(resourceId: Int): String = context.getString(resourceId)

    private fun returnLink(path: String = "success", secret: String = "forged") =
        Intent(Intent.ACTION_VIEW, Uri.parse("fintocsample://checkout/$path?n=$secret"))
            .setClass(context, MainActivity::class.java)

    @Test
    fun `an ordinary launch shows the home screen`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
        }
    }

    @Test
    fun `a return link that opens the app shows the hosted checkout, and a forged one is not acted on`() {
        ActivityScenario.launch<MainActivity>(returnLink()).use {
            composeRule.onNodeWithText(string(R.string.hosted_outcome_unrelated)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `a return link that reaches the open activity is read too`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()

        controller.newIntent(returnLink(path = "cancel"))

        composeRule.onNodeWithText(string(R.string.hosted_outcome_unrelated)).performScrollTo().assertIsDisplayed()
        controller.pause().stop().destroy()
    }

    @Test
    fun `an intent that is not a view of an address is not mistaken for a return`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()

        controller.newIntent(Intent(Intent.ACTION_MAIN))

        composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
        controller.pause().stop().destroy()
    }

    @Test
    fun `picking a language initializes the sdk again`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText(string(R.string.language_spanish)).performScrollTo().performClick()

            assertTrue(Fintoc.isInitialized)
        }
    }

    @Test
    fun `the manifest sends the return links of the checkout to this activity`() {
        val returnLink = Intent(Intent.ACTION_VIEW, Uri.parse("fintocsample://checkout/success?n=x"))
            .addCategory(Intent.CATEGORY_BROWSABLE)

        val resolved = context.packageManager.queryIntentActivities(returnLink, 0)

        assertTrue(resolved.any { it.activityInfo.name == MainActivity::class.java.name })
    }

    @Test
    fun `the activity is single task and handles configuration changes itself`() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, info.launchMode)
        assertTrue(info.configChanges and ActivityInfo.CONFIG_ORIENTATION != 0)
        assertTrue(info.configChanges and ActivityInfo.CONFIG_LOCALE != 0)
    }
}
