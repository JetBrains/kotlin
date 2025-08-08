/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*
import kotlin.math.min

internal expect inline fun WasmCharArray.createString(): String

internal expect fun String.getChars(): WasmCharArray

internal fun stringLiteralUtf16(poolId: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val addressAndLength = stringAddressesAndLengths.get(poolId)
    val length = (addressAndLength shr 32).toInt()
    val startAddress = (addressAndLength and ((1L shl 32) - 1L)).toInt()

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = chars.createString()
    stringPool[poolId] = newString
    return newString
}

internal fun stringLiteralLatin1(poolId: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val addressAndLength = stringAddressesAndLengths.get(poolId)
    val length = (addressAndLength shr 32).toInt()
    val startAddress = (addressAndLength and ((1L shl 32) - 1L)).toInt()

    val bytes = array_new_data0<WasmByteArray>(startAddress, length)
    val chars = WasmCharArray(length)
    for (i in 0..<length) {
        val chr = bytes.getU(i).toByte().reinterpretAsInt().reinterpretAsChar()
        chars.set(i, chr)
    }

    val newString = chars.createString()
    stringPool[poolId] = newString
    return newString
}

// TODO: remove after bootstrap
internal fun stringLiteral(poolId: Int, start: Int, length: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(start, length)
    val newString = chars.createString()
    stringPool[poolId] = newString
    return newString
}