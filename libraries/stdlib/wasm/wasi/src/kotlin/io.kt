/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private const val STDOUT = 1
private const val STDERR = 2

@ExperimentalWasmInterop
@WasmImport("spectest", "print_i32")
private external fun spectestPrint_i32(value: Int): Unit

@ExperimentalWasmInterop
@WasmImport("spectest", "print")
private external fun spectestPrint(): Unit

internal actual fun printError(error: String?) {
    throw UnsupportedOperationException("printError not supported. Use println")
}

/** Prints the line separator to the standard output stream. */
@ExperimentalWasmInterop
public actual fun println() {
    spectestPrint()
}

/** Prints the given [message] and the line separator to the standard output stream. */
@ExperimentalWasmInterop
public actual fun println(message: Any?) {
    message?.toString()?.forEach { char ->
        spectestPrint_i32(char.code)
    }
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    throw UnsupportedOperationException("print not supported. Use println")
}

@SinceKotlin("1.6")
public actual fun readln(): String = TODO("wasi")

@SinceKotlin("1.6")
public actual fun readlnOrNull(): String? = TODO("wasi")
