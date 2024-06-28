/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ConsoleKt")

package kotlin.io

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun print(message: Any?) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Int) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Long) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Byte) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Short) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Char) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Boolean) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Float) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Double) {
    System.out.print(message)
}

/** Prints the given [message] to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: CharArray) {
    System.out.print(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun println(message: Any?) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Int) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Long) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Byte) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Short) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Char) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Boolean) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Float) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Double) {
    System.out.println(message)
}

/** Prints the given [message] and the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: CharArray) {
    System.out.println(message)
}

/** Prints the line separator to the standard output stream. */
@kotlin.internal.InlineOnly
public actual inline fun println() {
    System.out.println()
}

/**
 * Reads a line of input from the standard input stream and returns it,
 * or throws a [RuntimeException] if EOF has already been reached when [readln] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is decoded using the system default Charset. A [CharacterCodingException] is thrown if input is malformed.
 */
@SinceKotlin("1.6")
public actual fun readln(): String = readlnOrNull() ?: throw ReadAfterEOFException("EOF has already been reached")

/**
 * Reads a line of input from the standard input stream and returns it,
 * or return `null` if EOF has already been reached when [readlnOrNull] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is decoded using the system default Charset. A [CharacterCodingException] is thrown if input is malformed.
 */
@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = readLine()

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
public fun readLine(): String? = LineReader.readLine(System.`in`, Charset.defaultCharset())

// Singleton object lazy initializes on the first use, internal for tests
internal object LineReader {
    private const val BUFFER_SIZE: Int = 32
    private lateinit var decoder: CharsetDecoder
    private var directEOL = false
    private val bytes = ByteArray(BUFFER_SIZE)
    private val chars = CharArray(BUFFER_SIZE)
    private val byteBuf: ByteBuffer = ByteBuffer.wrap(bytes)
    private val charBuf: CharBuffer = CharBuffer.wrap(chars)
    private val sb = StringBuilder()

    /**
     * Reads line from the specified [inputStream] with the given [charset].
     * The general design:
     * * This function contains only fast path code and all it state is kept in local variables as much as possible.
     * * All the slow-path code is moved to separate functions and the call-sequence bytecode is minimized for it.
     */
    @Synchronized
    fun readLine(inputStream: InputStream, charset: Charset): String? { // charset == null -> use default
        if (!::decoder.isInitialized || decoder.charset() != charset) updateCharset(charset)
        var nBytes = 0
        var nChars = 0
        while (true) {
            val readByte = inputStream.read()
            if (readByte == -1) {
                // The result is null only if there was absolutely nothing read
                if (sb.isEmpty() && nBytes == 0 && nChars == 0) {
                    return null
                } else {
                    nChars = decodeEndOfInput(nBytes, nChars) // throws exception if partial char
                    break
                }
            } else {
                bytes[nBytes++] = readByte.toByte()
            }
            // With "directEOL" encoding bytes are batched before being decoded all at once
            if (readByte == '\n'.code || nBytes == BUFFER_SIZE || !directEOL) {
                // Decode the bytes that were read
                byteBuf.limit(nBytes) // byteBuf position is always zero
                charBuf.position(nChars) // charBuf limit is always BUFFER_SIZE
                nChars = decode(false)
                // Break when we have decoded end of line
                if (nChars > 0 && chars[nChars - 1] == '\n') {
                    byteBuf.position(0) // reset position for next use
                    break
                }
                // otherwise we're going to read more bytes, so compact byteBuf
                nBytes = compactBytes()
            }
        }
        // Trim the end of line
        if (nChars > 0 && chars[nChars - 1] == '\n') {
            nChars--
            if (nChars > 0 && chars[nChars - 1] == '\r') nChars--
        }
        // Fast path for short lines (don't use StringBuilder)
        if (sb.isEmpty()) return String(chars, 0, nChars)
        // Copy the rest of chars to StringBuilder
        sb.append(chars, 0, nChars)
        // Build the result
        val result = sb.toString()
        if (sb.length > BUFFER_SIZE) trimStringBuilder()
        sb.setLength(0)
        return result
    }

    // The result is the number of chars in charBuf
    private fun decode(endOfInput: Boolean): Int {
        while (true) {
            val coderResult: CoderResult = decoder.decode(byteBuf, charBuf, endOfInput)
            if (coderResult.isError) {
                resetAll() // so that next call to readLine starts from clean state
                coderResult.throwException()
            }
            val nChars = charBuf.position()
            if (!coderResult.isOverflow) return nChars // has room in buffer -- everything possible was decoded
            // overflow (charBuf is full) -- offload everything from charBuf but last char into sb
            sb.append(chars, 0, nChars - 1)
            charBuf.position(0)
            charBuf.limit(BUFFER_SIZE)
            charBuf.put(chars[nChars - 1]) // retain last char
        }
    }

    // Slow path -- only on long lines (extra call to decode will be performed)
    private fun compactBytes(): Int = with(byteBuf) {
        compact()
        return position().also { position(0) }
    }

    // Slow path -- only on end of input
    private fun decodeEndOfInput(nBytes: Int, nChars: Int): Int {
        byteBuf.limit(nBytes) // byteBuf position is always zero
        charBuf.position(nChars) // charBuf limit is always BUFFER_SIZE
        return decode(true).also { // throws exception if partial char
            // reset decoder and byteBuf for next use
            decoder.reset()
            byteBuf.position(0)
        }
    }

    // Slow path -- only on charset change
    private fun updateCharset(charset: Charset) {
        decoder = charset.newDecoder()
        // try decoding ASCII line separator to see if this charset (like UTF-8) encodes it directly
        byteBuf.clear()
        charBuf.clear()
        byteBuf.put('\n'.code.toByte())
        byteBuf.flip()
        decoder.decode(byteBuf, charBuf, false)
        directEOL = charBuf.position() == 1 && charBuf.get(0) == '\n'
        resetAll()
    }

    // Slow path -- only on exception in decoder and on charset change
    private fun resetAll() {
        decoder.reset()
        byteBuf.position(0)
        sb.setLength(0)
    }

    // Slow path -- only on long lines
    private fun trimStringBuilder() {
        sb.setLength(BUFFER_SIZE)
        sb.trimToSize()
    }
}
