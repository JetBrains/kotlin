@file:JvmVersion
@file:JvmName("ConsoleKt")
package kotlin.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

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

// Since System.in can change its value on the course of program running, we should always delegate to current value.
// We could use "by" implementation, but we can only use "by" with interfaces and InputStream is abstract class.
private val stdin: InputStream by lazy { object : InputStream() {
    public override fun read(): Int {
        return System.`in`.read()
    }

    public override fun reset() {
        System.`in`.reset()
    }

    public override fun read(b: ByteArray): Int {
        return System.`in`.read(b)
    }

    public override fun close() {
        System.`in`.close()
    }

    public override fun mark(readlimit: Int) {
        System.`in`.mark(readlimit)
    }

    public override fun skip(n: Long): Long {
        return System.`in`.skip(n)
    }

    public override fun available(): Int {
        return System.`in`.available()
    }

    public override fun markSupported(): Boolean {
        return System.`in`.markSupported()
    }

    public override fun read(b: ByteArray, off: Int, len: Int): Int {
        return System.`in`.read(b, off, len)
    }
}}

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
public fun readLine(): String? {
    // Writing line into ByteArray because InputStreamReader uses buffer
    val bytes = ByteArrayOutputStream().use { outputStream ->
        val inputStream = stdin
        val endOfStream = -1
        val carriageReturn = '\r'.toInt()
        val lineFeed = '\n'.toInt()

        var previous = inputStream.read()
        if (previous == endOfStream) return null
        if (previous == lineFeed) return ""
        var current = inputStream.read()

        while (current != endOfStream && current != lineFeed) {
            outputStream.write(previous)
            previous = current
            current = inputStream.read()
        }

        if (!(previous == carriageReturn && current == lineFeed)) outputStream.write(previous)

        outputStream.toByteArray()
    }

    return ByteArrayInputStream(bytes).reader().use { it.readText() }
}
