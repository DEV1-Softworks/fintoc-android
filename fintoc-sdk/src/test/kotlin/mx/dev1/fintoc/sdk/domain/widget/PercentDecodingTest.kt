package mx.dev1.fintoc.sdk.domain.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentDecodingTest {

    @Test
    fun `value without escapes is returned untouched`() {
        assertEquals("plain value with spaces & symbols", PercentDecoding.decode("plain value with spaces & symbols"))
    }

    @Test
    fun `well formed escapes are decoded`() {
        assertEquals("a b&c=d", PercentDecoding.decode("a%20b%26c%3Dd"))
        assertEquals("a b&c=d", PercentDecoding.decode("a%20b%26c%3dd"))
    }

    @Test
    fun `consecutive escapes decode together as utf8`() {
        assertEquals("ñ€😀", PercentDecoding.decode("%C3%B1%E2%82%AC%F0%9F%98%80"))
    }

    @Test
    fun `characters that were never escaped are kept including emoji`() {
        assertEquals("ñ😀 raw%", PercentDecoding.decode("ñ😀 raw%"))
    }

    @Test
    fun `malformed escapes are kept as they are`() {
        assertEquals("100%", PercentDecoding.decode("100%"))
        assertEquals("50%2", PercentDecoding.decode("50%2"))
        assertEquals("%zz and %g1", PercentDecoding.decode("%zz and %g1"))
    }

    @Test
    fun `non ascii digits are not treated as hexadecimal`() {
        assertEquals("%١٢", PercentDecoding.decode("%١٢"))
    }

    @Test
    fun `plus stays a plus`() {
        assertEquals("a+b", PercentDecoding.decode("a+b"))
        assertEquals("a+b c", PercentDecoding.decode("a+b%20c"))
    }

    @Test
    fun `invalid utf8 never throws`() {
        assertEquals("�", PercentDecoding.decode("%FF"))
    }
}
