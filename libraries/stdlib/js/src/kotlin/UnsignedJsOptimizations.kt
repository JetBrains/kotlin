/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly

@InlineOnly
private inline fun jsToInt32(x: Double): Int = js("x | 0")

@InlineOnly
private inline fun jsToString(x: Double, base: Int): String = js("x.toString(base)")

@PublishedApi
internal fun jsUintRemainder(v1: UInt, v2: UInt): UInt =
    jsToInt32(uintToDouble(v1.toInt()) % uintToDouble(v2.toInt())).toUInt()

@PublishedApi
internal fun jsUintDivide(v1: UInt, v2: UInt): UInt =
    jsToInt32(uintToDouble(v1.toInt()) / uintToDouble(v2.toInt())).toUInt()


@PublishedApi
internal fun jsUintCompare(v1: Int, v2: Int): Int = when {
    v1 == v2 -> 0
    uintToDouble(v1) < uintToDouble(v2) -> -1
    else -> 1
}

// For JS engines, the operation `value >>> 0` is a no-op at run-time.
// It only changes the internal type information of the JIT to an unsigned 32-bit integer.
// That makes this operation, and subsequent operations on its result, very efficient.
@InlineOnly
@PublishedApi
internal inline fun jsUintToDouble(value: Int): Double = js("value >>> 0")

@PublishedApi
internal fun jsDoubleToUInt(value: Double): UInt = when {
    value <= UInt.MIN_VALUE.toDouble() -> UInt.MIN_VALUE
    value >= UInt.MAX_VALUE.toDouble() -> UInt.MAX_VALUE
    else -> jsToInt32(value).toUInt()
}

@InlineOnly
internal inline fun jsUintToString(value: Int): String = uintToDouble(value).toString()

@InlineOnly
internal inline fun jsUintToString(value: Int, base: Int): String =
    jsToString(uintToDouble(value), base)
