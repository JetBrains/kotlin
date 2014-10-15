package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.net.URL
import java.util.ArrayList
import java.util.NoSuchElementException

/**
 * Creates a new [[FileReader]] for this file
 */
public fun File.reader(): FileReader = FileReader(this)

/**
 * Reads the entire content of the file as bytes
 *
 * This method is not recommended on huge files.
 */
public fun File.readBytes(): ByteArray {
    return FileInputStream(this).use { it.readBytes(length().toInt()) }
}

/**
 * Writes the bytes as the contents of the file
 */
public fun File.writeBytes(data: ByteArray): Unit {
    return FileOutputStream(this).use { it.write(data) }
}

/**
 * Appends bytes to the contents of the file.
 */
public fun File.appendBytes(data: ByteArray): Unit {
    return FileOutputStream(this, true).use { it.write(data) }
}

/**
 * Reads the entire content of the file as a String using specified charset.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of the file as a String using UTF-8 or specified charset.
 *
 * This method is not recommended on huge files.
 */
public fun File.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Writes the text as the contents of the file using specified charset.
 */
public fun File.writeText(text: String, charset: String): Unit {
    writeBytes(text.toByteArray(charset))
}

/**
 * Writes the text as the contents of the file using UTF-8 or specified charset.
 */
public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    writeBytes(text.toByteArray(charset))
}

/**
 * Appends text to the contents of the file using UTF-8 or specified charset.
 */
public fun File.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    appendBytes(text.toByteArray(charset))
}

/**
 * Appends text to the contents of the file using specified charset.
 */
public fun File.appendText(text: String, charset: String): Unit {
    appendBytes(text.toByteArray(charset))
}

/**
 * Reads file by byte blocks and calls closure for each block read. Block size depends on implementation but never less than 512.
 * This functions passes byte array and amount of bytes in this buffer to the closure function.
 *
 * You can use this function for huge files
 */
public fun File.forEachBlock(closure: (ByteArray, Int) -> Unit): Unit {
    val arr = ByteArray(4096)
    val fis = FileInputStream(this)

    try {
        do {
            val size = fis.read(arr)
            if (size == -1) {
                break
            } else if (size > 0) {
                closure(arr, size)
            }
        } while (true)
    } finally {
        fis.close()
    }
}

/**
 * Reads file line by line using specified [charset]. Default charset is UTF-8.
 *
 * You may use this function on huge files
 */
public fun File.forEachLine(charset: Charset = Charsets.UTF_8, closure: (line: String) -> Unit): Unit {
    val reader = BufferedReader(InputStreamReader(FileInputStream(this), charset))
    try {
        reader.forEachLine(closure)
    } finally {
        reader.close()
    }
}

/**
 * Reads file line by line using the specified [charset].
 *
 * You may use this function on huge files
 */
public fun File.forEachLine(charset: String, closure: (line: String) -> Unit): Unit = forEachLine(Charset.forName(charset), closure)

/**
 * Reads file content into list of lines using specified [charset]
 *
 * Do not use this function for huge files.
 */
public fun File.readLines(charset: String): List<String> = readLines(Charset.forName(charset))

/**
 * Reads file content as strings list. By default uses UTF-8 charset.
 *
 * Do not use this function for huge files.
 */
public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

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

public inline fun <T> Reader.useLines(block: (Stream<String>) -> T): T =
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
 * Reads the entire content of the URL as a String using specified charset.
 *
 * This method is not recommended on huge files.
 */
public fun URL.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of the URL as a String using UTF-8 or specified charset..
 *
 * This method is not recommended on huge files.
 */
public fun URL.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Reads the entire content of the URL as bytes
 *
 * This method is not recommended on huge files.
 */
public fun URL.readBytes(): ByteArray = this.openStream()!!.use<InputStream, ByteArray>{ it.readBytes() }

/** Uses the given resource then closes it down correctly whether an exception is thrown or not */
public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
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
