package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.net.URL
import java.util.ArrayList
import java.util.NoSuchElementException

/**
 * Returns a new [FileReader] for reading the content of this file.
 */
public fun File.reader(): FileReader = FileReader(this)

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param bufferSize necessary size of the buffer
 */
public fun File.bufferedReader(bufferSize: Int = defaultBufferSize): BufferedReader = reader().buffered(bufferSize)

/**
 * Returns a new [FileWriter] for writing the content of this file.
 */
public fun File.writer(): FileWriter = FileWriter(this)

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param bufferSize necessary size of the buffer
 */
public fun File.bufferedWriter(bufferSize: Int = defaultBufferSize): BufferedWriter = writer().buffered(bufferSize)

/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
public fun File.printWriter(): PrintWriter = PrintWriter(writer().buffered())

/**
 * Reads the entire content of this file as a byte array.
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size
 *
 * @return the entire content of this file as a byte array
 */
public fun File.readBytes(): ByteArray {
    return FileInputStream(this).use { it.readBytes(length().toInt()) }
}

/**
 * Replaces the content of this file with [data].
 *
 * @param data byte array to write into this file
 */
deprecated("Use replaceBytes instead")
public fun File.writeBytes(data: ByteArray): Unit {
    return FileOutputStream(this).use { it.write(data) }
}

/**
 * Replaces the content of this file with [data].
 *
 * @param data byte array to write into this file
 */
public fun File.replaceBytes(data: ByteArray): Unit {
    return FileOutputStream(this).use { it.write(data) }
}

/**
 * Appends [data] to the content of this file.
 *
 * @param data byte array to append to this file
 */
public fun File.appendBytes(data: ByteArray): Unit {
    return FileOutputStream(this, true).use { it.write(data) }
}

/**
 * Reads the entire content of this file as a String using specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size
 *
 * @param charset character set to use
 * @return the entire content of this file as a String
 */
public fun File.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size
 *
 * @param charset character set to use
 * @return the entire content of this file as a String
 */
public fun File.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Replaces the content of this file with [text] encoded using the specified [charset].
 *
 * @param text text to write into file
 * @param charset character set to use
 */
deprecated("Use replaceText instead")
public fun File.writeText(text: String, charset: String): Unit {
    replaceBytes(text.toByteArray(charset))
}

/**
 * Replaces the content of this file with [text] encoded using the specified [charset].
 *
 * @param text text to write into file
 * @param charset character set to use
 */
deprecated("There is a same function with default parameter value")
public fun File.replaceText(text: String, charset: String): Unit {
    replaceBytes(text.toByteArray(charset))
}

/**
 * Replaces the content of this file with [text] encoded using UTF-8 or specified [charset].
 *
 * @param text text to write into file
 * @param charset character set to use
 */
deprecated("Use replaceText instead")
public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    replaceBytes(text.toByteArray(charset))
}

/**
 * Replaces the content of this file with [text] encoded using UTF-8 or specified [charset].
 *
 * @param text text to write into file
 * @param charset character set to use
 */
public fun File.replaceText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    replaceBytes(text.toByteArray(charset))
}

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file
 * @param charset character set to use
 */
public fun File.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    appendBytes(text.toByteArray(charset))
}

/**
 * Appends [text] to the content of the file using the specified [charset].
 *
 * @param text text to append to file
 * @param charset character set to use
 */
public fun File.appendText(text: String, charset: String): Unit {
    appendBytes(text.toByteArray(charset))
}

/**
 * Reads file by byte blocks and calls [closure] for each block read.
 * Block has default size which is implementation-dependent.
 * This functions passes the byte array and amount of bytes in the array to the [closure] function.
 *
 * You can use this function for huge files.
 *
 * @param closure function to proceed file blocks
 */
public fun File.forEachBlock(closure: (ByteArray, Int) -> Unit): Unit = forEachBlock(closure, defaultBlockSize)

/**
 * Reads file by byte blocks and calls [closure] for each block read.
 * This functions passes the byte array and amount of bytes in the array to the [closure] function.
 *
 * You can use this function for huge files.
 *
 * @param closure function to proceed file blocks
 * @param blockSize size of a block, replaced by 512 if it's less, 4096 by default
 */
public fun File.forEachBlock(closure: (ByteArray, Int) -> Unit, blockSize: Int): Unit {
    val arr = ByteArray(if (blockSize < minimumBlockSize) minimumBlockSize else blockSize)
    val fis = FileInputStream(this)

    try {
        do {
            val size = fis.read(arr)
            if (size <= 0) {
                break
            } else {
                closure(arr, size)
            }
        } while (true)
    } finally {
        fis.close()
    }
}

/**
 * Reads this file line by line using the specified [charset] and calls [closure] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use
 * @param closure function to proceed file lines
 */
public fun File.forEachLine(charset: Charset = Charsets.UTF_8, closure: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    BufferedReader(InputStreamReader(FileInputStream(this), charset)).forEachLine(closure)
}

/**
 * Reads this file line by line using the specified [charset] and calls [closure] for each line.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use
 * @param closure function to proceed file lines
 */
public fun File.forEachLine(charset: String, closure: (line: String) -> Unit): Unit = forEachLine(Charset.forName(charset), closure)

/**
 * Reads the file content as a list of lines, using the specified [charset].
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use
 * @return list of file lines
 */
public fun File.readLines(charset: String): List<String> = readLines(Charset.forName(charset))

/**
 * Reads the file content as a list of lines. By default uses UTF-8 charset.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use
 * @return list of file lines
 */
public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

/** Returns a buffered reader wrapping this Reader, or this Reader itself if it is already buffered. */
public fun Reader.buffered(bufferSize: Int = defaultBufferSize): BufferedReader
        = if (this is BufferedReader) this else BufferedReader(this, bufferSize)

/** Returns a buffered reader wrapping this Writer, or this Writer itself if it is already buffered. */
public fun Writer.buffered(bufferSize: Int = defaultBufferSize): BufferedWriter
        = if (this is BufferedWriter) this else BufferedWriter(this, bufferSize)

/**
 * Iterates through each line of this reader, calls [block] for each line read
 * and closes the [Reader] when it's completed
 *
 * @param block function to proceed file lines
 */
public fun Reader.forEachLine(block: (String) -> Unit): Unit = useLines { it.forEach(block) }

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.
 * @return the value returned by [block].
 */
public inline fun <T> Reader.useLines(block: (Sequence<String>) -> T): T =
        buffered().use { block(it.lines()) }

/**
 * Returns a sequence of corresponding file lines
 *
 * *Note*: the caller must close the underlying `BufferedReader`
 * when the iteration is finished; as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early.
 *
 * We suggest you try the method [useLines] instead which closes the stream when the processing is complete.
 *
 * @return a sequence of corresponding file lines
 */
public fun BufferedReader.lines(): Sequence<String> = LinesStream(this)

deprecated("Use lines() function which returns Sequence<String>")
public fun BufferedReader.lineIterator(): Iterator<String> = lines().iterator()

private class LinesStream(private val reader: BufferedReader) : Sequence<String> {
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
 * Reads this reader completely as a String
 *
 * *Note*:  It is the caller's responsibility to close this resource.
 *
 * @return the string with corresponding file content
 */
public fun Reader.readText(): String {
    val buffer = StringWriter()
    copyTo(buffer)
    return buffer.toString()
}

/**
 * Copies this reader to the given [out] writer, returning the number of bytes copied.
 *
 * **Note** it is the caller's responsibility to close both of these resources
 *
 * @param out writer to write to
 * @param bufferSize size of character buffer to use in process
 * @return number of bytes copies
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
 * @param charset a character set to use
 * @return a string with this URL entire content
 */
public fun URL.readText(charset: String): String = readBytes().toString(charset)

/**
 * Reads the entire content of this URL as a String using UTF-8 or the specified [charset].
 *
 * This method is not recommended on huge files.
 *
 * @param charset a character set to use
 * @return a string with this URL entire content
 */
public fun URL.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Reads the entire content of the URL as bytes
 *
 * This method is not recommended on huge files.
 *
 * @return a byte array with this URL entire content
 */
public fun URL.readBytes(): ByteArray = openStream().use<InputStream, ByteArray>{ it.readBytes() }

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not
 *
 * @param block a function to proceed this closable resource
 * @return the result of [block] function on this closable resource
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
