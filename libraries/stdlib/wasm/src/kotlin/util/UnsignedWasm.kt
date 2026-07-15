/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly
import kotlin.math.abs
import kotlin.wasm.internal.*
import kotlin.wasm.internal.WasmOp
import kotlin.wasm.internal.implementedAsIntrinsic
import kotlin.wasm.internal.wasm_u32_compareTo

@PublishedApi
@WasmOp(WasmOp.I32_REM_U)
internal fun uintRemainder(v1: UInt, v2: UInt): UInt = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I32_DIV_U)
internal fun uintDivide(v1: UInt, v2: UInt): UInt = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_REM_U)
internal fun ulongRemainder(v1: ULong, v2: ULong): ULong = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_DIV_U)
internal fun ulongDivide(v1: ULong, v2: ULong): ULong = implementedAsIntrinsic

@PublishedApi
@InlineOnly
internal fun uintCompare(v1: Int, v2: Int): Int = wasm_u32_compareTo(v1, v2)

@PublishedApi
@InlineOnly
internal fun ulongCompare(v1: Long, v2: Long): Int = wasm_u64_compareTo(v1, v2)

@PublishedApi
@WasmOp(WasmOp.I64_EXTEND_I32_U)
internal fun uintToULong(value: Int): ULong = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_EXTEND_I32_U)
internal fun uintToLong(value: Int): Long = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.F32_CONVERT_I32_U)
internal fun uintToFloat(value: Int): Float = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I32_TRUNC_SAT_F32_U)
internal fun floatToUInt(value: Float): UInt = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.F64_CONVERT_I32_U)
internal fun uintToDouble(value: Int): Double = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I32_TRUNC_SAT_F64_U)
internal fun doubleToUInt(value: Double): UInt = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.F32_CONVERT_I64_U)
internal fun ulongToFloat(value: Long): Float = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_TRUNC_SAT_F32_U)
internal fun floatToULong(value: Float): ULong = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.F64_CONVERT_I64_U)
internal fun ulongToDouble(value: Long): Double = implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_TRUNC_SAT_F64_U)
internal fun doubleToULong(value: Double): ULong = implementedAsIntrinsic

@InlineOnly
internal inline fun uintToString(value: Int): String = utoa32(value.toUInt())

internal fun uintToString(value: Int, base: Int): String {
    var unsignedValue = value.toUInt()

    if (base == 10) return unsignedValue.toString()
    if (value in 0 until base) return value.getChar().toString()

    val buffer = WasmCharArray(UInt.SIZE_BITS)

    val ulongRadix = base.toUInt()
    var currentBufferIndex = UInt.SIZE_BITS - 1

    while (unsignedValue != 0U) {
        buffer.set(currentBufferIndex, (unsignedValue % ulongRadix).toInt().getChar())
        unsignedValue /= ulongRadix
        currentBufferIndex--
    }

    return buffer.createStringStartingFrom(currentBufferIndex + 1)
}

@InlineOnly
internal inline fun ulongToString(value: Long): String = utoa64(value.toULong())

internal fun ulongToString(value: Long, base: Int): String {
    var unsignedValue = value.toULong()

    if (base == 10) return unsignedValue.toString()
    if (value in 0 until base) return value.toInt().getChar().toString()

    val buffer = WasmCharArray(ULong.SIZE_BITS)

    val ulongRadix = base.toULong()
    var currentBufferIndex = ULong.SIZE_BITS - 1

    while (unsignedValue != 0UL) {
        buffer.set(currentBufferIndex, (unsignedValue % ulongRadix).toInt().getChar())
        unsignedValue /= ulongRadix
        currentBufferIndex--
    }

    return buffer.createStringStartingFrom(currentBufferIndex + 1)
}

internal fun WasmCharArray.createStringStartingFrom(index: Int): String {
    if (index == 0) return createString()
    val newLength = this.len() - index
    if (newLength == 0) return ""
    val newChars = WasmCharArray(newLength)
    copyWasmArray(this, newChars, index, 0, newLength)
    return newChars.createString()
}

private fun Int.getChar() = if (this < 10) '0' + this else 'a' + (this - 10)
