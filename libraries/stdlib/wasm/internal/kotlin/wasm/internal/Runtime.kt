/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")  // File contains many intrinsics

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.BoxedBytesCache

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

@UsedFromCompilerGeneratedCode
internal fun nullableEquals(lhs: Any?, rhs: Any?): Boolean {
    if (wasm_ref_is_null(lhs))
        return wasm_ref_is_null(rhs)
    return unsafeNotNull(lhs).equals(rhs)
}

@UsedFromCompilerGeneratedCode
internal fun anyNtoString(x: Any?): String = x.toString()

@UsedFromCompilerGeneratedCode
internal fun nullableFloatIeee754Equals(lhs: Float?, rhs: Float?): Boolean {
    if (lhs == null) return rhs == null
    if (rhs == null) return false
    return wasm_f32_eq(lhs, rhs)
}

@UsedFromCompilerGeneratedCode
internal fun nullableDoubleIeee754Equals(lhs: Double?, rhs: Double?): Boolean {
    if (lhs == null) return rhs == null
    if (rhs == null) return false
    return wasm_f64_eq(lhs, rhs)
}

@UsedFromCompilerGeneratedCode
internal fun boxBoolean(x: Boolean): Boolean? =
    TODO("Remove after bootstrap")

//TODO: Remove after bootstrap
@UsedFromCompilerGeneratedCode
internal fun getBoxedBoolean(x: Boolean): Boolean? =
    if (x) {
        TRUE as Boolean? ?: boxBoolean(true).also { TRUE = it }
    } else {
        FALSE as Boolean? ?: boxBoolean(false).also { FALSE = it }
    }

//@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T> createBoxIntrinsic(x: T): T? =
    TODO("Make intrinsic after bootstap")

private var TRUE: Any? = null
private var FALSE: Any? = null

@UsedFromCompilerGeneratedCode
internal fun getOrBoxBoolean(x: Boolean): Any? =
    if (x) {
        TRUE ?: createBoxIntrinsic<Boolean>(true).also { TRUE = it }
    } else {
        FALSE ?: createBoxIntrinsic<Boolean>(false).also { FALSE = it }
    }

private var BoxedBytesCache: Array<Any?>? = null
@UsedFromCompilerGeneratedCode
internal fun getOrBoxByte(x: Byte): Any? {
    val index = x.toInt() + 128
    val cache = BoxedBytesCache ?: arrayOfNulls<Any?>(256).also { BoxedBytesCache = it }
    val cached: Any? = cache[index]
    if (cached !== null) return cached
    val boxed: Any? = createBoxIntrinsic<Byte>(x)
    cache[index] = boxed
    return boxed
}

private var BoxedShortsCache: Array<Any?>? = null
@UsedFromCompilerGeneratedCode
internal fun getOrBoxShort(x: Short): Any? {
    if (x < (-128).toShort() || x > 127.toShort()) return createBoxIntrinsic<Short>(x)
    val cache = BoxedShortsCache ?: arrayOfNulls<Any?>(256).also { BoxedShortsCache = it }
    val index = x.toInt() + 128
    val cached: Any? = cache[index]
    if (cached !== null) return cached
    val boxed: Any? = createBoxIntrinsic<Short>(x)
    cache[index] = boxed
    return boxed
}

private var BoxedIntsCache: Array<Any?>? = null
@UsedFromCompilerGeneratedCode
internal fun getOrBoxInt(x: Int): Any? {
    if (x < -128 || x > 127) return createBoxIntrinsic<Int>(x)
    val cache = BoxedIntsCache ?: arrayOfNulls<Any?>(256).also { BoxedIntsCache = it }
    val index = x + 128
    val cached: Any? = cache[index]
    if (cached !== null) return cached
    val boxed: Any? = createBoxIntrinsic<Int>(x)
    cache[index] = boxed
    return boxed
}

private var BoxedLongsCache: Array<Any?>? = null
@UsedFromCompilerGeneratedCode
internal fun getOrBoxLong(x: Long): Any? {
    if (x < -128L || x > 127L) return createBoxIntrinsic<Long>(x)
    val cache = BoxedLongsCache ?: arrayOfNulls<Any?>(256).also { BoxedLongsCache = it }
    val index = x.toInt() + 128
    val cached: Any? = cache[index]
    if (cached !== null) return cached
    val boxed: Any? = createBoxIntrinsic<Long>(x)
    cache[index] = boxed
    return boxed
}

private var BoxedCharsCache: Array<Any?>? = null
@UsedFromCompilerGeneratedCode
internal fun getOrBoxChar(x: Char): Any? {
    val index = x.code
    if (index > 127) return createBoxIntrinsic<Char>(x)
    val cache = BoxedCharsCache ?: arrayOfNulls<Any?>(128).also { BoxedCharsCache = it }
    val cached: Any? = cache[index]
    if (cached !== null) return cached
    val boxed: Any? = createBoxIntrinsic<Char>(x)
    cache[index] = boxed
    return boxed
}

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T, R> boxIntrinsic(x: T): R =
    implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T, R> unboxIntrinsic(x: T): R =
    implementedAsIntrinsic

// This intrinsic technically takes varargs, but we only introduce
// this in IR lowerings with arguments added in manually. The type of
// F is completely ignored and constrained by the backend to match a
// function type with argument types from the arguments of this
// instrinsic. The return type must be specified explicitly.
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <R> wasm_call_ref(f: Function<R>): R =
    implementedAsIntrinsic

// Represents absence of a value. Should never be used as a real object. See UnitToVoidLowering.kt for more info.
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal class Void private constructor()

// This is the only way to introduce Void type.
@UsedFromCompilerGeneratedCode
@WasmOp(WasmOp.DROP)
internal fun consumeAnyIntoVoid(a: Any?): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeBooleanIntoVoid(a: Boolean): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeByteIntoVoid(a: Byte): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeShortIntoVoid(a: Short): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeCharIntoVoid(a: Char): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeIntIntoVoid(a: Int): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeLongIntoVoid(a: Long): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeFloatIntoVoid(a: Float): Void =
    implementedAsIntrinsic

//TODO Remove after bootstrap
@WasmOp(WasmOp.DROP)
internal fun consumeDoubleIntoVoid(a: Double): Void =
    implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun getWasmAbiVersion(): Int =
    implementedAsIntrinsic

// Internal interface for producing Wasm branch hint annotations
@UsedFromCompilerGeneratedCode
internal fun likely(cond: Boolean): Boolean = cond
@UsedFromCompilerGeneratedCode
internal fun unlikely(cond: Boolean): Boolean = cond
