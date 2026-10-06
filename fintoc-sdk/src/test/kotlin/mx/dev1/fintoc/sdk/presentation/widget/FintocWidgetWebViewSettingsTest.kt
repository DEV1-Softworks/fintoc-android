package mx.dev1.fintoc.sdk.presentation.widget

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FintocWidgetWebViewSettingsTest {

    private val webView = WebView(ApplicationProvider.getApplicationContext()).apply { applyFintocWidgetSettings() }

    @Test
    fun `settings the widget needs are on`() {
        with(webView.settings) {
            assertTrue(javaScriptEnabled)
            assertTrue(domStorageEnabled)
            assertTrue(javaScriptCanOpenWindowsAutomatically)
            assertTrue(useWideViewPort)
            assertEquals(WebSettings.LOAD_NO_CACHE, cacheMode)
        }
    }

    @Test
    fun `local files content providers and insecure subresources are off`() {
        with(webView.settings) {
            assertFalse(allowFileAccess)
            assertFalse(allowContentAccess)
            assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, mixedContentMode)
            assertFalse(supportMultipleWindows())
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `saved form data is off`() {
        assertFalse(webView.settings.saveFormData)
    }

    // Robolectric's fake WebSettings does not keep the safe browsing flag, so that setting is asserted on a real
    // WebView in the instrumented tests. WebSettings has no public getter for geolocation, so it cannot be asserted.
}
