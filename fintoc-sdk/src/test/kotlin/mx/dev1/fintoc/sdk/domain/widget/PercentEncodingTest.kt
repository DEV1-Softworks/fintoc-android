package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentEncodingTest {

    @Test
    fun `unreserved characters are kept as they are`() {
        val unreserved = "AZaz09-._~"

        assertEquals(unreserved, PercentEncoding.encode(unreserved))
    }

    @Test
    fun `empty value stays empty`() {
        assertEquals("", PercentEncoding.encode(""))
    }

    @Test
    fun `space is written as percent twenty and never as plus`() {
        assertEquals("a%20b", PercentEncoding.encode("a b"))
    }

    @Test
    fun `characters that could end or split a query value are encoded`() {
        assertEquals("%26%3D%2B%25%23%3F%2F%3A%40", PercentEncoding.encode("&=+%#?/:@"))
    }

    @Test
    fun `non ascii characters are encoded over their utf8 bytes`() {
        assertEquals("%C3%B1", PercentEncoding.encode("ñ"))
        assertEquals("%E2%82%AC", PercentEncoding.encode("€"))
        assertEquals("%F0%9F%98%80", PercentEncoding.encode("😀"))
    }

    @Test
    fun `hexadecimal digits are uppercase`() {
        assertEquals("%0A%1F%7F", PercentEncoding.encode("\n\u001F\u007F"))
    }
}
