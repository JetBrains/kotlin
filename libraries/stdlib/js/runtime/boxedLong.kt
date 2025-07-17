/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright 2009 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

@file:Suppress("PackageDirectoryMismatch")

package kotlin.js.internal.boxedLong

import kotlin.internal.UsedFromCompilerGeneratedCode

/**
 * Marks the stdlib functions that implement the pre-BigInt Long boxing or rely on [Long] being implemented as a regular class
 * with two [Int] fields.
 *
 * If you use a function annotated with this annotation, you assume that [Long] is implemented as a regular class with
 * two [Int] fields. Don't do it unless you are sure that you also handle the BigInt-backed Long values.
 *
 * These declarations will need to be removed when we drop the ES5 target (KT-70480).
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
internal annotation class BoxedLongApi

/**
 * @see kotlin.js.internal.longAsBigInt.toNumber
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.toNumber() = high * TWO_PWR_32_DBL_ + getLowBitsUnsigned()

/**
 * @see kotlin.js.internal.longAsBigInt.convertToByte
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.convertToByte(): Byte = low.toByte()

/**
 * @see kotlin.js.internal.longAsBigInt.convertToChar
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.convertToChar(): Char = low.toChar()

/**
 * @see kotlin.js.internal.longAsBigInt.convertToShort
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.convertToShort(): Short = low.toShort()

/**
 * @see kotlin.js.internal.longAsBigInt.convertToInt
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.convertToInt(): Int = low

@BoxedLongApi
private fun Long.getLowBitsUnsigned() = if (low >= 0) low.toDouble() else TWO_PWR_32_DBL_ + low

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun hashCode(l: Long) = l.low xor l.high

/**
 * @see kotlin.js.internal.longAsBigInt.toStringImpl
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.toStringImpl(radix: Int): String {
    if (isZero()) {
        return "0"
    }

    if (isNegative()) {
        if (equalsLong(MIN_VALUE)) {
            // We need to change the Long value before it can be negated, so we remove
            // the bottom-most digit in this base and then recurse to do the rest.
            val radixLong = fromInt(radix)
            val div = div(radixLong)
            val rem = div.multiply(radixLong).subtract(this).toInt()
            // Using rem.asDynamic() to break dependency on "kotlin.text" package
            return div.toStringImpl(radix) + rem.asDynamic().toString(radix).unsafeCast<String>()
        } else {
            return "-${negate().toStringImpl(radix)}"
        }
    }

    // Do several digits each time through the loop, so as to
    // minimize the calls to the very expensive emulated div.
    val digitsPerTime = when {
        radix == 2 -> 31
        radix <= 10 -> 9
        radix <= 21 -> 7
        radix <= 35 -> 6
        else -> 5
    }
    val radixToPower = fromNumber(JsMath.pow(radix.toDouble(), digitsPerTime.toDouble()))

    var rem = this
    var result = ""
    while (true) {
        val remDiv = rem.div(radixToPower)
        val intval = rem.subtract(remDiv.multiply(radixToPower)).toInt()
        var digits = intval.asDynamic().toString(radix).unsafeCast<String>()

        rem = remDiv
        if (rem.isZero()) {
            return digits + result
        } else {
            while (digits.length < digitsPerTime) {
                digits = "0" + digits
            }
            result = digits + result
        }
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.negate
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.negate(): Long = invert() + 1L

@BoxedLongApi
private fun Long.isZero() = high == 0 && low == 0

@BoxedLongApi
private fun Long.isNegative() = high < 0

@BoxedLongApi
private fun Long.isOdd() = low and 1 == 1

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.equalsLong(other: Long) = high == other.high && low == other.low

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.lessThan(other: Long) = compare(other) < 0

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.greaterThan(other: Long) = compare(other) > 0

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.greaterThanOrEqual(other: Long) = compare(other) >= 0

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.compare(other: Long): Int {
    if (equalsLong(other)) {
        return 0;
    }

    val thisNeg = isNegative();
    val otherNeg = other.isNegative();

    return when {
        thisNeg && !otherNeg -> -1
        !thisNeg && otherNeg -> 1
    // at this point, the signs are the same, so subtraction will not overflow
        subtract(other).isNegative() -> -1
        else -> 1
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.add
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.add(other: Long): Long {
    // Divide each number into 4 chunks of 16 bits, and then sum the chunks.

    val a48 = high ushr 16
    val a32 = high and 0xFFFF
    val a16 = low ushr 16
    val a00 = low and 0xFFFF

    val b48 = other.high ushr 16
    val b32 = other.high and 0xFFFF
    val b16 = other.low ushr 16
    val b00 = other.low and 0xFFFF

    var c48 = 0
    var c32 = 0
    var c16 = 0
    var c00 = 0
    c00 += a00 + b00
    c16 += c00 ushr 16
    c00 = c00 and 0xFFFF
    c16 += a16 + b16
    c32 += c16 ushr 16
    c16 = c16 and 0xFFFF
    c32 += a32 + b32
    c48 += c32 ushr 16
    c32 = c32 and 0xFFFF
    c48 += a48 + b48
    c48 = c48 and 0xFFFF
    return Long((c16 shl 16) or c00, (c48 shl 16) or c32)
}

/**
 * @see kotlin.js.internal.longAsBigInt.subtract
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.subtract(other: Long) = add(other.unaryMinus())

/**
 * @see kotlin.js.internal.longAsBigInt.multiply
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.multiply(other: Long): Long {
    if (isZero()) {
        return ZERO
    } else if (other.isZero()) {
        return ZERO
    }

    if (equalsLong(MIN_VALUE)) {
        return if (other.isOdd()) MIN_VALUE else ZERO
    } else if (other.equalsLong(MIN_VALUE)) {
        return if (isOdd()) MIN_VALUE else ZERO
    }

    if (isNegative()) {
        return if (other.isNegative()) {
            negate().multiply(other.negate())
        } else {
            negate().multiply(other).negate()
        }
    } else if (other.isNegative()) {
        return multiply(other.negate()).negate()
    }

    // If both longs are small, use float multiplication
    if (lessThan(TWO_PWR_24_) && other.lessThan(TWO_PWR_24_)) {
        return fromNumber(toNumber() * other.toNumber())
    }

    // Divide each long into 4 chunks of 16 bits, and then add up 4x4 products.
    // We can skip products that would overflow.

    val a48 = high ushr 16
    val a32 = high and 0xFFFF
    val a16 = low ushr 16
    val a00 = low and 0xFFFF

    val b48 = other.high ushr 16
    val b32 = other.high and 0xFFFF
    val b16 = other.low ushr 16
    val b00 = other.low and 0xFFFF

    var c48 = 0
    var c32 = 0
    var c16 = 0
    var c00 = 0
    c00 += a00 * b00
    c16 += c00 ushr 16
    c00 = c00 and 0xFFFF
    c16 += a16 * b00
    c32 += c16 ushr 16
    c16 = c16 and 0xFFFF
    c16 += a00 * b16
    c32 += c16 ushr 16
    c16 = c16 and 0xFFFF
    c32 += a32 * b00
    c48 += c32 ushr 16
    c32 = c32 and 0xFFFF
    c32 += a16 * b16
    c48 += c32 ushr 16
    c32 = c32 and 0xFFFF
    c32 += a00 * b32
    c48 += c32 ushr 16
    c32 = c32 and 0xFFFF
    c48 += a48 * b00 + a32 * b16 + a16 * b32 + a00 * b48
    c48 = c48 and 0xFFFF
    return Long(c16 shl 16 or c00, c48 shl 16 or c32)
}

/**
 * @see kotlin.js.internal.longAsBigInt.divide
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.divide(other: Long): Long {
    if (other.isZero()) {
        throw Exception("division by zero")
    } else if (isZero()) {
        return ZERO
    }

    if (equalsLong(MIN_VALUE)) {
        if (other.equalsLong(ONE) || other.equalsLong(NEG_ONE)) {
            return MIN_VALUE  // recall that -MIN_VALUE == MIN_VALUE
        } else if (other.equalsLong(MIN_VALUE)) {
            return ONE
        } else {
            // At this point, we have |other| >= 2, so |this/other| < |MIN_VALUE|.
            val halfThis = shiftRight(1)
            val approx = halfThis.div(other).shiftLeft(1)
            if (approx.equalsLong(ZERO)) {
                return if (other.isNegative()) ONE else NEG_ONE
            } else {
                val rem = subtract(other.multiply(approx))
                return approx.add(rem.div(other))
            }
        }
    } else if (other.equalsLong(MIN_VALUE)) {
        return ZERO
    }

    if (isNegative()) {
        return if (other.isNegative()) {
            negate().div(other.negate())
        } else {
            negate().div(other).negate()
        }
    } else if (other.isNegative()) {
        return div(other.negate()).negate()
    }

    // Repeat the following until the remainder is less than other:  find a
    // floating-point that approximates remainder / other *from below*, add this
    // into the result, and subtract it from the remainder.  It is critical that
    // the approximate value is less than or equal to the real value so that the
    // remainder never becomes negative.
    var res = ZERO
    var rem = this
    while (rem.greaterThanOrEqual(other)) {
        // Approximate the result of division. This may be a little greater or
        // smaller than the actual value.
        val approxDouble = rem.toNumber() / other.toNumber()
        var approx2 = JsMath.max(1.0, JsMath.floor(approxDouble))

        // We will tweak the approximate result by changing it in the 48-th digit or
        // the smallest non-fractional digit, whichever is larger.
        val log2 = JsMath.ceil(JsMath.log(approx2) / JsMath.LN2)
        val delta = if (log2 <= 48) 1.0 else JsMath.pow(2.0, log2 - 48)

        // Decrease the approximation until it is smaller than the remainder.  Note
        // that if it is too large, the product overflows and is negative.
        var approxRes = fromNumber(approx2)
        var approxRem = approxRes.multiply(other)
        while (approxRem.isNegative() || approxRem.greaterThan(rem)) {
            approx2 -= delta
            approxRes = fromNumber(approx2)
            approxRem = approxRes.multiply(other)
        }

        // We know the answer can't be zero... and actually, zero would cause
        // infinite recursion since we would make no progress.
        if (approxRes.isZero()) {
            approxRes = ONE
        }

        res = res.add(approxRes)
        rem = rem.subtract(approxRem)
    }
    return res
}

/**
 * @see kotlin.js.internal.longAsBigInt.modulo
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.modulo(other: Long) = subtract(div(other).multiply(other))

/**
 * @see kotlin.js.internal.longAsBigInt.shiftLeft
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftLeft(numBits: Int): Long {
    @Suppress("NAME_SHADOWING")
    val numBits = numBits and 63
    if (numBits == 0) {
        return this
    } else {
        if (numBits < 32) {
            return Long(low shl numBits, (high shl numBits) or (low ushr (32 - numBits)))
        } else {
            return Long(0, low shl (numBits - 32))
        }
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.shiftRight
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftRight(numBits: Int): Long {
    @Suppress("NAME_SHADOWING")
    val numBits = numBits and 63
    if (numBits == 0) {
        return this
    } else {
        if (numBits < 32) {
            return Long((low ushr numBits) or (high shl (32 - numBits)), high shr numBits)
        } else {
            return Long(high shr (numBits - 32), if (high >= 0) 0 else -1)
        }
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.shiftRightUnsigned
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftRightUnsigned(numBits: Int): Long {
    @Suppress("NAME_SHADOWING")
    val numBits = numBits and 63
    if (numBits == 0) {
        return this
    } else {
        if (numBits < 32) {
            return Long((low ushr numBits) or (high shl (32 - numBits)), high ushr numBits)
        } else return if (numBits == 32) {
            Long(high, 0)
        } else {
            Long(high ushr (numBits - 32), 0)
        }
    }
}

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.bitwiseAnd(other: Long) = Long(this.low and other.low, this.high and other.high)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.bitwiseOr(other: Long) = Long(this.low or other.low, this.high or other.high)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.bitwiseXor(other: Long) = Long(this.low xor other.low, this.high xor other.high)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.invert() = Long(this.low.inv(), this.high.inv())

/**
 * Returns a Long representing the given (32-bit) integer value.
 * @param value The 32-bit integer in question.
 * @return The corresponding Long value.
 *
 * @see kotlin.js.internal.longAsBigInt.fromInt
 */
// TODO: cache
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun fromInt(value: dynamic) = Long(value, if (value < 0) -1 else 0)

/**
 * @see kotlin.js.internal.longAsBigInt.numberToLong
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun numberToLong(a: dynamic): Long = if (a is Long) a else fromNumber(a)

/**
 * Converts this [Double] value to [Long].
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
 * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
 *
 * @see kotlin.js.internal.longAsBigInt.fromNumber
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun fromNumber(value: Double): Long {
    if (value.isNaN()) {
        return ZERO;
    } else if (value <= -TWO_PWR_63_DBL_) {
        return MIN_VALUE;
    } else if (value + 1 >= TWO_PWR_63_DBL_) {
        return MAX_VALUE;
    } else if (value < 0) {
        return fromNumber(-value).negate();
    } else {
        val twoPwr32 = TWO_PWR_32_DBL_
        return Long(
            jsBitwiseOr(value.rem(twoPwr32), 0),
            jsBitwiseOr(value / twoPwr32, 0)
        )
    }
}

//private val TWO_PWR_32_DBL_ = TWO_PWR_16_DBL_ * TWO_PWR_16_DBL_
private const val TWO_PWR_32_DBL_ = (1 shl 16).toDouble() * (1 shl 16).toDouble()

//private val TWO_PWR_63_DBL_ = TWO_PWR_64_DBL_ / 2
private const val TWO_PWR_63_DBL_ = (((1 shl 16).toDouble() * (1 shl 16).toDouble()) * ((1 shl 16).toDouble() * (1 shl 16).toDouble())) / 2

@BoxedLongApi
private val ZERO = fromInt(0)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal val ONE = fromInt(1)

@BoxedLongApi
private val NEG_ONE = fromInt(-1)

@BoxedLongApi
private val MAX_VALUE = Long(-1, -1 ushr 1)

@BoxedLongApi
private val MIN_VALUE = Long(0, 1 shl 31)

@BoxedLongApi
private val TWO_PWR_24_ = fromInt(1 shl 24)
