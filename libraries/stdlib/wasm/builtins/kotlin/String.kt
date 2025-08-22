/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

internal expect fun WasmCharArray.createString(start: Int, end: Int): String

internal expect fun String.getChars(): WasmCharArray

// TODO: remove after bootstrap
internal fun stringLiteral(poolId: Int, start: Int, length: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(start, length)
    val newString = chars.createString(0, length)
    stringPool[poolId] = newString
    return newString
}