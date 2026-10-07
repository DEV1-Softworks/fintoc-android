package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FintocWidgetRedirectParserTest {

    private fun parse(rawUrl: String): FintocWidgetEvent? = FintocWidgetRedirectParser.parse(rawUrl)

    @Test
    fun `plain succeeded redirect carries no link intent`() {
        assertEquals(FintocWidgetEvent.Succeeded(linkIntent = null), parse("fintocwidget://succeeded"))
    }

    @Test
    fun `succeeded redirect for a link intent carries the exchange token and id`() {
        val event = parse(
            "fintocwidget://succeeded?object=link_intent&exchange_token=et_123&id=li_456&widget_token=wt_1",
        )

        assertEquals(
            FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_123", id = "li_456")),
            event,
        )
    }

    @Test
    fun `link intent without an id still reports the exchange token`() {
        val event = parse("fintocwidget://succeeded?object=link_intent&exchange_token=et_123")

        assertEquals(FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_123")), event)
    }

    @Test
    fun `blank id is treated as missing`() {
        val event = parse("fintocwidget://succeeded?object=link_intent&exchange_token=et_123&id=")

        assertEquals(FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_123", id = null)), event)
    }

    @Test
    fun `succeeded without a usable link intent stays a plain success`() {
        listOf(
            "fintocwidget://succeeded?object=link_intent",
            "fintocwidget://succeeded?object=link_intent&exchange_token=",
            "fintocwidget://succeeded?object=checkout_session&exchange_token=et_123",
            "fintocwidget://succeeded?exchange_token=et_123",
        ).forEach { redirect ->
            assertEquals(redirect, FintocWidgetEvent.Succeeded(linkIntent = null), parse(redirect))
        }
    }

    @Test
    fun `exit redirect becomes exited`() {
        assertEquals(FintocWidgetEvent.Exited, parse("fintocwidget://exit"))
    }

    @Test
    fun `scheme and action are case insensitive`() {
        assertEquals(FintocWidgetEvent.Exited, parse("FintocWidget://EXIT"))
        assertEquals(FintocWidgetEvent.Succeeded(), parse("FINTOCWIDGET://Succeeded"))
    }

    @Test
    fun `fragment is ignored`() {
        assertEquals(FintocWidgetEvent.Exited, parse("fintocwidget://exit#anything"))
        assertEquals(
            FintocWidgetEvent.Occurred(name = "opened"),
            parse("fintocwidget://event/opened#timestamp=5"),
        )
    }

    @Test
    fun `event redirect reports name timestamp and the remaining metadata`() {
        val event = parse(
            "fintocwidget://event/payment_error?timestamp=1700000000000&reason=insufficient_funds&code=42",
        )

        assertEquals(
            FintocWidgetEvent.Occurred(
                name = "payment_error",
                timestampMillis = 1_700_000_000_000L,
                metadata = mapOf("reason" to "insufficient_funds", "code" to "42"),
            ),
            event,
        )
    }

    @Test
    fun `event without query has no timestamp and no metadata`() {
        assertEquals(FintocWidgetEvent.Occurred(name = "closed"), parse("fintocwidget://event/closed"))
    }

    @Test
    fun `invalid timestamp becomes null and is not kept as metadata`() {
        val event = parse("fintocwidget://event/opened?timestamp=yesterday") as FintocWidgetEvent.Occurred

        assertNull(event.timestampMillis)
        assertTrue(event.metadata.isEmpty())
    }

    @Test
    fun `unknown event names are still reported with their raw name`() {
        val event = parse("fintocwidget://event/brand_new_event?timestamp=1") as FintocWidgetEvent.Occurred

        assertEquals("brand_new_event", event.name)
        assertNull(event.type)
    }

    @Test
    fun `every documented event name resolves to its type`() {
        FintocWidgetEventType.entries.forEach { type ->
            val event = parse("fintocwidget://event/${type.code}") as FintocWidgetEvent.Occurred

            assertEquals(type, event.type)
        }
    }

    @Test
    fun `percent escapes are decoded in names and values`() {
        val event = parse("fintocwidget://event/on_error?message=bank%20is%20down&emoji=%F0%9F%98%80")

        assertEquals(
            mapOf("message" to "bank is down", "emoji" to "😀"),
            (event as FintocWidgetEvent.Occurred).metadata,
        )
    }

    @Test
    fun `values the widget did not encode are kept as they are`() {
        val event = parse("fintocwidget://event/on_error?message=100% sure ñ")

        assertEquals(mapOf("message" to "100% sure ñ"), (event as FintocWidgetEvent.Occurred).metadata)
    }

    @Test
    fun `values serialized from null undefined and objects are dropped`() {
        val event = parse(
            "fintocwidget://event/on_error?a=null&b=undefined&c=[object Object]&d=kept&e=",
        ) as FintocWidgetEvent.Occurred

        assertEquals(mapOf("d" to "kept", "e" to ""), event.metadata)
    }

    @Test
    fun `first occurrence of a key wins even when it was dropped`() {
        val event = parse(
            "fintocwidget://event/on_error?a=first&a=second&b=null&b=real",
        ) as FintocWidgetEvent.Occurred

        assertEquals(mapOf("a" to "first"), event.metadata)
    }

    @Test
    fun `key without equals sign becomes an empty value and empty keys are skipped`() {
        val event = parse("fintocwidget://event/on_error?flag&&=orphan&x=1") as FintocWidgetEvent.Occurred

        assertEquals(mapOf("flag" to "", "x" to "1"), event.metadata)
    }

    @Test
    fun `an unencoded ampersand in a value cannot override an earlier parameter`() {
        val event = parse(
            "fintocwidget://succeeded?object=link_intent&exchange_token=et_real&id=li_1" +
                "&widget_token=a&exchange_token=et_evil",
        )

        assertEquals(
            FintocWidgetEvent.Succeeded(FintocLinkIntentResult(exchangeToken = "et_real", id = "li_1")),
            event,
        )
    }

    @Test
    fun `parameters beyond the limit are ignored`() {
        val query = (1..100).joinToString("&") { number -> "k$number=v$number" }

        val event = parse("fintocwidget://event/opened?$query") as FintocWidgetEvent.Occurred

        assertEquals(64, event.metadata.size)
        assertTrue(event.metadata.containsKey("k64"))
        assertFalse(event.metadata.containsKey("k65"))
    }

    @Test
    fun `oversized redirects are ignored`() {
        assertNull(parse("fintocwidget://event/opened?big=" + "x".repeat(9_000)))
    }

    @Test
    fun `unexpected event names are ignored`() {
        listOf(
            "fintocwidget://event",
            "fintocwidget://event/",
            "fintocwidget://event/?timestamp=1",
            "fintocwidget://event/opened/extra",
            "fintocwidget://event/has space",
            "fintocwidget://event/<script>",
            "fintocwidget://event/" + "a".repeat(65),
        ).forEach { redirect -> assertNull(redirect, parse(redirect)) }
    }

    @Test
    fun `unknown actions and foreign addresses are ignored`() {
        listOf(
            "fintocwidget://",
            "fintocwidget://unknown",
            "https://webview.fintoc.com/widget.html",
            "http://fintocwidget://exit",
            "javascript:alert(1)",
            "",
            "fintocwidget:/exit",
        ).forEach { redirect -> assertNull(redirect, parse(redirect)) }
    }

    @Test
    fun `isRedirect recognizes the scheme even for actions the parser ignores`() {
        assertTrue(FintocWidgetRedirectParser.isRedirect("fintocwidget://succeeded"))
        assertTrue(FintocWidgetRedirectParser.isRedirect("FINTOCWIDGET://something-new"))
        assertFalse(FintocWidgetRedirectParser.isRedirect("https://webview.fintoc.com/widget.html"))
        assertFalse(FintocWidgetRedirectParser.isRedirect("fintocwidget:/exit"))
    }
}
