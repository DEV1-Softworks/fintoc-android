package mx.dev1.fintoc.sdk.presentation.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CopyOnWriteArrayList
import mx.dev1.fintoc.sdk.R
import mx.dev1.fintoc.sdk.domain.widget.FintocLinkIntentResult
import mx.dev1.fintoc.sdk.domain.widget.FintocWidgetEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the Widget's WebView for real. The pages are `data:` URLs whose scripts behave like the Widget does, so the
 * tests need no network and show how Android itself reports each navigation.
 */
@RunWith(AndroidJUnit4::class)
class FintocWidgetWebViewInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val events = CopyOnWriteArrayList<FintocWidgetEvent>()
    private val openedLinks = CopyOnWriteArrayList<String>()

    private fun pageWithScript(script: String): String =
        "data:text/html;charset=utf-8," + Uri.encode("<html><body><script>$script</script></body></html>")

    private fun showWidget(url: String) {
        composeRule.setContent {
            FintocWidgetWebView(
                url = url,
                onEvent = { event -> events += event },
                externalLinkLauncher = { link -> openedLinks += link },
            )
        }
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
    }

    private fun findWebView(): WebView? = findWebView(composeRule.activity.window.decorView)

    private fun currentUrl(): String? {
        var url: String? = null
        composeRule.runOnUiThread { url = findWebView()?.url }
        return url
    }

    private fun waitForEvents(count: Int) = composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) { events.size >= count }

    @Test
    fun redirectsFromTheMainFrameReachTheAppInOrder() {
        showWidget(
            pageWithScript(
                """
                setTimeout(function() { location.href = 'fintocwidget://event/opened?timestamp=1700000000000&reason=a b'; }, 100);
                setTimeout(function() { location.href = 'fintocwidget://succeeded?object=link_intent&exchange_token=et_1&id=li_1'; }, 500);
                setTimeout(function() { location.href = 'fintocwidget://exit'; }, 900);
                """.trimIndent(),
            ),
        )

        waitForEvents(count = 3)

        assertEquals(
            listOf(
                FintocWidgetEvent.Occurred(
                    name = "opened",
                    timestampMillis = 1_700_000_000_000L,
                    metadata = mapOf("reason" to "a b"),
                ),
                FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_1", id = "li_1")),
                FintocWidgetEvent.Exited,
            ),
            events.toList(),
        )
        assertTrue("The page must stay where it is", currentUrl().orEmpty().startsWith("data:"))
    }

    @Test
    fun redirectsFromAFrameInsideThePageReachTheApp() {
        showWidget(
            pageWithScript(
                """
                var frame = document.createElement('iframe');
                frame.srcdoc = "<script>setTimeout(function() { location.href = 'fintocwidget://event/from_frame?timestamp=5'; }, 100);<\/script>";
                document.body.appendChild(frame);
                """.trimIndent(),
            ),
        )

        waitForEvents(count = 1)

        assertEquals(FintocWidgetEvent.Occurred(name = "from_frame", timestampMillis = 5L), events.first())
    }

    @Test
    fun windowsAndLinksThatLeaveFintocOpenOutsideAndTheWidgetStaysPut() {
        showWidget(
            pageWithScript(
                """
                setTimeout(function() { window.open('https://files.example.com/voucher.pdf'); }, 100);
                setTimeout(function() {
                    var link = document.createElement('a');
                    link.href = 'https://files.example.com/second.pdf';
                    link.target = '_blank';
                    document.body.appendChild(link);
                    link.click();
                }, 500);
                setTimeout(function() { location.href = 'fintocwidget://exit'; }, 1000);
                """.trimIndent(),
            ),
        )

        waitForEvents(count = 1)

        assertEquals(
            listOf("https://files.example.com/voucher.pdf", "https://files.example.com/second.pdf"),
            openedLinks.toList(),
        )
        assertTrue("The page must stay where it is", currentUrl().orEmpty().startsWith("data:"))
    }

    @Test
    fun unsafeNavigationsAreBlockedAndNeverOpened() {
        showWidget(
            pageWithScript(
                """
                setTimeout(function() { location.href = 'http://insecure.example/'; }, 100);
                setTimeout(function() { location.href = 'intent://scan/#Intent;scheme=zxing;end'; }, 500);
                setTimeout(function() { location.href = 'fintocwidget://exit'; }, 1000);
                """.trimIndent(),
            ),
        )

        waitForEvents(count = 1)

        assertTrue(openedLinks.isEmpty())
        assertTrue("The page must stay where it is", currentUrl().orEmpty().startsWith("data:"))
    }

    /**
     * Without this permission the WebView fails every load with `net::ERR_CACHE_MISS`, which tells the integrator
     * nothing. The SDK declares it in its own manifest so host apps cannot forget it.
     */
    @Test
    fun theSdkDeclaresTheInternetPermission() {
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.INTERNET),
        )
    }

    @Test
    fun theWebViewIsHardened() {
        showWidget(pageWithScript(""))
        composeRule.waitForIdle()

        composeRule.runOnUiThread {
            val settings = checkNotNull(findWebView()).settings
            assertTrue(settings.javaScriptEnabled)
            assertTrue(settings.domStorageEnabled)
            assertEquals(WebSettings.LOAD_NO_CACHE, settings.cacheMode)
            assertFalse(settings.allowFileAccess)
            assertFalse(settings.allowContentAccess)
            assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, settings.mixedContentMode)
            assertFalse(settings.supportMultipleWindows())
            assertTrue(settings.safeBrowsingEnabled)
        }
    }

    @Test
    fun anUnreachableAddressShowsTheFailureMessageInsteadOfTheBrowserErrorPage() {
        showWidget("https://fintoc-sdk-test.invalid/widget.html")

        composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) {
            isDisplayingText(context.getString(R.string.fintoc_widget_error_message))
        }

        composeRule.onNodeWithText(context.getString(R.string.fintoc_widget_error_message)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.fintoc_widget_error_retry)).assertIsDisplayed()
        composeRule.runOnUiThread { assertNull("The WebView must be gone", findWebView()) }
    }

    private fun isDisplayingText(text: String): Boolean =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WAIT_MILLIS = 20_000L
    }
}
