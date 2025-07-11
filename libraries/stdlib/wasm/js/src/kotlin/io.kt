/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update

package kotlin.io

import kotlin.wasm.internal.*

internal actual fun printError(error: String?): Unit =
    js("console.error(error)")

private fun printlnImpl(message: String?): Unit =
    js("console.log(message)")

private fun printImpl(message: String?): Unit =
    js("typeof write !== 'undefined' ? write(message) : console.log(message)")

/** Prints the line separator to the standard output stream. */
public actual fun println() {
    printlnImpl("")
}

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    printlnImpl(message?.toString())
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    printImpl(message?.toString())
}

/**
 * This function is not supported in Kotlin/Wasm and throws [UnsupportedOperationException].
 */
@SinceKotlin("1.6")
public actual fun readln(): String = throw UnsupportedOperationException("readln is not supported in Kotlin/Wasm")

/**
 * This function is not supported in Kotlin/Wasm and throws [UnsupportedOperationException].
 */
@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = throw UnsupportedOperationException("readlnOrNull is not supported in Kotlin/Wasm")
