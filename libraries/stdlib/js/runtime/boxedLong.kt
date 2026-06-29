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

import withType
import kotlin.internal.InlineOnly
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.reflect.js.internal.PrimitiveKClassImpl

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

/* Several algorithms come from "the JS Long paper":
 *   S. Doeraene and T. Schlatter,
 *   "64-bit Integer Division for the JavaScript Platform,"
 *   33rd IEEE Symposium on Computer Arithmetic (ARITH), Fulda, Germany, 2026.
 *   https://arith2026.org/papers/64-bit%20Integer%20Division%20for%20the%20JavaScript%20Platform.pdf
 */

/**
 * @see kotlin.js.internal.longAsBigInt.toNumber
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.toNumber(): Double {
    // JS Long paper, Section V.A.
    return high.toDouble() * TWO_PWR_32_DBL_ + low.toUInt().toDouble()
}

@BoxedLongApi
@InlineOnly
private inline fun toUnsignedNumber(low: Int, high: Int): Double {
    // JS Long paper, Section V.A.
    return high.toUInt().toDouble() * TWO_PWR_32_DBL_ + low.toUInt().toDouble()
}

@BoxedLongApi
@InlineOnly
private inline fun fromUnsignedSafeDouble(x: Double): Long {
    // JS Long paper, Section V.B.
    return Long(jsToInt32(x), jsToInt32(x * TWO_PWR_M32_DBL_))
}

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
@UsedFromCompilerGeneratedCode
internal fun hashCode(l: Long) = l.low xor l.high

// toString(), see the JS Long paper, Section VIII

@InlineOnly
private inline fun jsToString(x: Double, base: Int): String = js("x.toString(base)")

/**
 * @see kotlin.js.internal.longAsBigInt.toStringImpl
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.toStringImpl(radix: Int): String {
    // JS Long paper, Algorithm 11

    val low = this.low
    val high = this.high

    if ((low shr 31) == high) {
        // -2^31 <= this < 2^31
        return jsToString(low.toDouble(), radix)
    } else if (((high xor (high shr 10)) and 0xffe00000.toInt()) == 0) {
        // -2^53 <= this < 2^53
        return jsToString(toNumber(), radix)
    } else {
        // |this| >= 2^53
        if (high >= 0) {
            return ulongToStringLarge(this, radix)
        } else {
            return "-" + ulongToStringLarge(negate(), radix)
        }
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.negate
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.negate(): Long {
    // manually inlined and simplified from 0L - this
    val rlow = -low
    val rhigh = -high - boolToInt(rlow != 0)
    return Long(rlow, rhigh)
}

@BoxedLongApi
private fun Long.isNegative() = high < 0

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.equalsLong(other: Long) = bothZero(high xor other.high, low xor other.low)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.lessThan(other: Long): Boolean {
    if (high == other.high) {
        return low.toUInt().toDouble() < other.low.toUInt().toDouble()
    } else {
        return high < other.high
    }
}

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.greaterThan(other: Long): Boolean {
    if (high == other.high) {
        return low.toUInt().toDouble() > other.low.toUInt().toDouble()
    } else {
        return high > other.high
    }
}

@BoxedLongApi
@UsedFromCompilerGeneratedCode
private fun Long.greaterThanOrEqual(other: Long): Boolean {
    if (high == other.high) {
        return low.toUInt().toDouble() >= other.low.toUInt().toDouble()
    } else {
        return high > other.high
    }
}

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.compare(other: Long): Int {
    if (high == other.high) {
      return low.toUInt().compareTo(other.low.toUInt())
    } else {
      return if (high < other.high) -1 else 1
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.add
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.add(other: Long): Long {
    // JS Long paper, Algorithm 1
    val rlow = low + other.low
    val rhigh = high + other.high + boolToInt(rlow.toUInt().toDouble() < low.toUInt().toDouble())
    return Long(rlow, rhigh)
}

/**
 * @see kotlin.js.internal.longAsBigInt.subtract
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.subtract(other: Long): Long {
    // JS Long paper, Algorithm 2
    val rlow = low - other.low
    val rhigh = high - other.high - boolToInt(rlow.toUInt().toDouble() > low.toUInt().toDouble())
    return Long(rlow, rhigh)
}

/**
 * @see kotlin.js.internal.longAsBigInt.multiply
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.multiply(other: Long): Long {
    // JS Long paper, Algorithm 3

    val alow = low
    val blow = other.low

    val a0 = alow and 0xffff
    val a1 = alow ushr 16
    val b0 = blow and 0xffff
    val b1 = blow ushr 16

    val a0b0 = a0 * b0
    val a1b0 = a1 * b0
    val a0b1 = a0 * b1

    /* rlow = alow * blow, but we compute the above 3 subproducts for rhigh
     * anyway, we reuse them to compute rlow too, trading a * for 2 +'s and 1 <<.
     */
    val rlow = a0b0 + ((a1b0 + a0b1) shl 16)

    val c1part = (a0b0 ushr 16) + a0b1
    val rhigh = (
        alow * other.high + high * blow + a1 * b1 + (c1part ushr 16) +
        (((c1part and 0xffff) + a1b0) ushr 16)
    )

    return Long(rlow, rhigh)
}

/**
 * @see kotlin.js.internal.longAsBigInt.divide
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.divide(other: Long): Long {
    if (other.isZero()) throw ArithmeticException("/ by zero")
    // JS Long paper, Section VI, Algorithm 7, specialized to get the quotient

    val thisHigh = this.high
    val otherHigh = other.high
    val a = if (thisHigh < 0) this.negate() else this
    val b = if (otherHigh < 0) other.negate() else other

    val alo = a.low
    val ahi = a.high
    val blo = b.low
    val bhi = b.high

    /* Conveniently, when b = 0, we enter the first case, where the first thing
     * we do is an int division by blo. If that throws an ArithmeticException,
     * we will throw it as well. If it ignores it and returns 0, we'll also
     * compute a 0 quotient, so we're in sync with the behavior of Int division.
     */

    val q = if (bothZero(bhi, blo and 0xffe00000.toInt())) {
        // b < 2^21, Algorithm 4
        val quotHi = (ahi.toUInt() / blo.toUInt()).toInt()
        val k = ahi - blo * quotHi
        val quotLo = jsToInt32(toUnsignedNumber(alo, k) / blo.toDouble())
        Long(quotLo, quotHi)
    } else {
        // 2^21 <= b <= 2^63, Algorithm 5
        val aHat = toUnsignedNumber(alo, ahi)
        val bHat = toUnsignedNumber(blo, bhi)
        val qHat = fromUnsignedSafeDouble((aHat / bHat) + 0.00390625) // 2^(-8)
        val rHat = a - b * qHat
        if (rHat.isNegative()) qHat - 1L else qHat
    }

    return if ((thisHigh xor otherHigh) < 0) {
        return q.negate()
    } else {
        return q
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.modulo
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.modulo(other: Long): Long {
    // JS Long paper, Section VI, Algorithm 7, specialized to get the remainder

    val thisHigh = this.high
    val otherHigh = other.high
    val a = if (thisHigh < 0) this.negate() else this
    val b = if (otherHigh < 0) other.negate() else other

    val alo = a.low
    val ahi = a.high
    val blo = b.low
    val bhi = b.high

    // See divide about division by 0

    val r = if (bothZero(bhi, blo and 0xffe00000.toInt())) {
        // b < 2^21, Algorithm 4
        val k = (ahi.toUInt() % blo.toUInt()).toInt()
        val quotLo = jsToInt32(toUnsignedNumber(alo, k) / blo.toDouble())
        val remLo = alo - blo * quotLo
        Long(remLo, 0)
    } else {
        // 2^21 <= b <= 2^63, Algorithm 5
        val aHat = toUnsignedNumber(alo, ahi)
        val bHat = toUnsignedNumber(blo, bhi)
        val qHat = fromUnsignedSafeDouble((aHat / bHat) + 0.00390625) // 2^(-8)
        val rHat = a - b * qHat
        if (rHat.isNegative()) rHat + b else rHat
    }

    if (thisHigh < 0) {
        return r.negate()
    } else {
        return r
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.shiftLeft
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftLeft(numBits: Int): Long {
    /* For a proof, see the comment in the Scala.js implementation:
     * https://github.com/scala-js/scala-js/blob/v1.22.0/linker-private-library/src/main/scala/org/scalajs/linker/runtime/RuntimeLong.scala#L111
     */
    if ((numBits and 32) == 0) {
        return Long(low shl numBits, (high shl numBits) or ((low ushr 1) ushr (31 - numBits)))
    } else {
        return Long(0, low shl numBits)
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.shiftRight
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftRight(numBits: Int): Long {
    // Similar to shiftLeft
    if ((numBits and 32) == 0) {
        return Long((low ushr numBits) or ((high shl 1) shl (31 - numBits)), high shr numBits)
    } else {
        return Long(high shr numBits, high shr 31)
    }
}

/**
 * @see kotlin.js.internal.longAsBigInt.shiftRightUnsigned
 */
@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun Long.shiftRightUnsigned(numBits: Int): Long {
    // Similar to shiftLeft
    if ((numBits and 32) == 0) {
        return Long((low ushr numBits) or ((high shl 1) shl (31 - numBits)), high ushr numBits)
    } else {
        return Long(high ushr numBits, 0)
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
internal fun fromInt(value: dynamic) = Long(value, value.unsafeCast<Int>() shr 31)

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
    /* For a proof, see the comment in the Scala.js implementation:
     * https://github.com/scala-js/scala-js/blob/v1.22.0/linker-private-library/src/main/scala/org/scalajs/linker/runtime/RuntimeLong.scala#L402
     */
    if (value <= -TWO_PWR_63_DBL_) {
        return MIN_VALUE;
    } else if (value >= TWO_PWR_63_DBL_) {
        return MAX_VALUE;
    } else {
        val rawLow = jsToInt32(value)
        val rawHigh = jsToInt32(value * TWO_PWR_M32_DBL_)
        return Long(
            rawLow,
            if (value < 0 && rawLow != 0) rawHigh - 1 else rawHigh
        )
    }
}

private const val TWO_PWR_32_DBL_ = 4294967296.0

private const val TWO_PWR_M32_DBL_ = 1.0 / TWO_PWR_32_DBL_

private const val TWO_PWR_63_DBL_ = 9223372036854775808.0

@BoxedLongApi
@UsedFromCompilerGeneratedCode
@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
internal val ONE = fromInt(1)

@BoxedLongApi
@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
private val MAX_VALUE = Long(-1, -1 ushr 1)

@BoxedLongApi
@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
private val MIN_VALUE = Long(0, 1 shl 31)

@BoxedLongApi
@UsedFromCompilerGeneratedCode
@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
// TODO(KT-85540): remove the property after bootstrapping
internal val longArrayClass = PrimitiveKClassImpl(js("Array").unsafeCast<JsClass<LongArray>>(), "LongArray", { it is LongArray })

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun isLongArray(a: dynamic): Boolean = isJsArray(a) && a.`$type$` === "LongArray"

@BoxedLongApi
@UsedFromCompilerGeneratedCode
internal fun longCopyOfRange(arr: dynamic, fromIndex: dynamic = VOID, toIndex: dynamic = VOID): LongArray =
    withType("LongArray", arr.slice(fromIndex, toIndex)).unsafeCast<LongArray>()

@InlineOnly
private inline fun bothZero(a: Int, b: Int): Boolean = (a or b) == 0

@InlineOnly
@OptIn(JsIntrinsic::class)
private inline fun jsToInt32(x: Double): Int = jsBitOr(x, 0)

@InlineOnly
@OptIn(JsIntrinsic::class)
private inline fun boolToInt(x: Boolean): Int = jsBitOr(x, 0)
