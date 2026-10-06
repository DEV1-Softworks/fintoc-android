package mx.dev1.fintoc.sample

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import mx.dev1.fintoc.R
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The hosted checkout's way back into the app, through real Android intents: the intent filter in the manifest, the
 * `singleTask` launch mode and the SDK's matcher, with the address that the screen tells you to give to Fintoc.
 *
 * The Activity is started directly instead of through ActivityScenario, whose own bookkeeping loses track of an
 * Activity that receives `onNewIntent` and then reports that it never reached DESTROYED, although it did.
 */
@RunWith(AndroidJUnit4::class)
class HostedCheckoutReturnInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var activity: MainActivity

    private fun string(resourceId: Int): String = context.getString(resourceId)

    @Before
    fun openTheHostedCheckoutDemo() {
        val launch = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity = instrumentation.startActivitySync(launch) as MainActivity
        composeRule.onNodeWithText(string(R.string.demo_hosted_title)).performScrollTo().performClick()
    }

    @After
    fun closeTheActivity() {
        instrumentation.runOnMainSync { activity.finish() }
    }

    private fun addressOnScreen(prefix: String): String =
        composeRule.onNode(hasText(prefix, substring = true)).fetchSemanticsNode()
            .config[SemanticsProperties.Text].first().text

    /** Opens an address the way a browser does when Fintoc sends the customer back. */
    private fun comeBackThrough(address: String) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address)).setPackage(context.packageName))
    }

    /** The Activities of the app that exist right now, which is how a second copy of the screen would show. */
    private fun liveMainActivities(): Int {
        var count = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            count = Stage.entries
                .filter { stage -> stage != Stage.DESTROYED && stage != Stage.PRE_ON_CREATE }
                .flatMap { stage -> ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage) }
                .filterIsInstance<MainActivity>()
                .distinct()
                .size
        }
        return count
    }

    private fun assertOutcome(message: Int) {
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodes(hasText(string(message))).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(string(message)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSuccessAddressComesBackToTheOpenActivityAndIsRecognized() {
        comeBackThrough(addressOnScreen("fintocsample://checkout/success?n="))

        assertOutcome(R.string.hosted_outcome_succeeded)
    }

    @Test
    fun theCancelAddressComesBackAndIsRecognized() {
        comeBackThrough(addressOnScreen("fintocsample://checkout/cancel?n="))

        assertOutcome(R.string.hosted_outcome_cancelled)
    }

    @Test
    fun anAddressWithoutTheSecretValueIsNotActedOn() {
        comeBackThrough("fintocsample://checkout/success?n=forged")

        assertOutcome(R.string.hosted_outcome_unrelated)
    }

    @Test
    fun theReturnReachesTheSameActivityInsteadOfOpeningASecondOne() {
        assertEquals(1, liveMainActivities())

        comeBackThrough(addressOnScreen("fintocsample://checkout/success?n="))
        assertOutcome(R.string.hosted_outcome_succeeded)

        assertEquals("singleTask must reuse the open Activity", 1, liveMainActivities())
    }
}
