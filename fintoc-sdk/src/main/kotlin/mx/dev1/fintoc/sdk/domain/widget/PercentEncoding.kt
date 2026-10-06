package mx.dev1.fintoc.sdk.domain.widget

/**
 * RFC 3986 percent-encoding for query-string components.
 *
 * It leaves only the unreserved characters (`A-Z a-z 0-9 - . _ ~`) as they are and encodes everything else as
 * uppercase `%XX` over the UTF-8 bytes. Unlike `java.net.URLEncoder` it writes a space as `%20`, never `+`, so a
 * value is decoded the same way by every parser.
 */
internal object PercentEncoding {
    private const val HEX_DIGITS = "0123456789ABCDEF"
    private const val LOW_NIBBLE_MASK = 0x0F
    private const val BITS_PER_NIBBLE = 4
    private const val BYTE_MASK = 0xFF

    fun encode(value: String): String {
        val encoded = StringBuilder(value.length)
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val unsignedByte = byte.toInt() and BYTE_MASK
            val character = unsignedByte.toChar()
            if (isUnreserved(character)) {
                encoded.append(character)
            } else {
                encoded.append('%')
                encoded.append(HEX_DIGITS[unsignedByte shr BITS_PER_NIBBLE])
                encoded.append(HEX_DIGITS[unsignedByte and LOW_NIBBLE_MASK])
            }
        }
        return encoded.toString()
    }

    private fun isUnreserved(character: Char): Boolean =
        character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
            character == '-' || character == '.' || character == '_' || character == '~'
}
