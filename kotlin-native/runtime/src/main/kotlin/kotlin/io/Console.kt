/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.io

import kotlin.native.internal.GCUnsafeCall

/** Prints the given [message] to the standard output stream. */
@GCUnsafeCall("Kotlin_io_Console_print")
@PublishedApi
internal external fun print(message: String)

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    print(message.toString())
}

/** Prints the given [message] and the line separator to the standard output stream. */
@GCUnsafeCall("Kotlin_io_Console_println")
@PublishedApi
internal external fun println(message: String)

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    println(message.toString())
}

/** Prints the line separator to the standard output stream. */
@GCUnsafeCall("Kotlin_io_Console_println0")
public actual external fun println()

/**
 * Reads a line of input from the standard input stream and returns it,
 * or throws a [RuntimeException] if EOF has already been reached when [readln] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is interpreted as UTF-8. Invalid bytes are replaced by the replacement character '\uFFFD'.
 */
@SinceKotlin("1.6")
public actual fun readln(): String = readlnOrNull() ?: throw ReadAfterEOFException("EOF has already been reached")

/**
 * Reads a line of input from the standard input stream and returns it,
 * or return `null` if EOF has already been reached when [readlnOrNull] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * The input is interpreted as UTF-8. Invalid bytes are replaced by the replacement character '\uFFFD'.
 */
@SinceKotlin("1.6")
@GCUnsafeCall("Kotlin_io_Console_readlnOrNull")
public actual external fun readlnOrNull(): String?

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input stream is redirected to a file and the end of file has been reached.
 */
@GCUnsafeCall("Kotlin_io_Console_readLine")
public external fun readLine(): String?
