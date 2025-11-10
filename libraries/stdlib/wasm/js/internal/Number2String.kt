/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin.wasm.internal

internal actual fun itoa32(inputValue: Int): String = js("String(inputValue)")

internal actual fun utoa32(inputValue: UInt): String = js("String(inputValue)")

internal actual fun itoa64(inputValue: Long): String = js("String(inputValue)")

internal actual fun utoa64(inputValue: ULong): String = js("String(inputValue)")
