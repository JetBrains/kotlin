package kotlin.io

import java.io.*
import java.nio.charset.*
import java.util.NoSuchElementException
import java.net.URL
import kotlin.InlineOption.ONLY_LOCAL_RETURN

/**
 * Returns the default buffer size when working with buffered streams
 */
public val defaultBufferSize: Int = 64 * 1024

/**
 * Returns the default [[Charset]] which defaults to UTF-8
 */
public val defaultCharset: Charset = Charset.forName("UTF-8")!!


/** Prints the given message to [[System.out]] */
public fun print(message: Any?) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Int) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Long) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Byte) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Short) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Char) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Boolean) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Float) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: Double) {
    System.out.print(message)
}
/** Prints the given message to [[System.out]] */
public fun print(message: CharArray) {
    System.out.print(message)
}

/** Prints the given message and newline to [[System.out]] */
public fun println(message: Any?) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Int) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Long) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Byte) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Short) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Char) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Boolean) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Float) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: Double) {
    System.out.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public fun println(message: CharArray) {
    System.out.println(message)
}
/** Prints a newline t[[System.out]] */
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

/** Reads a line of input from [[System.in]] */
public fun readLine(): String? = stdin.readLine()

/** Returns an [Iterator] of bytes over an input stream */
public fun InputStream.iterator(): ByteIterator =
        object: ByteIterator() {
            override fun hasNext(): Boolean = available() > 0

            public override fun nextByte(): Byte = read().toByte()
        }

/** Creates a buffered input stream */
public fun InputStream.buffered(bufferSize: Int = defaultBufferSize): InputStream
        = if (this is BufferedInputStream)
    this
else
    BufferedInputStream(this, bufferSize)

/** Creates a reader on an input stream with specified *encoding* */
public fun InputStream.reader(encoding: Charset = defaultCharset): InputStreamReader = InputStreamReader(this, encoding)

/** Creates a reader on an input stream with specified *encoding* */
public fun InputStream.reader(encoding: String): InputStreamReader = InputStreamReader(this, encoding)

/** Creates a reader on an input stream with specified *encoding* */
public fun InputStream.reader(encoding: CharsetDecoder): InputStreamReader = InputStreamReader(this, encoding)


/** Creates a buffered output stream */
public fun OutputStream.buffered(bufferSize: Int = defaultBufferSize): BufferedOutputStream
        = if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

/** Creates a writer on an output stream with specified *encoding* */
public fun OutputStream.writer(encoding: Charset = defaultCharset): OutputStreamWriter = OutputStreamWriter(this, encoding)

/** Creates a writer on an output stream with specified *encoding* */
public fun OutputStream.writer(encoding: String): OutputStreamWriter = OutputStreamWriter(this, encoding)

/** Creates a writer on an output stream with specified *encoding* */
public fun OutputStream.writer(encoding: CharsetEncoder): OutputStreamWriter = OutputStreamWriter(this, encoding)


/** Creates a buffered reader, or returns self if Reader is already buffered */
public fun Reader.buffered(bufferSize: Int = defaultBufferSize): BufferedReader
        = if (this is BufferedReader) this else BufferedReader(this, bufferSize)

/** Creates a buffered writer, or returns self if Writer is already buffered */
public fun Writer.buffered(bufferSize: Int = defaultBufferSize): BufferedWriter
        = if (this is BufferedWriter) this else BufferedWriter(this, bufferSize)

/**
 * Iterates through each line of this reader then closing the [[Reader]] when its completed
 */
public fun Reader.forEachLine(block: (String) -> Unit): Unit = useLines { lines -> lines.forEach(block) }

public inline fun <T> Reader.useLines([inlineOptions(ONLY_LOCAL_RETURN)] block: (Stream<String>) -> T): T =
        this.buffered().use { block(it.lines()) }

/**
 * Returns an iterator over each line.
 * <b>Note</b> the caller must close the underlying <code>BufferedReader</code>
 * when the iteration is finished; as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early.
 * <br>
 * We suggest you try the method useLines() instead which closes the stream when the processing is complete.
 */
public fun BufferedReader.lines(): Stream<String> = LinesStream(this)

deprecated("Use lines() function which returns Stream<String>")
public fun BufferedReader.lineIterator(): Iterator<String> = lines().iterator()

private class LinesStream(private val reader: BufferedReader) : Stream<String> {
    override fun iterator(): Iterator<String> {
        return object : Iterator<String> {
            private var nextValue: String? = null
            private var done = false

            override fun hasNext(): Boolean {
                if (nextValue == null && !done) {
                    nextValue = reader.readLine()
                    if (nextValue == null) done = true
                }
                return nextValue != null
            }

            public override fun next(): String {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                val answer = nextValue
                nextValue = null
                return answer!!
            }
        }
    }
}


/**
 * Reads this stream completely into a byte array
 *
 * **Note** it is the callers responsibility to close this resource
 */
public fun InputStream.readBytes(estimatedSize: Int = defaultBufferSize): ByteArray {
    val buffer = ByteArrayOutputStream(estimatedSize)
    this.copyTo(buffer)
    return buffer.toByteArray()
}

/**
 * Reads this reader completely as a String
 *
 * **Note** it is the callers responsibility to close this resource
 */
public fun Reader.readText(): String {
    val buffer = StringWriter()
    copyTo(buffer)
    return buffer.toString()
}

/**
 * Copies this stream to the given output stream, returning the number of bytes copied
 *
 * **Note** it is the callers responsibility to close both of these resources
 */
public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = defaultBufferSize): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}

/**
 * Copies this reader to the given output writer, returning the number of bytes copied.
 *
 * **Note** it is the callers responsibility to close both of these resources
 */
public fun Reader.copyTo(out: Writer, bufferSize: Int = defaultBufferSize): Long {
    var charsCopied: Long = 0
    val buffer = CharArray(bufferSize)
    var chars = read(buffer)
    while (chars >= 0) {
        out.write(buffer, 0, chars)
        charsCopied += chars
        chars = read(buffer)
    }
    return charsCopied
}

/**
 * Reads the entire content of the URL as a String with a character set name
 *
 * This method is not recommended on huge files.
 */
public fun URL.readText(encoding: String = Charset.defaultCharset().name()): String = readBytes().toString(encoding)

/**
 * Reads the entire content of the URL as a String with the specified character encoding.
 *
 * This method is not recommended on huge files.
 */
public fun URL.readText(encoding: Charset): String = readBytes().toString(encoding)

/**
 * Reads the entire content of the URL as bytes
 *
 * This method is not recommended on huge files.
 */
public fun URL.readBytes(): ByteArray = this.openStream()!!.use<InputStream, ByteArray>{ it.readBytes() }

