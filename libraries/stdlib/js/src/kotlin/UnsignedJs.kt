/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly
import kotlin.js.internal.boxedLong.BoxedLongApi

// CHANGES IN THIS FILE SHOULD BE SYNCED WITH THE SAME CHANGES IN: UnsignedJVM.kt and kotlin-native/Unsigned.kt

@InlineOnly
internal inline fun jsToInt32(x: Double): Int = js("x | 0")

@InlineOnly
private inline fun jsToString(x: Double, base: Int): String = js("x.toString(base)")

@PublishedApi
internal actual fun uintRemainder(v1: UInt, v2: UInt): UInt {
    val d1 = uintToDouble(v1.toInt()).unsafeCast<Int>()
    val d2 = uintToDouble(v2.toInt()).unsafeCast<Int>()
    return (d1 % d2).toUInt()
}

@PublishedApi
internal actual fun uintDivide(v1: UInt, v2: UInt): UInt {
    val d1 = uintToDouble(v1.toInt()).unsafeCast<Int>()
    val d2 = uintToDouble(v2.toInt()).unsafeCast<Int>()
    return (d1 / d2).toUInt()
}

@PublishedApi
internal actual fun uintCompare(v1: Int, v2: Int): Int = when {
    v1 == v2 -> 0
    uintToDouble(v1) < uintToDouble(v2) -> -1
    else -> 1
}

@PublishedApi
internal actual fun ulongCompare(v1: Long, v2: Long): Int = (v1 xor Long.MIN_VALUE).compareTo(v2 xor Long.MIN_VALUE)

@PublishedApi
@InlineOnly
internal actual inline fun uintToULong(value: Int): ULong = ULong(uintToLong(value))

@PublishedApi
@InlineOnly
internal actual inline fun uintToLong(value: Int): Long = value.toLong() and 0xFFFF_FFFF

@PublishedApi
@InlineOnly
internal actual inline fun uintToFloat(value: Int): Float = uintToDouble(value).toFloat()

@PublishedApi
@InlineOnly
internal actual inline fun floatToUInt(value: Float): UInt = doubleToUInt(value.toDouble())

// For JS engines, the operation `value >>> 0` is a no-op at run-time.
// It only changes the internal type information of the JIT to an unsigned 32-bit integer.
// That makes this operation, and subsequent operations on its result, very efficient.
@InlineOnly
@PublishedApi
internal actual inline fun uintToDouble(value: Int): Double = js("value >>> 0")

@PublishedApi
internal actual fun doubleToUInt(value: Double): UInt = when {
    value <= UInt.MIN_VALUE.toDouble() -> UInt.MIN_VALUE
    value >= UInt.MAX_VALUE.toDouble() -> UInt.MAX_VALUE
    else -> jsToInt32(value).toUInt()
}

@PublishedApi
@InlineOnly
internal actual inline fun ulongToFloat(value: Long): Float = ulongToDouble(value).toFloat()

@PublishedApi
@InlineOnly
internal actual inline fun floatToULong(value: Float): ULong = doubleToULong(value.toDouble())


@PublishedApi
internal actual fun doubleToULong(value: Double): ULong = when {
    value <= ULong.MIN_VALUE.toDouble() -> ULong.MIN_VALUE
    value >= ULong.MAX_VALUE.toDouble() -> ULong.MAX_VALUE
    else -> ulongFromUnsignedSafeDouble(value)
}

@InlineOnly
internal actual inline fun uintToString(value: Int): String = uintToDouble(value).toString()

@InlineOnly
internal actual inline fun uintToString(value: Int, base: Int): String =
    jsToString(uintToDouble(value), base)

@InlineOnly
internal actual inline fun ulongToString(value: Long): String = ulongToString(value, 10)

// The function call will be replaced by the compiler depends on the Long representation (bigint vs boxed)
internal fun ulongFromUnsignedSafeDouble(value: Double): ULong =
// TODO: Throw an exception after the bootstrap update
//    throw IllegalStateException("Should be replaced by compiler")
    @OptIn(BoxedLongApi::class)
    kotlin.js.internal.boxedLong.ulongFromUnsignedSafeDouble(value)

@PublishedApi
// The function call will be replaced by the compiler depends on the Long representation (bigint vs boxed)
internal actual fun ulongDivide(v1: ULong, v2: ULong): ULong =
// TODO(KT-88679): Throw an exception after the bootstrap update
//    throw IllegalStateException("Should be replaced by compiler")
    @OptIn(BoxedLongApi::class)
    kotlin.js.internal.boxedLong.ulongDivide(v1, v2)

@PublishedApi
// The function call will be replaced by the compiler depends on the Long representation (bigint vs boxed)
internal actual fun ulongRemainder(v1: ULong, v2: ULong): ULong =
// TODO(KT-88679): Throw an exception after the bootstrap update
//    throw IllegalStateException("Should be replaced by compiler")
    @OptIn(BoxedLongApi::class)
    kotlin.js.internal.boxedLong.ulongRemainder(v1, v2)

@PublishedApi
// The function call will be replaced by the compiler depends on the Long representation (bigint vs boxed)
internal actual fun ulongToDouble(value: Long): Double =
// TODO(KT-88679): Throw an exception after the bootstrap update
//    throw IllegalStateException("Should be replaced by compiler")
    @OptIn(BoxedLongApi::class)
    kotlin.js.internal.boxedLong.ulongToDouble(value)

@PublishedApi
// The function call will be replaced by the compiler depends on the Long representation (bigint vs boxed)
internal actual fun ulongToString(value: Long, base: Int): String =
// TODO(KT-88679): Throw an exception after the bootstrap update
//    throw IllegalStateException("Should be replaced by compiler")
    @OptIn(BoxedLongApi::class)
    kotlin.js.internal.boxedLong.ulongToString(value, base)

