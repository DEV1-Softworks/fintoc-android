package mx.dev1.fintoc.sdk.presentation.widget

import android.content.Context
import android.net.Uri
import android.util.AndroidRuntimeException
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FintocWidgetWebViewComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<FintocWidgetEvent>()
    private val openedLinks = mutableListOf<String>()

    private fun string(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resourceId)

    private fun findWebView(): WebView? = findWebView(composeRule.activity.window.decorView)

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
    }

    private fun webView(): WebView = checkNotNull(findWebView()) { "No WebView in the composition." }

    private fun simulatePageFinished() = composeRule.runOnIdle {
        webView().let { view -> view.webViewClient.onPageFinished(view, view.url) }
    }

    private fun simulateMainFrameFailure() = composeRule.runOnIdle {
        val view = webView()
        view.webViewClient.onReceivedError(view, request(WIDGET_URL), mock())
    }

    private fun request(url: String): WebResourceRequest = mock {
        on { this.url } doReturn Uri.parse(url)
        on { isForMainFrame } doReturn true
    }

    private fun showWidget(url: String = WIDGET_URL) {
        composeRule.setContent {
            FintocWidgetWebView(
                url = url,
                onEvent = { event -> events += event },
                externalLinkLauncher = { link -> openedLinks += link },
            )
        }
    }

    @Test
    fun `the page is requested from the given address in a hardened webview`() {
        showWidget()

        composeRule.runOnIdle {
            assertEquals(WIDGET_URL, shadowOf(webView()).lastLoadedUrl)
            assertTrue(webView().settings.javaScriptEnabled)
            assertEquals(false, webView().settings.allowFileAccess)
        }
    }

    @Test
    fun `a progress indicator covers the page until it finishes loading`() {
        showWidget()

        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertIsDisplayed()

        simulatePageFinished()

        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertDoesNotExist()
        assertNotNull(findWebView())
    }

    @Test
    fun `events the page reports reach the app`() {
        showWidget()

        composeRule.runOnIdle {
            val view = webView()
            view.webViewClient.shouldOverrideUrlLoading(view, request("fintocwidget://exit"))
        }

        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Exited), events)
    }

    @Test
    fun `links that leave fintoc go to the launcher`() {
        showWidget()

        composeRule.runOnIdle {
            val view = webView()
            view.webViewClient.shouldOverrideUrlLoading(view, request("https://files.example.com/voucher.pdf"))
        }

        assertEquals(listOf("https://files.example.com/voucher.pdf"), openedLinks)
    }

    @Test
    fun `downloads go to the launcher when https and are dropped otherwise`() {
        showWidget()

        composeRule.runOnIdle {
            val listener = shadowOf(webView()).downloadListener
            listener.onDownloadStart("https://files.example.com/voucher.pdf", "", "", "application/pdf", 10L)
            listener.onDownloadStart("http://files.example.com/voucher.pdf", "", "", "application/pdf", 10L)
            listener.onDownloadStart("file:///data/secret", "", "", "text/plain", 10L)
        }

        assertEquals(listOf("https://files.example.com/voucher.pdf"), openedLinks)
    }

    @Test
    fun `a page that fails to load is replaced by a failure message and the webview is released`() {
        showWidget()
        val firstWebView = webView()

        simulateMainFrameFailure()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_message)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).assertIsDisplayed()
        composeRule.runOnIdle {
            assertNull(findWebView())
            assertTrue(shadowOf(firstWebView).wasDestroyCalled())
        }
    }

    @Test
    fun `trying again loads the page in a fresh webview`() {
        showWidget()
        val firstWebView = webView()
        simulateMainFrameFailure()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).performClick()

        composeRule.runOnIdle {
            assertNotSame(firstWebView, webView())
            assertEquals(WIDGET_URL, shadowOf(webView()).lastLoadedUrl)
        }
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_message)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertIsDisplayed()
    }

    @Test
    fun `a different address loads in a fresh webview`() {
        var url by mutableStateOf(WIDGET_URL)
        composeRule.setContent {
            FintocWidgetWebView(url = url, onEvent = {}, externalLinkLauncher = {})
        }
        val firstWebView = webView()

        url = "https://webview.fintoc.com/widget.html?product=movements"
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertNotSame(firstWebView, webView())
            assertEquals(url, shadowOf(webView()).lastLoadedUrl)
        }
    }

    @Test
    fun `a new callback is used without reloading the page`() {
        val firstEvents = mutableListOf<FintocWidgetEvent>()
        val secondEvents = mutableListOf<FintocWidgetEvent>()
        var currentCallback: (FintocWidgetEvent) -> Unit by mutableStateOf({ event -> firstEvents += event })
        composeRule.setContent {
            FintocWidgetWebView(url = WIDGET_URL, onEvent = currentCallback, externalLinkLauncher = {})
        }
        val firstWebView = webView()

        currentCallback = { event -> secondEvents += event }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            firstWebView.webViewClient.shouldOverrideUrlLoading(firstWebView, request("fintocwidget://exit"))
        }

        assertTrue(firstEvents.isEmpty())
        assertEquals(listOf<FintocWidgetEvent>(FintocWidgetEvent.Exited), secondEvents)
        assertEquals(firstWebView, webView())
    }

    private fun unavailableFactory(creationAttempts: MutableList<Context>? = null) = FintocWebViewFactory { context ->
        creationAttempts?.add(context)
        throw AndroidRuntimeException("No WebView provider is installed.")
    }

    private fun showWidgetWith(webViewFactory: FintocWebViewFactory) {
        composeRule.setContent {
            FintocWidgetWebView(
                url = WIDGET_URL,
                onEvent = { event -> events += event },
                externalLinkLauncher = { link -> openedLinks += link },
                webViewFactory = webViewFactory,
            )
        }
    }

    @Test
    fun `a device that cannot create a webview shows what is missing instead of crashing`() {
        showWidgetWith(unavailableFactory())

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_message)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertDoesNotExist()
        composeRule.runOnIdle { assertNull(findWebView()) }
    }

    @Test
    fun `trying again asks the device for a webview again and still explains when there is none`() {
        val creationAttempts = mutableListOf<Context>()
        showWidgetWith(unavailableFactory(creationAttempts))

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).performClick()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertIsDisplayed()
        assertEquals(2, creationAttempts.size)
    }

    @Test
    fun `the widget loads once the webview provider is back`() {
        var isProviderInstalled = false
        showWidgetWith { context ->
            if (isProviderInstalled) WebView(context) else throw AndroidRuntimeException("No WebView provider.")
        }
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertIsDisplayed()

        isProviderInstalled = true
        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_retry)).performClick()

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.fintoc_widget_loading)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(WIDGET_URL, shadowOf(webView()).lastLoadedUrl) }
    }

    @Test
    fun `a webview that breaks while it is being set up is freed and replaced by the message`() {
        lateinit var brokenWebView: WebView
        showWidgetWith { context ->
            object : WebView(context) {
                override fun loadUrl(url: String) = throw IllegalStateException("The provider died while loading.")
            }.also { createdWebView -> brokenWebView = createdWebView }
        }

        composeRule.onNodeWithText(string(R.string.fintoc_widget_error_webview_unavailable)).assertIsDisplayed()
        composeRule.runOnIdle {
            assertNull(findWebView())
            assertTrue(shadowOf(brokenWebView).wasDestroyCalled())
        }
    }

    @Test
    fun `a webview is destroyed exactly once when the widget leaves the screen`() {
        var destroyCalls = 0
        var isShown by mutableStateOf(true)
        composeRule.setContent {
            if (isShown) {
                FintocWidgetWebView(
                    url = WIDGET_URL,
                    onEvent = {},
                    externalLinkLauncher = {},
                    webViewFactory = { context ->
                        object : WebView(context) {
                            override fun destroy() {
                                destroyCalls += 1
                                super.destroy()
                            }
                        }
                    },
                )
            }
        }
        composeRule.runOnIdle { assertNotNull(findWebView()) }

        isShown = false
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertNull(findWebView())
            assertEquals(1, destroyCalls)
        }
    }

    private companion object {
        const val WIDGET_URL = "https://webview.fintoc.com/widget.html?product=payments"
    }
}
