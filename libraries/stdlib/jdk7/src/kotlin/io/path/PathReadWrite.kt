/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "RedundantVisibilityModifier", "RedundantUnitReturnType", "SameParameterValue")
@file:JvmMultifileClass
@file:JvmName("PathsKt")

package kotlin.io.path

import java.io.*
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.contracts.*
import kotlin.jvm.Throws

/**
 * Returns a new [InputStreamReader] for reading the content of this file.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.reader(charset: Charset = Charsets.UTF_8, vararg options: OpenOption): InputStreamReader {
    return InputStreamReader(Files.newInputStream(this, *options), charset)
}

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @param bufferSize necessary size of the buffer.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.bufferedReader(
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    vararg options: OpenOption
): BufferedReader {
    return BufferedReader(
        InputStreamReader(
            Files.newInputStream(this, *options),
            charset
        ),
        bufferSize)
}

/**
 * Returns a new [OutputStreamWriter] for writing the content of this file.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.writer(charset: Charset = Charsets.UTF_8, vararg options: OpenOption): OutputStreamWriter {
    return OutputStreamWriter(Files.newOutputStream(this, *options), charset)
}

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param bufferSize necessary size of the buffer.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.bufferedWriter(
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    vararg options: OpenOption
): BufferedWriter {
    return BufferedWriter(
        OutputStreamWriter(
            Files.newOutputStream(this, *options),
            charset),
        bufferSize)
}

/**
 * Gets the entire content of this file as a byte array.
 *
 * It's not recommended to use this function on huge files.
 * It has an internal limitation of approximately 2 GB byte array size.
 * For reading large files or files of unknown size, open an [InputStream][Path.inputStream] and read blocks sequentially.
 *
 * @return the entire content of this file as a byte array.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.readBytes(): ByteArray {
    return Files.readAllBytes(this)
}

/**
 * Writes an [array] of bytes to this file.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param array byte array to write into this file.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.writeBytes(array: ByteArray, vararg options: OpenOption): Unit {
    Files.write(this, array, *options)
}

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.appendBytes(array: ByteArray) {
    Files.write(this, array, StandardOpenOption.APPEND)
}

/**
 * Gets the entire content of this file as a String using UTF-8 or the specified [charset].
 *
 * It's not recommended to use this function on huge files.
 * For reading large files or files of unknown size, open a [Reader][Path.reader] and read blocks of text sequentially.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @return the entire content of this file as a String.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
public fun Path.readText(charset: Charset = Charsets.UTF_8): String =
    reader(charset).use { it.readText() }

/**
 * Sets the content of this file as [text] encoded using UTF-8 or the specified [charset].
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param text text to write into file.
 * @param charset character set to use for writing text, UTF-8 by default.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
public fun Path.writeText(text: CharSequence, charset: Charset = Charsets.UTF_8, vararg options: OpenOption) {
    Files.newOutputStream(this, *options).use { out ->
        if (text is String) {
            out.writeTextImpl(text, charset)
            return@use
        }

        val encoder = charset.newReplaceEncoder()
        val charBuffer = if (text is CharBuffer) text.asReadOnlyBuffer() else CharBuffer.wrap(text)
        val byteBuffer = byteBufferForEncoding(chunkSize = minOf(text.length, DEFAULT_BUFFER_SIZE), encoder)

        while (charBuffer.hasRemaining()) {
            encoder.encode(charBuffer, byteBuffer, /*endOfInput = */true).let { check(!it.isError) }
            out.write(byteBuffer.array(), 0, byteBuffer.position())
            byteBuffer.clear()
        }
    }
}

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use for writing text, UTF-8 by default.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
public fun Path.appendText(text: CharSequence, charset: Charset = Charsets.UTF_8) {
    writeText(text, charset, StandardOpenOption.APPEND)
}

/**
 * Reads this file line by line using the specified [charset] and calls [action] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @param action function to process file lines.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
    // cannot use non-inline forEachLine
    Files.newBufferedReader(this, charset).useLines { it.forEach(action) }
}

/**
 * Constructs a new InputStream of this file and returns it as a result.
 *
 * The [options] parameter determines how the file is opened. If no options are present then it is
 * equivalent to opening the file with the [READ][StandardOpenOption.READ] option.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.inputStream(vararg options: OpenOption): InputStream {
    return Files.newInputStream(this, *options)
}

/**
 * Constructs a new OutputStream of this file and returns it as a result.
 *
 * The [options] parameter determines how the file is opened. If no options are present then it is
 * equivalent to opening the file with the [CREATE][StandardOpenOption.CREATE],
 * [TRUNCATE_EXISTING][StandardOpenOption.TRUNCATE_EXISTING], and [WRITE][StandardOpenOption.WRITE]
 * options.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.outputStream(vararg options: OpenOption): OutputStream {
    return Files.newOutputStream(this, *options)
}

/**
 * Reads the file content as a list of lines.
 *
 * It's not recommended to use this function on huge files.
 * For reading lines of a large file or a file of unknown size, use [Path.forEachLine] or [Path.useLines].
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @return list of file lines.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    return Files.readAllLines(this, charset)
}

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use for reading text, UTF-8 by default.
 * @return the value returned by [block].
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Files.newBufferedReader(this, charset).use { block(it.lineSequence()) }
}

/**
 * Write the specified collection of char sequences [lines] to a file terminating each one with the platform's line separator.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.writeLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8, vararg options: OpenOption): Path {
    return Files.write(this, lines, charset, *options)
}

/**
 * Write the specified sequence of char sequences [lines] to a file terminating each one with the platform's line separator.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.writeLines(lines: Sequence<CharSequence>, charset: Charset = Charsets.UTF_8, vararg options: OpenOption): Path {
    return Files.write(this, lines.asIterable(), charset, *options)
}

/**
 * Appends the specified collection of char sequences [lines] to a file terminating each one with the platform's line separator.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.appendLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8): Path {
    return Files.write(this, lines, charset, StandardOpenOption.APPEND)
}

/**
 * Appends the specified sequence of char sequences [lines] to a file terminating each one with the platform's line separator.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
@SinceKotlin("1.5")
@Throws(IOException::class)
@kotlin.internal.InlineOnly
public inline fun Path.appendLines(lines: Sequence<CharSequence>, charset: Charset = Charsets.UTF_8): Path {
    return Files.write(this, lines.asIterable(), charset, StandardOpenOption.APPEND)
}
