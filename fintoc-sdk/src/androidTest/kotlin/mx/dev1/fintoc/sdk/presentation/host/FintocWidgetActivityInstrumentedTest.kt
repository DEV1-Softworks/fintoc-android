package mx.dev1.fintoc.sdk.presentation.host

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Starts the real screen on a device. Only what Android itself decides is checked here, so the tests give the same
 * answer whether or not the page of the Widget manages to load.
 */
@RunWith(AndroidJUnit4::class)
class FintocWidgetActivityInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val contract = FintocWidgetContract()
    private val options = FintocWidgetOptions.Payments(sessionToken = "cs_instrumented_test_token")

    @Before
    fun initializeSdk() {
        Fintoc.initialize(context, FintocConfiguration(publicKey = "pk_test_instrumented_tests"))
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
        FintocWidgetRequests.clear()
    }

    @Test
    fun theWindowHidesItselfFromScreenshotsAndTheRecentAppsList() {
        ActivityScenario.launch<FintocWidgetActivity>(contract.createIntent(context, options)).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }

    @Test
    fun aRotationDoesNotRecreateTheScreenSoAPaymentInProgressSurvives() {
        ActivityScenario.launch<FintocWidgetActivity>(contract.createIntent(context, options)).use { scenario ->
            var before: Activity? = null
            scenario.onActivity { activity ->
                before = activity
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) { orientationOf(scenario) == Configuration.ORIENTATION_LANDSCAPE }

            scenario.onActivity { activity ->
                assertSame("The screen must handle the rotation itself", before, activity)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    @Test
    fun closingBeforeTheFlowEndsReportsACancellation() {
        ActivityScenario.launchActivityForResult<FintocWidgetActivity>(contract.createIntent(context, options))
            .use { scenario ->
                composeRule.onNodeWithText(context.getString(R.string.fintoc_widget_close)).performClick()

                composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) { scenario.state.name == "DESTROYED" }
                assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            }
    }

    private fun orientationOf(scenario: ActivityScenario<FintocWidgetActivity>): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED
        scenario.onActivity { activity -> orientation = activity.resources.configuration.orientation }
        return orientation
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
