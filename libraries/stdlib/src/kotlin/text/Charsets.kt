@file:JvmName("CharsetsKt")
@file:JvmVersion
package kotlin.text

import java.nio.charset.*

/**
 * Returns a named charset with the given [charsetName] name.
 *
 * @throws UnsupportedCharsetException If the specified named charset is not available.
 */
@kotlin.internal.InlineOnly
public inline fun charset(charsetName: String): Charset = Charset.forName(charsetName)

/**
 * Constant definitions for the standard [charsets](Charset). These
 * charsets are guaranteed to be available on every implementation of the Java
 * platform.
 */
public object Charsets {
    /**
     * Eight-bit UCS Transformation Format.
     */
    @JvmField
    public val UTF_8: Charset = Charset.forName("UTF-8")

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark.
     */
    @JvmField
    public val UTF_16: Charset = Charset.forName("UTF-16")

    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order.
     */
    @JvmField
    public val UTF_16BE: Charset = Charset.forName("UTF-16BE")

    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order.
     */
    @JvmField
    public val UTF_16LE: Charset = Charset.forName("UTF-16LE")

    /**
     * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
     * Unicode character set.
     */
    @JvmField
    public val US_ASCII: Charset = Charset.forName("US-ASCII")

    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
     */
    @JvmField
    public val ISO_8859_1: Charset = Charset.forName("ISO-8859-1")

    /**
     * 32-bit Unicode (or UCS) Transformation Format, byte order identified by an optional byte-order mark
     */
    public val UTF_32: Charset
        @JvmName("UTF32")
        get() = utf_32 ?: run {
            val charset: Charset = Charset.forName("UTF-32")
            utf_32 = charset
            charset
        }
    private var utf_32: Charset? = null

    /**
     * 32-bit Unicode (or UCS) Transformation Format, little-endian byte order.
     */
    public val UTF_32LE: Charset
        @JvmName("UTF32_LE")
        get() = utf_32le ?: run {
            val charset: Charset = Charset.forName("UTF-32LE")
            utf_32le = charset
            charset
        }
    private var utf_32le: Charset? = null

    /**
     * 32-bit Unicode (or UCS) Transformation Format, big-endian byte order.
     */
    public val UTF_32BE: Charset
        @JvmName("UTF32_BE")
        get() = utf_32be ?: run {
            val charset: Charset = Charset.forName("UTF-32BE")
            utf_32be = charset
            charset
        }
    private var utf_32be: Charset? = null

}
