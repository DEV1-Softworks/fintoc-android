package mx.dev1.fintoc.sdk.domain.widget

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the URLs against Android's own `Uri` parser, which is what the WebView ends up using. */
@RunWith(AndroidJUnit4::class)
class FintocWidgetUrlInstrumentedTest {

    private val publicKey = "pk_test_abc123"

    @Test
    fun androidParsesTheWidgetAddress() {
        val uri = Uri.parse(
            FintocWidgetUrlBuilder.build(publicKey, FintocWidgetOptions.Payments(sessionToken = "cs_123_sec_456")),
        )

        assertEquals("https", uri.scheme)
        assertEquals("webview.fintoc.com", uri.host)
        assertEquals("/widget.html", uri.path)
        assertEquals(publicKey, uri.getQueryParameter("public_key"))
        assertEquals("payments", uri.getQueryParameter("product"))
        assertEquals("cs_123_sec_456", uri.getQueryParameter("session_token"))
        assertNull(uri.getQueryParameter("holder_type"))
    }

    @Test
    fun androidDecodesTrickyValuesBackToTheOriginal() {
        val options = FintocWidgetOptions.Movements(
            holderType = FintocHolderType.BUSINESS,
            country = FintocCountry.MEXICO,
            linkToken = "link_ña&b=c+d%20e#f?g/h:i@j😀",
            webhookUrl = "https://example.com/hook?a=1&b=2%203#section",
        )

        val uri = Uri.parse(FintocWidgetUrlBuilder.build(publicKey, options))

        assertEquals("business", uri.getQueryParameter("holder_type"))
        assertEquals("mx", uri.getQueryParameter("country"))
        assertEquals(options.linkToken, uri.getQueryParameter("link_token"))
        assertEquals(options.webhookUrl, uri.getQueryParameter("webhook_url"))
        assertEquals(publicKey, uri.getQueryParameter("public_key"))
    }
}
