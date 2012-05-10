package kotlin.io

import java.io.*
import java.nio.charset.*
import java.util.NoSuchElementException
import java.net.URL

/**
 * Returns the default buffer size when working with buffered streams
 */
public val defaultBufferSize: Int = 64 * 1024

/**
 * Returns the default [[Charset]] which defaults to UTF-8
 */
public val defaultCharset: Charset = Charset.forName("UTF-8").sure()


/** Prints the given message to [[System.out]] */
public inline fun print(message : Any?) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Int) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Long) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Byte) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Short) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Char) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Boolean) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Float) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : Double) {
    System.out?.print(message)
}
/** Prints the given message to [[System.out]] */
public inline fun print(message : CharArray) {
    System.out?.print(message)
}

/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Any?) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Int) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Long) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Byte) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Short) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Char) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Boolean) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Float) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : Double) {
    System.out?.println(message)
}
/** Prints the given message and newline to [[System.out]] */
public inline fun println(message : CharArray) {
    System.out?.println(message)
}
/** Prints a newline t[[System.out]] */
public inline fun println() {
    System.out?.println()
}

private val stdin : BufferedReader = BufferedReader(InputStreamReader(object : InputStream() {
    public override fun read() : Int {
        return System.`in`?.read() ?: -1
    }

    public override fun reset() {
        System.`in`?.reset()
    }

    public override fun read(b: ByteArray?): Int {
        return System.`in`?.read(b) ?: -1
    }

    public override fun close() {
        System.`in`?.close()
    }

    public override fun mark(readlimit: Int) {
        System.`in`?.mark(readlimit)
    }

    public override fun skip(n: Long): Long {
        return System.`in`?.skip(n) ?: -1.toLong()
    }

    public override fun available(): Int {
        return System.`in`?.available() ?: 0
    }

    public override fun markSupported(): Boolean {
        return System.`in`?.markSupported() ?: false
    }

    public override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return System.`in`?.read(b, off, len) ?: -1
    }
}))

/** Reads a line of input from [[System.in]] */
public inline fun readLine() : String? = stdin.readLine()

/** Uses the given resource then closes it down correctly whether an exception is thrown or not */
public inline fun <T: Closeable, R> T.use(block: (T)-> R) : R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
            // eat the closeException as we are already throwing the original cause
            // and we don't want to mask the real exception

            // TODO on Java 7 we should call
            // e.addSuppressed(closeException)
            // to work like try-with-resources
            // http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html#suppressed-exceptions
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

/** Returns an [Iterator] of bytes over an input stream */
public fun InputStream.iterator() : ByteIterator =
object: ByteIterator() {
    override val hasNext : Boolean
    get() = available() > 0

    public override fun nextByte() : Byte = read().toByte()
}

/** Creates a buffered input stream */
public inline fun InputStream.buffered(bufferSize: Int = defaultBufferSize) : InputStream
= if (this is BufferedInputStream)
    this
else
    BufferedInputStream(this, bufferSize)

public inline fun InputStream.reader(encoding: Charset = defaultCharset) : InputStreamReader = InputStreamReader(this, encoding)

public inline fun InputStream.reader(encoding: String) : InputStreamReader = InputStreamReader(this, encoding)

public inline fun InputStream.reader(encoding: CharsetDecoder) : InputStreamReader = InputStreamReader(this, encoding)


public inline fun OutputStream.buffered(bufferSize: Int = defaultBufferSize) : BufferedOutputStream
= if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

public inline fun OutputStream.writer(encoding: Charset = defaultCharset) : OutputStreamWriter = OutputStreamWriter(this, encoding)

public inline fun OutputStream.writer(encoding: String) : OutputStreamWriter = OutputStreamWriter(this, encoding)

public inline fun OutputStream.writer(encoding: CharsetEncoder) : OutputStreamWriter = OutputStreamWriter(this, encoding)


public inline fun Reader.buffered(bufferSize: Int = defaultBufferSize): BufferedReader
= if(this is BufferedReader) this else BufferedReader(this, bufferSize)

public inline fun Writer.buffered(bufferSize: Int = defaultBufferSize): BufferedWriter
= if(this is BufferedWriter) this else BufferedWriter(this, bufferSize)


/**
 * Iterates through each line of this reader then closing the [[Reader]] when its completed
 */
public inline fun Reader.forEachLine(block: (String) -> Unit): Unit {
    this.use{
        val iter = buffered().lineIterator()
        while (iter.hasNext) {
            val elem = iter.next()
            block(elem)
        }
    }
}

public inline fun <T> Reader.useLines(block: (Iterator<String>) -> T): T = this.buffered().use<BufferedReader, T>{block(it.lineIterator())}

/**
 * Returns an iterator over each line.
 * <b>Note</b> the caller must close the underlying <code>BufferedReader</code>
 * when the iteration is finished; as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early.
 * <br>
 * We suggest you try the method useLines() instead which closes the stream when the processing is complete.
 */
public inline fun BufferedReader.lineIterator() : Iterator<String> = LineIterator(this)

class LineIterator(val reader: BufferedReader) : Iterator<String> {
    private var nextValue: String? = null
    private var done = false

    override val hasNext: Boolean
    get() {
        if (nextValue == null && !done) {
            nextValue = reader.readLine()
            if (nextValue == null) done = true
        }
        return nextValue != null
    }

    public override fun next(): String {
        if (!hasNext) {
            throw NoSuchElementException()
        }
        val answer = nextValue
        nextValue = null
        return answer.sure()
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
    return buffer.toByteArray().sure()
}

/**
 * Reads this reader completely as a String
 *
 * **Note** it is the callers responsibility to close this resource
 */
public fun Reader.readText(): String {
    val buffer = StringWriter()
    copyTo(buffer)
    return buffer.toString().sure()
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
 * Reads the entire content of the URL as a String with an optional character set name
 *
 * This method is not recommended on huge files.
 */
public fun URL.readText(encoding: String? = null): String = readBytes().toString(encoding)

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
public fun URL.readBytes(): ByteArray = this.openStream().sure().use<InputStream,ByteArray>{ it.readBytes() }

