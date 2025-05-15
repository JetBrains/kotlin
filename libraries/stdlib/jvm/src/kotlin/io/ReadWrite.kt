/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TextStreamsKt")

package kotlin.io

import java.io.*
import java.nio.charset.Charset
import java.net.URL
import java.util.NoSuchElementException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.*


/** Returns a buffered reader wrapping this Reader, or this Reader itself if it is already buffered. */
@kotlin.internal.InlineOnly
public inline fun Reader.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedReader =
    if (this is BufferedReader) this else BufferedReader(this, bufferSize)

/** Returns a buffered writer wrapping this Writer, or this Writer itself if it is already buffered. */
@kotlin.internal.InlineOnly
public inline fun Writer.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    if (this is BufferedWriter) this else BufferedWriter(this, bufferSize)

/**
 * Iterates through each line of this reader, calls [action] for each line read
 * and closes the [Reader] when it's completed.
 *
 * @param action function to process file lines.
 */
public fun Reader.forEachLine(action: (String) -> Unit): Unit = useLines { it.forEach(action) }

/**
 * Reads this reader content as a list of lines.
 *
 * Do not use this function for huge files.
 */
public fun Reader.readLines(): List<String> {
    val result = arrayListOf<String>()
    forEachLine { result.add(it) }
    return result
}

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.
 * @return the value returned by [block].
 */
public inline fun <T> Reader.useLines(block: (Sequence<String>) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return buffered().use { block(it.lineSequence()) }
}

/** Creates a new reader for the string. */
@kotlin.internal.InlineOnly
public inline fun String.reader(): StringReader = StringReader(this)

/**
 * Returns a sequence of corresponding file lines.
 *
 * *Note*: the caller must close the underlying `BufferedReader`
 * when the iteration is finished, as the user may not complete the iteration loop (e.g. using a method like find() or any() on the iterator
 * may terminate the iteration early).
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
    val _ = copyTo(buffer)
    return buffer.toString()
}

/**
 * Copies this reader to the given [out] writer, returning the number of characters copied.
 *
 * **Note** it is the caller's responsibility to close both of these resources.
 *
 * @param out writer to write to.
 * @param bufferSize size of character buffer to use in process.
 * @return number of characters copied.
 */
public fun Reader.copyTo(out: Writer, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
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
 * Reads the entire content of this URL as a String using UTF-8 or the specified [charset].
 *
 * This method is not recommended on huge files.
 *
 * @param charset a character set to use.
 * @return a string with this URL entire content.
 */
@kotlin.internal.InlineOnly
public inline fun URL.readText(charset: Charset = Charsets.UTF_8): String = readBytes().toString(charset)

/**
 * Reads the entire content of the URL as byte array.
 *
 * This method is not recommended on huge files.
 *
 * @return a byte array with this URL entire content.
 */
public fun URL.readBytes(): ByteArray = openStream().use { it.readBytes() }
