/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "RedundantVisibilityModifier", "RedundantUnitReturnType", "SameParameterValue")
@file:JvmMultifileClass
@file:JvmName("PathsKt")
@file:kotlin.jvm.JvmPackageName("kotlin.io.jdk7")

package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Returns a new [InputStreamReader] for reading the content of this file.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.reader(charset: Charset = Charsets.UTF_8, vararg options: OpenOption): InputStreamReader {
    return inputStream(*options).reader(charset)
}

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param charset character set to use.
 * @param bufferSize necessary size of the buffer.
 * @param options options to determine how the file is opened
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.bufferedReader(
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    vararg options: OpenOption
): BufferedReader {
    return reader(charset, *options).buffered(bufferSize)
}

/**
 * Returns a new [OutputStreamWriter] for writing the content of this file.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.writer(charset: Charset = Charsets.UTF_8, vararg options: OpenOption): OutputStreamWriter {
    return outputStream(*options).writer(charset)
}

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param charset character set to use.
 * @param bufferSize necessary size of the buffer.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.bufferedWriter(
    charset: Charset = Charsets.UTF_8,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    vararg options: OpenOption
): BufferedWriter {
    return writer(charset, *options).buffered(bufferSize)
}

/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.printWriter(charset: Charset = Charsets.UTF_8, vararg options: OpenOption): PrintWriter {
    return PrintWriter(bufferedWriter(charset, options = options))
}

/**
 * Gets the entire content of this file as a byte array.
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB byte array size.
 *
 * @return the entire content of this file as a byte array.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.readBytes(): ByteArray {
    return Files.readAllBytes(this)
}

/**
 * Write an [array] of bytes to this file.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param array byte array to write into this file.
 * @param options options to determine how the file is opened.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.writeBytes(array: ByteArray, vararg options: OpenOption): Unit {
    Files.write(this, array, *options)
}

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.appendBytes(array: ByteArray): Unit {
    writeBytes(array, StandardOpenOption.APPEND)
}

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 *
 * @param charset character set to use.
 * @return the entire content of this file as a String.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8, vararg options: OpenOption): Unit {
    writeBytes(text.toByteArray(charset), *options)
}

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8): Unit {
    writeText(text, charset, StandardOpenOption.APPEND)
}

/**
 * Reads this file line by line using the specified [charset] and calls [action] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param options options to determine how the file is opened.
 * @param charset character set to use.
 * @param action function to process file lines.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.forEachLine(charset: Charset = Charsets.UTF_8, vararg options: OpenOption, action: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    bufferedReader(charset, options = options).forEachLine(action)
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
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public fun Path.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Unit {
    // Note: close is called at forEachLine
    bufferedReader(charset = charset).forEachLine(action)
}

/**
 * Constructs a new InputStream of this file and returns it as a result.
 *
 * The [options] parameter determines how the file is opened. If no options are present then it is
 * equivalent to opening the file with the [READ][StandardOpenOption.READ] option.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
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
 * option.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.outputStream(vararg options: OpenOption): OutputStream {
    return Files.newOutputStream(this, *options)
}

/**
 * Reads the file content as a list of lines.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use. By default uses UTF-8 charset.
 * @return list of file lines.
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    return Files.readAllLines(this, charset)
}

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use. By default uses UTF-8 charset.
 * @return the value returned by [block].
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T {
    return bufferedReader(charset).use { block(it.lineSequence()) }
}
