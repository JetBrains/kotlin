/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin

import kotlin.wasm.internal.*

internal actual fun WasmCharArray.createString(): String {
    val size = this.len()
    return String(jsFromCharCodeArray(this, 0, size).unsafeCast())
}

internal actual fun String.getChars(): WasmCharArray {
    val copy = WasmCharArray(length)
    jsIntoCharCodeArray(internalStr, copy, 0)
    return copy
}
