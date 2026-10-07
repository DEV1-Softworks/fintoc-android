package mx.dev1.fintoc.sdk.presentation.widget

import android.content.Context
import android.net.Uri
import android.util.AndroidRuntimeException
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import mx.dev1.fintoc.sdk.Fintoc
import mx.dev1.fintoc.sdk.FintocConfiguration
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.domain.widget.FintocHolderType
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FintocWidgetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun initializeSdk() {
        Fintoc.initialize(applicationContext, FintocConfiguration(publicKey = "pk_test_abc123"))
    }

    @After
    fun shutDownSdk() {
        Fintoc.shutdown()
    }

    private fun string(resourceId: Int): String = applicationContext.getString(resourceId)

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
    }

    private fun loadedUrl(): String? = findWebView(composeRule.activity.window.decorView)
        ?.let { webView -> shadowOf(webView).lastLoadedUrl }

    @Test
    fun `options overload loads the widget with the configured public key`() {
        composeRule.setContent {
            FintocWidget(options = FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456"), onEvent = {})
        }

        composeRule.runOnIdle {
            assertEquals(
                "https://webview.fintoc.com/widget.html" +
                    "?public_key=pk_test_abc123&product=payments&session_token=cs_123_sec_456&_on_event=true",
                loadedUrl(),
            )
        }
    }

    @Test
    fun `options overload serves every product`() {
        composeRule.setContent {
            FintocWidget(
                options = FintocWidgetOptions.Movements(holderType = FintocHolderType.INDIVIDUAL),
                onEvent = {},
            )
        }

        composeRule.runOnIdle {
            assertTrue(loadedUrl().orEmpty().contains("product=movements&holder_type=individual"))
        }
    }

    @Test
    fun `a device without a webview shows a message and never crashes the host`() {
        val deviceWithoutWebView = module {
            factory<FintocWebViewFactory> { FintocWebViewFactory { throw AndroidRuntimeException("No WebView.") } }
        }
        Fintoc.requireKoin().loadModules(listOf(deviceWithoutWebView), allowOverride = true)

        composeRule.setContent {
            FintocWidget(options = FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456"), onEvent = {})
        }

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertIsDisplayed()
        composeRule.runOnIdle { assertNull(loadedUrl()) }
    }

    @Test
    fun `using the widget before initializing the sdk fails with a clear message`() {
        Fintoc.shutdown()

        val failure = assertThrows(IllegalStateException::class.java) {
            composeRule.setContent {
                FintocWidget(options = FintocWidgetOptions.Payments(sessionToken = "cs_token"), onEvent = {})
            }
        }

        assertTrue(failure.message.orEmpty().contains("Fintoc.initialize"))
    }

    @Test
    fun `provider overload waits for the token and then loads the widget with it`() {
        val token = CompletableDeferred<String>()
        composeRule.setContent { FintocWidget(sessionTokenProvider = { token.await() }, onEvent = {}) }

        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertIsDisplayed()
        composeRule.runOnIdle { assertNull(loadedUrl()) }

        token.complete("cs_from_backend")
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(loadedUrl().orEmpty().contains("session_token=cs_from_backend"))
        }
    }

    @Test
    fun `provider is asked once even when the screen recomposes`() {
        var calls = 0
        var recompositionTrigger by mutableIntStateOf(0)
        composeRule.setContent {
            val trigger = recompositionTrigger
            FintocWidget(sessionTokenProvider = { calls += 1; "cs_token" }, onEvent = { trigger.hashCode() })
        }
        composeRule.waitForIdle()

        recompositionTrigger += 1
        composeRule.waitForIdle()
        recompositionTrigger += 1
        composeRule.waitForIdle()

        assertEquals(1, calls)
    }

    @Test
    fun `a failing provider shows the failure message and trying again asks for a new token`() {
        var calls = 0
        composeRule.setContent {
            FintocWidget(
                sessionTokenProvider = {
                    calls += 1
                    if (calls == 1) throw IOException("backend down") else "cs_second_attempt"
                },
                onEvent = {},
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_message)).assertIsDisplayed()
        composeRule.runOnIdle { assertNull(loadedUrl()) }

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).performClick()
        composeRule.waitForIdle()

        assertEquals(2, calls)
        composeRule.runOnIdle {
            assertTrue(loadedUrl().orEmpty().contains("session_token=cs_second_attempt"))
        }
    }

    @Test
    fun `an unusable token is a failure and never reaches the widget`() {
        composeRule.setContent { FintocWidget(sessionTokenProvider = { "sk_live_secret" }, onEvent = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_message)).assertIsDisplayed()
        composeRule.runOnIdle { assertNull(loadedUrl()) }
    }

    @Test
    fun `events from a widget started through the provider reach the app`() {
        val events = mutableListOf<FintocWidgetEvent>()
        composeRule.setContent {
            FintocWidget(sessionTokenProvider = { "cs_token" }, onEvent = { event -> events += event })
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val webView = checkNotNull(findWebView(composeRule.activity.window.decorView))
            val request = mock<WebResourceRequest> {
                on { url } doReturn Uri.parse("fintocwidget://succeeded")
                on { isForMainFrame } doReturn true
            }
            webView.webViewClient.shouldOverrideUrlLoading(webView, request)
        }

        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Succeeded()), events)
    }
}
