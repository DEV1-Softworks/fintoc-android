package mx.dev1.fintoc.sdk.domain.widget

import java.io.ByteArrayOutputStream

/**
 * Lenient percent-decoding for the redirects the Widget sends.
 *
 * The Widget does not encode the values it puts in a redirect, so a value may hold a lone `%`, a space or other
 * characters that are not valid in a URL. Only well-formed `%XX` sequences are decoded; everything else is kept as it
 * is. `+` stays a plus, because the Widget never uses form encoding.
 */
internal object PercentDecoding {
    private const val HEX_RADIX = 16
    private const val ESCAPE_LENGTH = 3

    fun decode(value: String): String {
        if (!value.contains('%')) return value

        val decoded = StringBuilder(value.length)
        val pendingBytes = ByteArrayOutputStream()
        var index = 0
        while (index < value.length) {
            val escapedByte = if (value[index] == '%') readEscapedByte(value, index) else null
            if (escapedByte != null) {
                pendingBytes.write(escapedByte)
                index += ESCAPE_LENGTH
            } else {
                flush(pendingBytes, decoded)
                decoded.append(value[index])
                index += 1
            }
        }
        flush(pendingBytes, decoded)
        return decoded.toString()
    }

    /** Escaped bytes form UTF-8 sequences, so they are decoded together rather than one by one. */
    private fun flush(pendingBytes: ByteArrayOutputStream, decoded: StringBuilder) {
        if (pendingBytes.size() == 0) return
        decoded.append(String(pendingBytes.toByteArray(), Charsets.UTF_8))
        pendingBytes.reset()
    }

    private fun readEscapedByte(value: String, percentIndex: Int): Int? {
        if (percentIndex + ESCAPE_LENGTH > value.length) return null
        val highDigit = hexDigitValue(value[percentIndex + 1])
        val lowDigit = hexDigitValue(value[percentIndex + 2])
        if (highDigit < 0 || lowDigit < 0) return null
        return highDigit * HEX_RADIX + lowDigit
    }

    /** Only ASCII hexadecimal digits count: `Character.digit` would also accept other scripts' digits. */
    private fun hexDigitValue(character: Char): Int = when (character) {
        in '0'..'9' -> character - '0'
        in 'a'..'f' -> character - 'a' + DECIMAL_DIGIT_COUNT
        in 'A'..'F' -> character - 'A' + DECIMAL_DIGIT_COUNT
        else -> -1
    }

    private const val DECIMAL_DIGIT_COUNT = 10
}
