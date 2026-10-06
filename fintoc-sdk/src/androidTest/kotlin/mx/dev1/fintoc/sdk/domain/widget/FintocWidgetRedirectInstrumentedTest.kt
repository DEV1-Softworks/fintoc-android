package mx.dev1.fintoc.sdk.domain.widget

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The WebView hands the app an Android `Uri`, and the app passes `uri.toString()` to the parser. These tests send the
 * redirects through the real `Uri` first, including values the Widget does not encode.
 */
@RunWith(AndroidJUnit4::class)
class FintocWidgetRedirectInstrumentedTest {

    private fun parseThroughAndroidUri(redirect: String): FintocWidgetEvent? =
        FintocWidgetRedirectParser.parse(Uri.parse(redirect).toString())

    @Test
    fun androidSeesTheSchemeAndActionTheWayFintocDocumentsThem() {
        val uri = Uri.parse("fintocwidget://succeeded")

        assertEquals("fintocwidget", uri.scheme)
        assertEquals("succeeded", uri.host)
        assertTrue(FintocWidgetRedirectParser.isRedirect(uri.toString()))
    }

    @Test
    fun succeededWithLinkIntentSurvivesAndroidUri() {
        val event = parseThroughAndroidUri(
            "fintocwidget://succeeded?object=link_intent&exchange_token=et_123&id=li_456&widget_token=wt_1",
        )

        assertEquals(
            FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_123", id = "li_456")),
            event,
        )
    }

    @Test
    fun exitSurvivesAndroidUri() {
        assertEquals(FintocWidgetEvent.Exited, parseThroughAndroidUri("fintocwidget://exit"))
    }

    @Test
    fun eventWithUnencodedValuesSurvivesAndroidUri() {
        val event = parseThroughAndroidUri(
            "fintocwidget://event/payment_error?timestamp=1700000000000&reason=a b&c&nested=[object Object]&gone=null",
        ) as FintocWidgetEvent.Occurred

        assertEquals(FintocWidgetEventType.PAYMENT_ERROR, event.type)
        assertEquals(1_700_000_000_000L, event.timestampMillis)
        assertEquals(mapOf("reason" to "a b", "c" to ""), event.metadata)
    }

    @Test
    fun widgetAddressIsNeverMistakenForARedirect() {
        val widgetUrl = FintocWidgetUrlBuilder.build(
            publicKey = "pk_test_abc123",
            options = FintocWidgetOptions.Payments(sessionToken = "cs_123"),
        )

        assertFalse(FintocWidgetRedirectParser.isRedirect(widgetUrl))
    }
}
