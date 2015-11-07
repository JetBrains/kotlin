@file:JvmVersion
@file:JvmName("ByteStreamsKt")
package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.util.NoSuchElementException

/** Returns an [Iterator] of bytes in this input stream. */
@Deprecated("It's not recommended to iterate through input stream bytes")
public fun InputStream.iterator(): ByteIterator =
        object : ByteIterator() {

            var nextByte = -1

            var nextPrepared = false

            var finished = false

            private fun prepareNext() {
                if (!nextPrepared && !finished) {
                    nextByte = read()
                    nextPrepared = true
                    finished = (nextByte == -1)
                }
            }

            public override fun hasNext(): Boolean {
                prepareNext()
                return !finished
            }

            public override fun nextByte(): Byte {
                prepareNext()
                if (finished)
                    throw NoSuchElementException("Input stream is over")
                val res = nextByte.toByte()
                nextPrepared = false
                return res
            }
        }


/** Creates a new byte input stream for the string. */
public fun String.byteInputStream(charset: Charset = Charsets.UTF_8): InputStream = ByteArrayInputStream(toByteArray(charset))

/**
 * Creates an input stream for reading data from this byte array.
 */
public fun ByteArray.inputStream(): ByteArrayInputStream = ByteArrayInputStream(this)

/**
 * Creates an input stream for reading data from the specified portion of this byte array.
 * @param offset the start offset of the portion of the array to read.
 * @param length the length of the portion of the array to read.
 */
public fun ByteArray.inputStream(offset: Int, length: Int) : ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

/**
 * Creates a buffered input stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
public fun InputStream.buffered(bufferSize: Int = defaultBufferSize): InputStream
        = if (this is BufferedInputStream) this else BufferedInputStream(this, bufferSize)

/** Creates a reader on this input stream using UTF-8 or the specified [charset]. */
public fun InputStream.reader(charset: Charset = Charsets.UTF_8): InputStreamReader = InputStreamReader(this, charset)

/** Creates a buffered reader on this input stream using UTF-8 or the specified [charset]. */
public fun InputStream.bufferedReader(charset: Charset = Charsets.UTF_8): BufferedReader = reader(charset).buffered()

/** Creates a reader on this input stream using the specified [charset]. */
public fun InputStream.reader(charset: String): InputStreamReader = InputStreamReader(this, charset)

/** Creates a buffered reader on this input stream using the specified [charset]. */
public fun InputStream.bufferedReader(charset: String): BufferedReader = reader(charset).buffered()

/**
 * Creates a buffered output stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
public fun OutputStream.buffered(bufferSize: Int = defaultBufferSize): BufferedOutputStream
        = if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

/** Creates a writer on this output stream using UTF-8 or the specified [charset]. */
public fun OutputStream.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter = OutputStreamWriter(this, charset)

/** Creates a buffered writer on this output stream using UTF-8 or the specified [charset]. */
public fun OutputStream.bufferedWriter(charset: Charset = Charsets.UTF_8): BufferedWriter = writer(charset).buffered()

/** Creates a writer on this output stream using the specified [charset]. */
public fun OutputStream.writer(charset: String): OutputStreamWriter = OutputStreamWriter(this, charset)

/** Creates a buffered writer on this output stream using the specified [charset]. */
public fun OutputStream.bufferedWriter(charset: String): BufferedWriter = writer(charset).buffered()

/**
 * Copies this stream to the given output stream, returning the number of bytes copied
 *
 * **Note** It is the caller's responsibility to close both of these resources.
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
 * Reads this stream completely into a byte array.
 *
 * **Note**: It is the caller's responsibility to close this stream.
 */
public fun InputStream.readBytes(estimatedSize: Int = defaultBufferSize): ByteArray {
    val buffer = ByteArrayOutputStream(estimatedSize)
    copyTo(buffer)
    return buffer.toByteArray()
}

