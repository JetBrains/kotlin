/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin.wasm.internal

internal actual fun itoa32(inputValue: Int): String =
    js("Function.prototype.call.bind(Number.prototype.toString)(inputValue)")

internal actual fun utoa32(inputValue: UInt): String =
    js("Function.prototype.call.bind(Number.prototype.toString)(inputValue)")

internal actual fun itoa64(inputValue: Long): String =
    js("Function.prototype.call.bind(BigInt.prototype.toString)(inputValue)")

internal actual fun utoa64(inputValue: ULong): String =
    js("Function.prototype.call.bind(BigInt.prototype.toString)(inputValue)")
