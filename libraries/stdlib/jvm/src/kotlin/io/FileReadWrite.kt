/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("FilesKt")

package kotlin.io

import java.io.*
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.*
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil


/**
 * Returns a new [FileReader] for reading the content of this file.
 */
@kotlin.internal.InlineOnly
public inline fun File.reader(charset: Charset = Charsets.UTF_8): InputStreamReader =
    inputStream().reader(charset)

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
@kotlin.internal.InlineOnly
public inline fun File.bufferedReader(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedReader =
    reader(charset).buffered(bufferSize)

/**
 * Returns a new [FileWriter] for writing the content of this file.
 */
@kotlin.internal.InlineOnly
public inline fun File.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter =
    outputStream().writer(charset)

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
@kotlin.internal.InlineOnly
public inline fun File.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    writer(charset).buffered(bufferSize)

/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
@kotlin.internal.InlineOnly
public inline fun File.printWriter(charset: Charset = Charsets.UTF_8): PrintWriter =
    PrintWriter(bufferedWriter(charset))

/**
 * Gets the entire content of this file as a byte array.
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size.
 *
 * @return the entire content of this file as a byte array.
 */
public fun File.readBytes(): ByteArray = inputStream().use { input ->
    var offset = 0
    var remaining = this.length().also { length ->
        if (length > Int.MAX_VALUE) throw OutOfMemoryError("File $this is too big ($length bytes) to fit in memory.")
    }.toInt()
    val result = ByteArray(remaining)
    while (remaining > 0) {
        val read = input.read(result, offset, remaining)
        if (read < 0) break
        remaining -= read
        offset += read
    }
    if (remaining > 0) return@use result.copyOf(offset)

    val extraByte = input.read()
    if (extraByte == -1) return@use result

    // allocation estimate: (RS + DBS + max(ES, DBS + 1)) + (RS + ES),
    // where RS = result.size, ES = extra.size, DBS = DEFAULT_BUFFER_SIZE
    // when RS = 0, ES >> DBS   => DBS + DBS + 1 + ES + ES = 2DBS + 2ES
    // when RS >> ES, ES << DBS => RS + DBS + DBS+1 + RS + ES = 2RS + 2DBS + ES
    val extra = ExposingBufferByteArrayOutputStream(DEFAULT_BUFFER_SIZE + 1)
    extra.write(extraByte)
    val _ = input.copyTo(extra)

    val resultingSize = result.size + extra.size()
    if (resultingSize < 0) throw OutOfMemoryError("File $this is too big to fit in memory.")

    return@use extra.buffer.copyInto(
        destination = result.copyOf(resultingSize),
        destinationOffset = result.size,
        startIndex = 0, endIndex = extra.size()
    )
}

private class ExposingBufferByteArrayOutputStream(size: Int) : ByteArrayOutputStream(size) {
    val buffer: ByteArray get() = buf
}

/**
 * Sets the content of this file as an [array] of bytes.
 * If this file already exists, it becomes overwritten.
 *
 * @param array byte array to write into this file.
 */
public fun File.writeBytes(array: ByteArray): Unit = FileOutputStream(this).use { it.write(array) }

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
public fun File.appendBytes(array: ByteArray): Unit = FileOutputStream(this, true).use { it.write(array) }

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
public fun File.readText(charset: Charset = Charsets.UTF_8): String = reader(charset).use { it.readText() }

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8): Unit =
    FileOutputStream(this).use { it.writeTextImpl(text, charset) }

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
public fun File.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit =
    FileOutputStream(this, true).use { it.writeTextImpl(text, charset) }

internal fun OutputStream.writeTextImpl(text: String, charset: Charset) {
    val chunkSize = DEFAULT_BUFFER_SIZE

    if (text.length < 2 * chunkSize) {
        this.write(text.toByteArray(charset))
        return
    }

    val encoder = charset.newReplaceEncoder()
    val charBuffer = CharBuffer.allocate(chunkSize)
    val byteBuffer = byteBufferForEncoding(chunkSize, encoder)

    var startIndex = 0
    var leftover = 0

    while (startIndex < text.length) {
        val copyLength = minOf(chunkSize - leftover, text.length - startIndex)
        val endIndex = startIndex + copyLength

        text.toCharArray(charBuffer.array(), leftover, startIndex, endIndex)
        charBuffer.limit(copyLength + leftover)
        encoder.encode(charBuffer, byteBuffer, /*endOfInput = */endIndex == text.length).let { check(it.isUnderflow) }
        this.write(byteBuffer.array(), 0, byteBuffer.position())

        if (charBuffer.position() != charBuffer.limit()) {
            charBuffer.put(0, charBuffer.get()) // the last char is a high surrogate
            leftover = 1
        } else {
            leftover = 0
        }

        charBuffer.clear()
        byteBuffer.clear()
        startIndex = endIndex
    }
}

internal fun Charset.newReplaceEncoder() = newEncoder()
    .onMalformedInput(CodingErrorAction.REPLACE)
    .onUnmappableCharacter(CodingErrorAction.REPLACE)

internal fun byteBufferForEncoding(chunkSize: Int, encoder: CharsetEncoder): ByteBuffer {
    val maxBytesPerChar = ceil(encoder.maxBytesPerChar()).toInt() // including replacement sequence
    return ByteBuffer.allocate(chunkSize * maxBytesPerChar)
}

/**
 * Reads file by byte blocks and calls [action] for each block read.
 * Block has default size which is implementation-dependent.
 * This functions passes the byte array and amount of bytes in the array to the [action] function.
 *
 * You can use this function for huge files.
 *
 * @param action function to process file blocks.
 */
public fun File.forEachBlock(action: (buffer: ByteArray, bytesRead: Int) -> Unit): Unit = forEachBlock(DEFAULT_BLOCK_SIZE, action)

/**
 * Reads file by byte blocks and calls [action] for each block read.
 * This functions passes the byte array and amount of bytes in the array to the [action] function.
 *
 * You can use this function for huge files.
 *
 * @param action function to process file blocks.
 * @param blockSize size of a block, replaced by 512 if it's less, 4096 by default.
 */
public fun File.forEachBlock(blockSize: Int, action: (buffer: ByteArray, bytesRead: Int) -> Unit): Unit {
    val arr = ByteArray(blockSize.coerceAtLeast(MINIMUM_BLOCK_SIZE))

    inputStream().use { input ->
        do {
            val size = input.read(arr)
            if (size <= 0) {
                break
            } else {
                action(arr, size)
            }
        } while (true)
    }
}

/**
 * Reads this file line by line using the specified [charset] and calls [action] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use.
 * @param action function to process file lines.
 */
public fun File.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    BufferedReader(InputStreamReader(FileInputStream(this), charset)).forEachLine(action)
}

/**
 * Constructs a new FileInputStream of this file and returns it as a result.
 */
@kotlin.internal.InlineOnly
public inline fun File.inputStream(): FileInputStream {
    return FileInputStream(this)
}

/**
 * Constructs a new FileOutputStream of this file and returns it as a result.
 */
@kotlin.internal.InlineOnly
public inline fun File.outputStream(): FileOutputStream {
    return FileOutputStream(this)
}

/**
 * Reads the file content as a list of lines.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use. By default uses UTF-8 charset.
 * @return list of file lines.
 */
public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use. By default uses UTF-8 charset.
 * @return the value returned by [block].
 */
public inline fun <T> File.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return bufferedReader(charset).use { block(it.lineSequence()) }
}
