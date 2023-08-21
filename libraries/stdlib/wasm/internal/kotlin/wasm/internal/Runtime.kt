/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")  // File contains many intrinsics

package kotlin.wasm.internal

internal const val CHAR_SIZE_BYTES = 2

internal fun unsafeRawMemoryToWasmCharArray(srcAddr: Int, dstOffset: Int, dstLength: Int, dst: WasmCharArray) {
    var curAddr = srcAddr
    val srcAddrEndOffset = srcAddr + dstLength * CHAR_SIZE_BYTES
    var dstIndex = dstOffset
    while (curAddr < srcAddrEndOffset) {
        val char = wasm_i32_load16_u(curAddr).toChar()
        dst.set(dstIndex, char)
        curAddr += CHAR_SIZE_BYTES
        dstIndex++
    }
}

// Returns starting address of unused linear memory.
@ExcludedFromCodegen
@PublishedApi
internal fun unsafeGetScratchRawMemory(): Int =
    implementedAsIntrinsic

// Assumes there is enough space at the destination, fails with wasm trap otherwise.
internal fun unsafeWasmCharArrayToRawMemory(src: WasmCharArray, srcOffset: Int, srcLength: Int, dstAddr: Int) {
    var curAddr = dstAddr
    val srcEndOffset = srcOffset + srcLength
    var srcIndex = srcOffset
    while (srcIndex < srcEndOffset) {
        wasm_i32_store16(curAddr, src.get(srcIndex))
        curAddr += CHAR_SIZE_BYTES
        srcIndex++
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

@WasmOp(WasmOp.DROP)
internal fun consumeBooleanIntoVoid(a: Boolean): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeByteIntoVoid(a: Byte): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeShortIntoVoid(a: Short): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeCharIntoVoid(a: Char): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeIntIntoVoid(a: Int): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeLongIntoVoid(a: Long): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeFloatIntoVoid(a: Float): Void =
    implementedAsIntrinsic

@WasmOp(WasmOp.DROP)
internal fun consumeDoubleIntoVoid(a: Double): Void =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun stringGetPoolSize(): Int =
    implementedAsIntrinsic

// This initializer is a special case in FieldInitializersLowering
@Suppress("DEPRECATION")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
internal val stringPool: Array<String?> = Array(stringGetPoolSize())