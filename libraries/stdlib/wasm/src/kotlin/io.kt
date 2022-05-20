/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.internal.*

@JsFun("(error) => console.error(error)")
internal external fun printError(error: String?): Unit

@JsFun("(message) => console.log(message)")
private external fun printlnImpl(message: String?): Unit

@JsFun("(message) => typeof write !== 'undefined' ? write(message) : console.log(message)")
private external fun printImpl(message: String?): Unit

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

@SinceKotlin("1.6")
public actual fun readln(): String = throw UnsupportedOperationException("readln is not supported in Kotlin/WASM")

@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = throw UnsupportedOperationException("readlnOrNull is not supported in Kotlin/WASM")

internal actual interface Serializable