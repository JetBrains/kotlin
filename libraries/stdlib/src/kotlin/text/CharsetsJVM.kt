package kotlin

import java.nio.charset.*
import kotlin.platform.*

/**
 * Constant definitions for the standard [charsets](Charset). These
 * charsets are guaranteed to be available on every implementation of the Java
 * platform.
 */
public object Charsets {
    /**
     * Eight-bit UCS Transformation Format
     */
    // TODO: KT-5766 Cannot compile function with optional parameters with platformStatic value
    public val UTF_8: Charset = Charset.forName("UTF-8")

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark
     */
    public val UTF_16: Charset = Charset.forName("UTF-16")

    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order
     */
    public val UTF_16BE: Charset = Charset.forName("UTF-16BE")

    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order
     */
    public val UTF_16LE: Charset = Charset.forName("UTF-16LE")

    /**
     * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
     * Unicode character set
     */
    public val US_ASCII: Charset = Charset.forName("US-ASCII")

    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
     */
    public val ISO_8859_1: Charset = Charset.forName("ISO-8859-1")
}
