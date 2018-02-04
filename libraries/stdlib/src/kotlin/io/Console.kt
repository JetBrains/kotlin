@file:JvmVersion
@file:JvmName("ConsoleKt")

package kotlin.io

import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Any?) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Int) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Long) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Byte) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Short) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Char) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Boolean) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Float) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: Double) {
    System.out.print(message)
}

/** Prints the given message to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun print(message: CharArray) {
    System.out.print(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Any?) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Int) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Long) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Byte) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Short) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Char) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Boolean) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Float) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: Double) {
    System.out.println(message)
}

/** Prints the given message and newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println(message: CharArray) {
    System.out.println(message)
}

/** Prints a newline to the standard output stream. */
@kotlin.internal.InlineOnly
public inline fun println() {
    System.out.println()
}

private const val BUFFER_SIZE: Int = 32
private const val LINE_SEPARATOR_MAX_LENGTH: Int = 2

private var cachedDecoder: CharsetDecoder? = null

private fun decoderFor(charset: Charset): CharsetDecoder {
    return cachedDecoder?.takeIf { cached ->
        cached.charset() == charset
    } ?: charset.newDecoder().also { newDecoder ->
        cachedDecoder = newDecoder
    }
}

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
fun readLine(charset: Charset = Charset.defaultCharset()): String? = readLine(System.`in`, decoderFor(charset))

internal fun readLine(inputStream: InputStream, decoder: CharsetDecoder): String? {
    require(decoder.maxCharsPerByte() <= 1) { "Encodings with multiple chars per byte are not supported" }

    val byteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
    val charBuffer = CharBuffer.allocate(LINE_SEPARATOR_MAX_LENGTH)
    val stringBuilder = StringBuilder()

    var read = inputStream.read()
    if (read == -1) return null
    while (read != -1) {
        byteBuffer.put(read.toByte())
        if (decoder.tryDecode(byteBuffer, charBuffer, false)) {
            if (charBuffer.containsLineSeparator()) {
                break
            }
            if (!charBuffer.hasRemaining()) {
                stringBuilder.append(charBuffer.dequeue())
            }
        }
        read = inputStream.read()
    }

    with(decoder) {
        tryDecode(byteBuffer, charBuffer, true) // throws exception if undecoded bytes are left
        reset()
    }

    with(charBuffer) {
        val length = position()
        val first = get(0)
        val second = get(1)
        flip()
        when (length) {
            2 -> {
                if (!(first == '\r' && second == '\n')) stringBuilder.append(first)
                if (second != '\n') stringBuilder.append(second)
            }
            1 -> if (first != '\n') stringBuilder.append(first)
        }
    }

    return stringBuilder.toString()
}

private fun CharsetDecoder.tryDecode(byteBuffer: ByteBuffer, charBuffer: CharBuffer, isEndOfStream: Boolean): Boolean {
    val positionBefore = charBuffer.position()
    byteBuffer.flip()
    with(decode(byteBuffer, charBuffer, isEndOfStream)) {
        if (isError) throwException()
    }
    return (charBuffer.position() > positionBefore).also { isDecoded ->
        byteBuffer.apply { if (isDecoded) clear() else flipBack() }
    }
}

private fun CharBuffer.containsLineSeparator(): Boolean {
    return get(1) == '\n' || get(0) == '\n'
}

private fun Buffer.flipBack(): Buffer = apply {
    position(limit())
    limit(capacity())
}

private fun CharBuffer.dequeue(): Char {
    flip()
    return get().also { compact() }
}