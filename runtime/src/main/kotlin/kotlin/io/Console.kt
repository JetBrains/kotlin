/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.io

/** Prints the given [message] to the standard output stream. */
@SymbolName("Kotlin_io_Console_print")
external public fun print(message: String)

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    print(message.toString())
}

/** Prints the given [message] and newline to the standard output stream. */
@SymbolName("Kotlin_io_Console_println")
external public fun println(message: String)

/** Prints the given [message] and newline to the standard output stream. */
public actual fun println(message: Any?) {
    println(message.toString())
}

/** Prints newline to the standard output stream. */
@SymbolName("Kotlin_io_Console_println0")
external public actual fun println()

/**
 * Reads a line of input from the standard input stream.
 *
 * @return the line read or `null` if the input is empty.
 */
@SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): String?
