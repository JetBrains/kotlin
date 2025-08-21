/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io


/** Prints the line separator to the standard output stream. */
public expect fun println()

/** Prints the given [message] and the line separator to the standard output stream. */
public expect fun println(message: Any?)

/** Prints the given [message] to the standard output stream. */
public expect fun print(message: Any?)

/**
 * Reads a line of input from the standard input stream and returns it,
 * or throws a [RuntimeException] if EOF has already been reached when [readln] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * Currently this function is not supported in Kotlin/JS and in Kotlin/Wasm, and throws [UnsupportedOperationException].
 */
@SinceKotlin("1.6")
public expect fun readln(): String

/**
 * Reads a line of input from the standard input stream and returns it,
 * or return `null` if EOF has already been reached when [readlnOrNull] is called.
 *
 * LF or CRLF is treated as the line terminator. Line terminator is not included in the returned string.
 *
 * Currently this function is not supported in Kotlin/JS and in Kotlin/Wasm, and throws [UnsupportedOperationException].
 */
@SinceKotlin("1.6")
public expect fun readlnOrNull(): String?

internal class ReadAfterEOFException(message: String?) : RuntimeException(message)


internal expect interface Serializable
