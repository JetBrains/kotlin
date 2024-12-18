/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")  // File contains many intrinsics

package kotlin

import kotlin.wasm.internal.*

@Suppress("INVISIBLE_REFERENCE")
internal fun stringLiteral(poolId: Int, startAddress: Int, length: Int): String {
//    val cached = stringPool[poolId]
//    if (cached !== null) {
//        return cached
//    }

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = String(null, length, chars)
//    stringPool[poolId] = newString
    return newString
}