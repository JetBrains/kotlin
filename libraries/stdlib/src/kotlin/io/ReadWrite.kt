@file:JvmVersion
@file:JvmName("TextStreamsKt")
package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.net.URL
import java.util.NoSuchElementException


/** Returns a buffered reader wrapping this Reader, or this Reader itself if it is already buffered. */
public fun Reader.buffered(bufferSize: Int = defaultBufferSize): BufferedReader
        = if (this is BufferedReader) this else BufferedReader(this, bufferSize)

/** Returns a buffered reader wrapping this Writer, or this Writer itself if it is already buffered. */
public fun Writer.buffered(bufferSize: Int = defaultBufferSize): BufferedWriter
        = if (this is BufferedWriter) this else BufferedWriter(this, bufferSize)

/**
 * Iterates through each line of this reader, calls [block] for each line read
 * and closes the [Reader] when it's completed.
 *
 * @param block function to process file lines.
 */
public fun Reader.forEachLine(block: (String) -> Unit): Unit = useLines { it.forEach(block) }

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.
 * @return the value returned by [block].
 */
public inline fun <T> Reader.useLines(block: (Sequence<String>) -> T): T =
        buffered().use { block(it.lineSequence()) }

/** Creates a new reader for the string. */
public fun String.reader(): StringReader = StringReader(this)

/**
 * Returns a sequence of corresponding file lines.
 *
 * *Note*: the caller must close the underlying `BufferedReader`
 * when the iteration is finished; as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early.
 *
 * We suggest you try the method [useLines] instead which closes the stream when the processing is complete.
 *
 * @return a sequence of corresponding file lines. The sequence returned can be iterated only once.
 */
public fun BufferedReader.lineSequence(): Sequence<String> = LinesSequence(this).constrainOnce()

private class LinesSequence(private val reader: BufferedReader) : Sequence<String> {
    override public fun iterator(): Iterator<String> {
        return object : Iterator<String> {
            private var nextValue: String? = null
            private var done = false

            override public fun hasNext(): Boolean {
                if (nextValue == null && !done) {
                    nextValue = reader.readLine()
                    if (nextValue == null) done = true
                }
                return nextValue != null
            }

            override public fun next(): String {
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
 * Reads this reader completely as a String.
 *
 * *Note*:  It is the caller's responsibility to close this reader.
 *
 * @return the string with corresponding file content.
 */
public fun Reader.readText(): String {
    val buffer = StringWriter()
    copyTo(buffer)
    return buffer.toString()
}

/**
 * Copies this reader to the given [out] writer, returning the number of bytes copied.
 *
 * **Note** it is the caller's responsibility to close both of these resources.
 *
 * @param out writer to write to.
 * @param bufferSize size of character buffer to use in process.
 * @return number of bytes copies.
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
 * Reads the entire content of this URL as a String using the specified [charset].
 *
 * This method is not recommended on huge files.
 *
 * @param charset a character set to use.
 * @return a string with this URL entire content.
 */
public fun URL.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of this URL as a String using UTF-8 or the specified [charset].
 *
 * This method is not recommended on huge files.
 *
 * @param charset a character set to use.
 * @return a string with this URL entire content.
 */
public fun URL.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Reads the entire content of the URL as byte array.
 *
 * This method is not recommended on huge files.
 *
 * @return a byte array with this URL entire content.
 */
public fun URL.readBytes(): ByteArray = openStream().use { it.readBytes() }

// TODO: Move to kotlin package, rename to using
/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this closable resource.
 * @return the result of [block] function on this closable resource.
 */
public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            close()
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
            close()
        }
    }
}
