/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly

// CHANGES IN THIS FILE SHOULD BE SYNCED WITH THE SAME CHANGES IN: UnsignedJVM.kt and kotlin-native/Unsigned.kt
// Division and remainder are based on Guava's UnsignedLongs implementation
// Copyright 2011 The Guava Authors


@PublishedApi
internal actual fun uintRemainder(v1: UInt, v2: UInt): UInt = (v1.toLong() % v2.toLong()).toUInt()

@PublishedApi
internal actual fun uintDivide(v1: UInt, v2: UInt): UInt = (v1.toLong() / v2.toLong()).toUInt()

@PublishedApi
internal actual fun ulongDivide(v1: ULong, v2: ULong): ULong {
    val dividend = v1.toLong()
    val divisor = v2.toLong()
    if (divisor < 0) { // i.e., divisor >= 2^63:
        return if (v1 < v2) ULong(0) else ULong(1)
    }

    // Optimization - use signed division if both dividend and divisor < 2^63
    if (dividend >= 0) {
        return ULong(dividend / divisor)
    }

    // Otherwise, approximate the quotient, check, and correct if necessary.
    val quotient = ((dividend ushr 1) / divisor) shl 1
    val rem = dividend - quotient * divisor
    return ULong(quotient + if (ULong(rem) >= ULong(divisor)) 1 else 0)

}

@PublishedApi
internal actual fun ulongRemainder(v1: ULong, v2: ULong): ULong {
    val dividend = v1.toLong()
    val divisor = v2.toLong()
    if (divisor < 0) { // i.e., divisor >= 2^63:
        return if (v1 < v2) {
            v1 // dividend < divisor
        } else {
            v1 - v2 // dividend >= divisor
        }
    }

    // Optimization - use signed modulus if both dividend and divisor < 2^63
    if (dividend >= 0) {
        return ULong(dividend % divisor)
    }

    // Otherwise, approximate the quotient, check, and correct if necessary.
    val quotient = ((dividend ushr 1) / divisor) shl 1
    val rem = dividend - quotient * divisor
    return ULong(rem - if (ULong(rem) >= ULong(divisor)) divisor else 0)
}

@PublishedApi
internal actual fun uintCompare(v1: Int, v2: Int): Int = (v1 xor Int.MIN_VALUE).compareTo(v2 xor Int.MIN_VALUE)

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

@PublishedApi
internal actual fun uintToDouble(value: Int): Double = (value and Int.MAX_VALUE).toDouble() + (value ushr 31 shl 30).toDouble() * 2

@PublishedApi
internal actual fun doubleToUInt(value: Double): UInt = when {
    value.isNaN() -> 0u
    value <= UInt.MIN_VALUE.toDouble() -> UInt.MIN_VALUE
    value >= UInt.MAX_VALUE.toDouble() -> UInt.MAX_VALUE
    value <= Int.MAX_VALUE -> value.toInt().toUInt()
    else -> (value - Int.MAX_VALUE).toInt().toUInt() + Int.MAX_VALUE.toUInt()      // Int.MAX_VALUE < v < UInt.MAX_VALUE
}

@PublishedApi
@InlineOnly
internal actual inline fun ulongToFloat(value: Long): Float = ulongToDouble(value).toFloat()

@PublishedApi
@InlineOnly
internal actual inline fun floatToULong(value: Float): ULong = doubleToULong(value.toDouble())

@PublishedApi
internal actual fun ulongToDouble(value: Long): Double = (value ushr 11).toDouble() * 2048 + (value and 2047)

@PublishedApi
internal actual fun doubleToULong(value: Double): ULong = when {
    value.isNaN() -> 0u
    value <= ULong.MIN_VALUE.toDouble() -> ULong.MIN_VALUE
    value >= ULong.MAX_VALUE.toDouble() -> ULong.MAX_VALUE
    value < Long.MAX_VALUE -> value.toLong().toULong()

    // Real values from Long.MAX_VALUE to (Long.MAX_VALUE + 1) are not representable in Double, so don't handle them.
    else -> (value - 9223372036854775808.0).toLong().toULong() + 9223372036854775808uL      // Long.MAX_VALUE + 1 < v < ULong.MAX_VALUE
}

@InlineOnly
internal actual inline fun uintToString(value: Int): String = uintToLong(value).toString()

@InlineOnly
internal actual inline fun uintToString(value: Int, base: Int): String = ulongToString(uintToLong(value), base)

@InlineOnly
internal actual inline fun ulongToString(value: Long): String = ulongToString(value, 10)

internal actual fun ulongToString(value: Long, base: Int): String {
    if (value >= 0) return value.toString(base)

    var quotient = ((value ushr 1) / base) shl 1
    var rem = value - quotient * base
    if (rem >= base) {
        rem -= base
        quotient += 1
    }
    return quotient.toString(base) + rem.toString(base)
}
