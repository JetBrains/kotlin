/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

internal const val CHAR_SIZE_BYTES = 2
internal const val INT_SIZE_BYTES = 4

internal fun unsafeRawMemoryToChar(addr: Int) = wasm_i32_load16_u(addr).toChar()

internal fun unsafeRawMemoryToWasmCharArray(startAddr: Int, length: Int): WasmCharArray {
    val result = WasmCharArray(length)
    result.fill(length) { unsafeRawMemoryToChar(startAddr + it * CHAR_SIZE_BYTES) }
    return result
}

// Returns a pointer into a temporary scratch segment in the raw wasm memory. Aligned by 4.
// Note: currently there is single such segment for a whole wasm module, so use with care.
@ExcludedFromCodegen
internal fun unsafeGetScratchRawMemory(sizeBytes: Int): Int =
    implementedAsIntrinsic

// Assumes there is enough space at the destination, fails with wasm trap otherwise.
internal fun unsafeWasmCharArrayToRawMemory(src: WasmCharArray, dstAddr: Int) {
    var curAddr = dstAddr
    var i = 0
    while (i < src.len()) {
        wasm_i32_store16(curAddr, src.get(i))
        curAddr += CHAR_SIZE_BYTES
        i++
    }
}

@WasmNoOpCast
internal fun unsafeNotNull(x: Any?): Any =
    implementedAsIntrinsic

internal fun nullableEquals(lhs: Any?, rhs: Any?): Boolean {
    if (wasm_ref_is_null(lhs))
        return wasm_ref_is_null(rhs)
    return unsafeNotNull(lhs).equals(rhs)
}

internal fun anyNtoString(x: Any?): String = x.toString()

internal fun nullableFloatIeee754Equals(lhs: Float?, rhs: Float?): Boolean {
    if (lhs == null) return rhs == null
    if (rhs == null) return false
    return wasm_f32_eq(lhs, rhs)
}

internal fun nullableDoubleIeee754Equals(lhs: Double?, rhs: Double?): Boolean {
    if (lhs == null) return rhs == null
    if (rhs == null) return false
    return wasm_f64_eq(lhs, rhs)
}


internal fun <T : Any> ensureNotNull(v: T?): T = if (v == null) THROW_NPE() else v

@ExcludedFromCodegen
internal fun <T, R> boxIntrinsic(x: T): R =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun <T, R> unboxIntrinsic(x: T): R =
    implementedAsIntrinsic

// Represents absence of a value. Should never be used as a real object. See UnitToVoidLowering.kt for more info.
@ExcludedFromCodegen
internal class Void private constructor()

// This is the only way to introduce Void type.
@WasmOp(WasmOp.DROP)
internal fun consumeAnyIntoVoid(a: Any?): Void =
    implementedAsIntrinsic