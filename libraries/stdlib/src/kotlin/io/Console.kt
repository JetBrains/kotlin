package kotlin.io

import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

/**
 * Returns the default buffer size when working with buffered streams
 */
public val defaultBufferSize: Int = 64 * 1024

/** Prints the given message to the standard output stream. */
public fun print(message: Any?) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Int) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Long) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Byte) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Short) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Char) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Boolean) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Float) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: Double) {
    System.out.print(message)
}
/** Prints the given message to the standard output stream. */
public fun print(message: CharArray) {
    System.out.print(message)
}

/** Prints the given message and newline to the standard output stream. */
public fun println(message: Any?) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Int) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Long) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Byte) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Short) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Char) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Boolean) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Float) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: Double) {
    System.out.println(message)
}
/** Prints the given message and newline to the standard output stream. */
public fun println(message: CharArray) {
    System.out.println(message)
}
/** Prints a newline to the standard output stream. */
public fun println() {
    System.out.println()
}

// Since System.in can change its value on the course of program running,
// we should always delegate to current value and cannot just pass it to InputStreamReader constructor.
// We could use "by" implementation, but we can only use "by" with traits and InputStream is abstract class.
private val stdin: BufferedReader = BufferedReader(InputStreamReader(object : InputStream() {
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
}))

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or null if the input stream is redirected to a file and the end of file has been reached.
 */
public fun readLine(): String? = stdin.readLine()
