/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@file:kotlin.jvm.JvmName("UnsignedKt")
@file:UseExperimental(ExperimentalUnsignedTypes::class)
package kotlin

@PublishedApi
internal fun uintCompare(v1: Int, v2: Int): Int = (v1 xor Int.MIN_VALUE).compareTo(v2 xor Int.MIN_VALUE)
@PublishedApi
internal fun ulongCompare(v1: Long, v2: Long): Int = (v1 xor Long.MIN_VALUE).compareTo(v2 xor Long.MIN_VALUE)

@PublishedApi
internal fun uintDivide(v1: UInt, v2: UInt): UInt = (v1.toLong() / v2.toLong()).toUInt()
@PublishedApi
internal fun uintRemainder(v1: UInt, v2: UInt): UInt = (v1.toLong() % v2.toLong()).toUInt()

// Division and remainder are based on Guava's UnsignedLongs implementation
// Copyright 2011 The Guava Authors

@PublishedApi
internal fun ulongDivide(v1: ULong, v2: ULong): ULong {
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
internal fun ulongRemainder(v1: ULong, v2: ULong): ULong {
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
internal fun doubleToUByte(v: Double): UByte = when {
    v.isNaN() -> 0u
    v <= UByte.MIN_VALUE.toDouble() -> UByte.MIN_VALUE
    v >= UByte.MAX_VALUE.toDouble() -> UByte.MAX_VALUE
    else -> v.toInt().toUByte()
}

@PublishedApi
internal fun doubleToUShort(v: Double): UShort = when {
    v.isNaN() -> 0u
    v <= UShort.MIN_VALUE.toDouble() -> UShort.MIN_VALUE
    v >= UShort.MAX_VALUE.toDouble() -> UShort.MAX_VALUE
    else -> v.toInt().toUShort()
}

@PublishedApi
internal fun doubleToUInt(v: Double): UInt = when {
    v.isNaN() -> 0u
    v <= UInt.MIN_VALUE.toDouble() -> UInt.MIN_VALUE
    v >= UInt.MAX_VALUE.toDouble() -> UInt.MAX_VALUE
    v <= Int.MAX_VALUE -> v.toInt().toUInt()
    else -> (v - Int.MAX_VALUE).toInt().toUInt() + Int.MAX_VALUE.toUInt()      // Int.MAX_VALUE < v < UInt.MAX_VALUE
}

@PublishedApi
internal fun doubleToULong(v: Double): ULong = when {
    v.isNaN() -> 0u
    v <= ULong.MIN_VALUE.toDouble() -> ULong.MIN_VALUE
    v >= ULong.MAX_VALUE.toDouble() -> ULong.MAX_VALUE
    v < Long.MAX_VALUE -> v.toLong().toULong()

    // Real values from Long.MAX_VALUE to (Long.MAX_VALUE + 1) are not representable in Double, so don't handle them.
    else -> (v - 9223372036854775808.0).toLong().toULong() + 9223372036854775808uL      // Long.MAX_VALUE + 1 < v < ULong.MAX_VALUE
}


@PublishedApi
internal fun uintToDouble(v: Int): Double = (v and Int.MAX_VALUE).toDouble() + (v ushr 31 shl 30).toDouble() * 2

@PublishedApi
internal fun ulongToDouble(v: Long): Double = (v ushr 11).toDouble() * 2048 + (v and 2047)


internal fun ulongToString(v: Long): String = ulongToString(v, 10)

internal fun ulongToString(v: Long, base: Int): String {
    if (v >= 0) return v.toString(base)

    var quotient = ((v ushr 1) / base) shl 1
    var rem = v - quotient * base
    if (rem >= base) {
        rem -= base
        quotient += 1
    }
    return quotient.toString(base) + rem.toString(base)
}

