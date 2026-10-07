package mx.dev1.fintoc.sdk.presentation.host

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.FintocLanguage
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FintocWidgetActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contract = FintocWidgetContract()
    private val options = FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456")

    @Before
    fun initializeSdk() {
        initializeSdk(language = null)
    }

    @After
    fun tearDown() {
        Fintoc.shutdown()
    }

    private fun pendingRequests(): FintocWidgetRequests = Fintoc.requireKoin().get()

    private fun initializeSdk(language: FintocLanguage?) {
        Fintoc.initialize(context, FintocConfiguration(publicKey = "pk_test_abc123", language = language))
    }

    private fun launch(intent: Intent = contract.createIntent(context, options)): ActivityScenario<FintocWidgetActivity> =
        ActivityScenario.launch(intent)

    private fun launchForResult(intent: Intent): ActivityScenario<FintocWidgetActivity> =
        ActivityScenario.launchActivityForResult(intent)

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
    }

    private fun ActivityScenario<FintocWidgetActivity>.report(redirect: String) = onActivity { activity ->
        val webView = checkNotNull(findWebView(activity.window.decorView))
        val request = mock<WebResourceRequest> {
            on { url } doReturn Uri.parse(redirect)
            on { isForMainFrame } doReturn true
        }
        webView.webViewClient.shouldOverrideUrlLoading(webView, request)
    }

    @Test
    fun `an intent without a request closes the screen as cancelled`() {
        launchForResult(Intent(context, FintocWidgetActivity::class.java)).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun `a request the process no longer remembers closes the screen as cancelled`() {
        val intent = Intent(context, FintocWidgetActivity::class.java)
            .putExtra(FintocWidgetContract.EXTRA_REQUEST_ID, "request-from-a-dead-process")

        launchForResult(intent).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun `an intent prepared before the sdk was shut down no longer opens the widget`() {
        val staleIntent = contract.createIntent(context, options)

        Fintoc.shutdown()
        initializeSdk(language = null)

        launchForResult(staleIntent).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun `a screen restored where the host has not initialized the sdk closes instead of crashing`() {
        val restoredIntent = contract.createIntent(context, options)

        Fintoc.shutdown()

        launchForResult(restoredIntent).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun `the screen loads the widget with the configured key and the requested options`() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                val url = shadowOf(checkNotNull(findWebView(activity.window.decorView))).lastLoadedUrl
                assertTrue(url.orEmpty().contains("public_key=pk_test_abc123"))
                assertTrue(url.orEmpty().contains("session_token=cs_123_sec_456"))
            }
        }
    }

    @Test
    fun `the screen hides itself from screenshots and the recent apps list`() {
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }

    @Test
    fun `the screen is private to the app`() {
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, FintocWidgetActivity::class.java),
            0,
        )

        assertFalse(activityInfo.exported)
    }

    @Test
    fun `the screen offers to close until the flow succeeds`() {
        launch().use {
            composeRule.onNodeWithText("Close").assertIsDisplayed()
            composeRule.onNodeWithText("Done").assertDoesNotExist()
        }
    }

    @Test
    fun `leaving before the flow succeeds is reported as an exit`() {
        launch().use { scenario ->
            composeRule.onNodeWithText("Close").performClick()

            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
                assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
            }
        }
    }

    @Test
    fun `an exit reported by the widget closes the screen as cancelled`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://exit")

            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
                assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
            }
        }
    }

    @Test
    fun `a success keeps the screen open and turns the button into done`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://succeeded")
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Done").assertIsDisplayed()
            composeRule.onNodeWithText("Close").assertDoesNotExist()
            scenario.onActivity { activity ->
                assertFalse("The user may still need to download the voucher", activity.isFinishing)
                assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
            }
        }
    }

    @Test
    fun `tapping done after a success finishes the screen and reports it`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://succeeded")
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Done").performClick()

            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
                assertEquals(
                    FintocWidgetResult.Succeeded(linkIntent = null),
                    contract.parseResult(shadowOf(activity).resultCode, shadowOf(activity).resultIntent),
                )
            }
        }
    }

    @Test
    fun `a link intent reaches the app in the result`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://succeeded?object=link_intent&exchange_token=et_123&id=li_456")

            scenario.onActivity { activity ->
                val result = contract.parseResult(shadowOf(activity).resultCode, shadowOf(activity).resultIntent)
                assertEquals("et_123", (result as FintocWidgetResult.Succeeded).linkIntent?.exchangeToken)
                assertEquals("li_456", result.linkIntent?.id)
            }
        }
    }

    @Test
    fun `an exit after a success does not turn it into a cancellation`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://succeeded")
            scenario.report("fintocwidget://exit")

            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
                assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
            }
        }
    }

    @Test
    fun `progress events do not close the screen`() {
        launch().use { scenario ->
            scenario.report("fintocwidget://event/opened?timestamp=1")
            scenario.report("fintocwidget://event/payment_error?timestamp=2")

            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).resultCode)
            }
        }
    }

    @Test
    fun `the request is forgotten when the screen finishes and kept while it lives`() {
        val intent = contract.createIntent(context, options)
        val requestId = intent.getStringExtra(FintocWidgetContract.EXTRA_REQUEST_ID).orEmpty()

        launch(intent).use { scenario ->
            assertNotNull(pendingRequests().find(requestId))

            composeRule.onNodeWithText("Close").performClick()
            scenario.moveToState(Lifecycle.State.DESTROYED)

            assertNull(pendingRequests().find(requestId))
        }
    }

    @Test
    fun `the texts of the screen follow the configured language`() {
        initializeSdk(language = FintocLanguage.SPANISH)

        launch().use {
            composeRule.onNodeWithText("Cerrar").assertIsDisplayed()
        }
    }
}
