/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ByteStreamsKt")

package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.util.NoSuchElementException

/** Returns an [Iterator] of bytes read from this input stream. */
public operator fun BufferedInputStream.iterator(): ByteIterator =
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
                throw NoSuchElementException("Input stream is over.")
            val res = nextByte.toByte()
            nextPrepared = false
            return res
        }
    }


/** Creates a new byte input stream for the string. */
@kotlin.internal.InlineOnly
public inline fun String.byteInputStream(charset: Charset = Charsets.UTF_8): ByteArrayInputStream = ByteArrayInputStream(toByteArray(charset))

/**
 * Creates an input stream for reading data from this byte array.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.inputStream(): ByteArrayInputStream = ByteArrayInputStream(this)

/**
 * Creates an input stream for reading data from the specified portion of this byte array.
 * @param offset the start offset of the portion of the array to read.
 * @param length the length of the portion of the array to read.
 */
@kotlin.internal.InlineOnly
public inline fun ByteArray.inputStream(offset: Int, length: Int): ByteArrayInputStream = ByteArrayInputStream(this, offset, length)

/**
 * Creates a buffered input stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
@kotlin.internal.InlineOnly
public inline fun InputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream =
    if (this is BufferedInputStream) this else BufferedInputStream(this, bufferSize)

/** Creates a reader on this input stream using UTF-8 or the specified [charset]. */
@kotlin.internal.InlineOnly
public inline fun InputStream.reader(charset: Charset = Charsets.UTF_8): InputStreamReader = InputStreamReader(this, charset)

/** Creates a buffered reader on this input stream using UTF-8 or the specified [charset]. */
@kotlin.internal.InlineOnly
public inline fun InputStream.bufferedReader(charset: Charset = Charsets.UTF_8): BufferedReader = reader(charset).buffered()

/**
 * Creates a buffered output stream wrapping this stream.
 * @param bufferSize the buffer size to use.
 */
@kotlin.internal.InlineOnly
public inline fun OutputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream =
    if (this is BufferedOutputStream) this else BufferedOutputStream(this, bufferSize)

/** Creates a writer on this output stream using UTF-8 or the specified [charset]. */
@kotlin.internal.InlineOnly
public inline fun OutputStream.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter = OutputStreamWriter(this, charset)

/** Creates a buffered writer on this output stream using UTF-8 or the specified [charset]. */
@kotlin.internal.InlineOnly
public inline fun OutputStream.bufferedWriter(charset: Charset = Charsets.UTF_8): BufferedWriter = writer(charset).buffered()

/**
 * Copies this stream to the given output stream, returning the number of bytes copied
 *
 * **Note** It is the caller's responsibility to close both of these resources.
 */
public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
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
@Deprecated("Use readBytes() overload without estimatedSize parameter", ReplaceWith("readBytes()"))
@DeprecatedSinceKotlin(warningSince = "1.3", errorSince = "1.5")
public fun InputStream.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf(estimatedSize, this.available()))
    val _ = copyTo(buffer)
    return buffer.toByteArray()
}

/**
 * Reads this stream completely into a byte array.
 *
 * **Note**: It is the caller's responsibility to close this stream.
 */
@SinceKotlin("1.3")
public fun InputStream.readBytes(): ByteArray {
    val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available()))
    val _ = copyTo(buffer)
    return buffer.toByteArray()
}
