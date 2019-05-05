/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package kotlin

internal fun Long.toNumber() = high * TWO_PWR_32_DBL_ + getLowBitsUnsigned()

internal fun Long.getLowBitsUnsigned() = if (low >= 0) low.toDouble() else TWO_PWR_32_DBL_ + low

internal fun hashCode(l: Long) = l.low xor l.high

internal fun Long.toStringImpl(radix: Int): String {
    if (radix < 2 || 36 < radix) {
        throw Exception("radix out of range: $radix")
    }

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

    // Do several (6) digits each time through the loop, so as to
    // minimize the calls to the very expensive emulated div.
    val radixToPower = fromNumber(JsMath.pow(radix.toDouble(), 6.0))

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
            while (digits.length < 6) {
                digits = "0" + digits
            }
            result = digits + result
        }
    }
}

internal fun Long.negate() = unaryMinus()

internal fun Long.isZero() = high == 0 && low == 0

internal fun Long.isNegative() = high < 0

internal fun Long.isOdd() = low and 1 == 1

internal fun Long.equalsLong(other: Long) = high == other.high && low == other.low

internal fun Long.lessThan(other: Long) = compare(other) < 0

internal fun Long.lessThanOrEqual(other: Long) = compare(other) <= 0

internal fun Long.greaterThan(other: Long) = compare(other) > 0

internal fun Long.greaterThanOrEqual(other: Long) = compare(other) >= 0

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

internal fun Long.subtract(other: Long) = add(other.unaryMinus())

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
        val delta = if (log2 <= 48) 1.0 else JsMath.pow(2, log2 - 48)

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

internal fun Long.modulo(other: Long) = subtract(div(other).multiply(other))

internal fun Long.shiftLeft(numBits: Int): Long {
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

internal fun Long.shiftRight(numBits: Int): Long {
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

internal fun Long.shiftRightUnsigned(numBits: Int): Long {
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

/**
 * Returns a Long representing the given (32-bit) integer value.
 * @param {number} value The 32-bit integer in question.
 * @return {!Kotlin.Long} The corresponding Long value.
 */
// TODO: cache
internal fun fromInt(value: Int) = Long(value, if (value < 0) -1 else 0)

/**
 * Returns a Long representing the given value, provided that it is a finite
 * number.  Otherwise, zero is returned.
 * @param {number} value The number in question.
 * @return {!Kotlin.Long} The corresponding Long value.
 */
internal fun fromNumber(value: Double): Long {
    if (value.isNaN() || !value.isFinite()) {
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

private val TWO_PWR_16_DBL_ = (1 shl 16).toDouble()

private val TWO_PWR_24_DBL_ = (1 shl 24).toDouble()

//private val TWO_PWR_32_DBL_ = TWO_PWR_16_DBL_ * TWO_PWR_16_DBL_
private val TWO_PWR_32_DBL_ = (1 shl 16).toDouble() * (1 shl 16).toDouble()

//private val TWO_PWR_64_DBL_ = TWO_PWR_32_DBL_ * TWO_PWR_32_DBL_
private val TWO_PWR_64_DBL_ = ((1 shl 16).toDouble() * (1 shl 16).toDouble()) * ((1 shl 16).toDouble() * (1 shl 16).toDouble())

//private val TWO_PWR_63_DBL_ = TWO_PWR_64_DBL_ / 2
private val TWO_PWR_63_DBL_ = (((1 shl 16).toDouble() * (1 shl 16).toDouble()) * ((1 shl 16).toDouble() * (1 shl 16).toDouble())) / 2

private val ZERO = fromInt(0)

private val ONE = fromInt(1)

private val NEG_ONE = fromInt(-1)

private val MAX_VALUE = Long(-1, -1 ushr 1)

private val MIN_VALUE = Long(0, 1 shl 31)

private val TWO_PWR_24_ = fromInt(1 shl 24)

@JsName("Math")
external object JsMath {
    fun max(lhs: Number, rhs: Number): Double
    fun floor(x: Number): Double
    fun ceil(x: Number): Double
    fun log(x: Number): Double
    fun pow(base: Number, exponent: Number): Double
    val LN2: Double
}

