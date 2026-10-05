/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.math

// region ================ Double Math ========================================

/**
 * Returns the sign of the given value [x]:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `sign(NaN)` is `NaN`
 *
 * @sample samples.math.MathSamples.Doubles.signFun
 */
@SinceKotlin("1.2")
public actual fun sign(x: Double): Double = when {
    x.isNaN() -> Double.NaN
    x > 0.0 -> 1.0
    x < 0.0 -> -1.0
    else -> x
}

/**
 * Returns the sign of this value:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `NaN.sign` is `NaN`
 *
 * @sample samples.math.MathSamples.Doubles.sign
 */
@SinceKotlin("1.2")
public actual val Double.sign: Double get() = sign(this)

/**
 * Rounds this [Double] value to the nearest integer and converts the result to [Int].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *   - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 * @sample samples.math.MathSamples.Doubles.roundToInt
 */
@SinceKotlin("1.2")
public actual fun Double.roundToInt(): Int = when {
    isNaN() -> throw IllegalArgumentException("Cannot round NaN value.")
    this > Int.MAX_VALUE -> Int.MAX_VALUE
    this < Int.MIN_VALUE -> Int.MIN_VALUE
    else -> floor(this + 0.5).toInt()
}

/**
 * Rounds this [Double] value to the nearest integer and converts the result to [Long].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToLong() == Long.MAX_VALUE` when `x > Long.MAX_VALUE`
 *   - `x.roundToLong() == Long.MIN_VALUE` when `x < Long.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 * @sample samples.math.MathSamples.Doubles.roundToLong
 */
@SinceKotlin("1.2")
public actual fun Double.roundToLong(): Long = when {
    isNaN() -> throw IllegalArgumentException("Cannot round NaN value.")
    this > Long.MAX_VALUE -> Long.MAX_VALUE
    this < Long.MIN_VALUE -> Long.MIN_VALUE
    else -> floor(this + 0.5).toLong()
}

/**
 * Returns the ulp (unit in the last place) of this value.
 *
 * An ulp is a positive distance between this value and the next nearest [Double] value larger in magnitude.
 *
 * Special cases:
 *   - `NaN.ulp` is `NaN`
 *   - `x.ulp` is `+Inf` when `x` is `+Inf` or `-Inf`
 *   - `x.ulp` is `2^971` when `x` is `Double.MAX_VALUE` or `-Double.MAX_VALUE`
 *   - `0.0.ulp` is `Double.MIN_VALUE`
 *
 * @see nextUp
 * @see nextDown
 * @see nextTowards
 * @sample samples.math.MathSamples.Doubles.ulp
 * @sample samples.math.MathSamples.Doubles.discreteValues
 */
@SinceKotlin("1.2")
public actual val Double.ulp: Double
    get() {
        val magnitude = abs(this)
        val bits = magnitude.toRawBits()
        // 1. Check the exponent: drop 52 fraction bits and check if what's left is 0x7ffL (the sign bit isn't set, all eight exponent bits
        // are), if yes, it's either NaN or +Inf.
        return if (bits shr 52 != 0x7ffL) {
            // 2.1. If magnitude is Double.MAX_VALUE, return 2^971, otherwise the difference between magnitude.nextUp() & magnitude.
            if (bits == 0x7fef_ffff_ffff_ffffL) Double.fromBits(0x7ca0_0000_0000_0000L) else Double.fromBits(bits + 1) - magnitude
        } else {
            // 2.2. If this is NaN, return as-is (abs won't change it), if this is +Inf or -Inf, return +Inf.
            magnitude
        }
    }

// endregion

// region ================ Float Math ========================================

/**
 * Returns the sign of the given value [x]:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `sign(NaN)` is `NaN`
 *
 * @sample samples.math.MathSamples.Floats.signFun
 */
@SinceKotlin("1.2")
public actual fun sign(x: Float): Float = when {
    x.isNaN() -> Float.NaN
    x > 0.0f -> 1.0f
    x < 0.0f -> -1.0f
    else -> x
}

/**
 * Returns the sign of this value:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `NaN.sign` is `NaN`
 *
 * @sample samples.math.MathSamples.Floats.sign
 */
@SinceKotlin("1.2")
public actual val Float.sign: Float
    get() = sign(this)

/**
 * Rounds this [Float] value to the nearest integer and converts the result to [Int].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *   - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 * @sample samples.math.MathSamples.Floats.roundToInt
 */
@SinceKotlin("1.2")
public actual fun Float.roundToInt(): Int = when {
    isNaN() -> throw IllegalArgumentException("Cannot round NaN value.")
    this > Int.MAX_VALUE -> Int.MAX_VALUE
    this < Int.MIN_VALUE -> Int.MIN_VALUE
    else -> floor(this + 0.5f).toInt()
}

/**
 * Rounds this [Float] value to the nearest integer and converts the result to [Long].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToLong() == Long.MAX_VALUE` when `x > Long.MAX_VALUE`
 *   - `x.roundToLong() == Long.MIN_VALUE` when `x < Long.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 * @sample samples.math.MathSamples.Floats.roundToLong
 */
@SinceKotlin("1.2")
public actual fun Float.roundToLong(): Long = when {
    isNaN() -> throw IllegalArgumentException("Cannot round NaN value.")
    this > Long.MAX_VALUE -> Long.MAX_VALUE
    this < Long.MIN_VALUE -> Long.MIN_VALUE
    else -> floor(this + 0.5f).toLong()
}

// endregion

// region ================ Integer Math ========================================

/**
 * Returns the smaller of two values.
 *
 * @sample samples.math.MathSamples.Ints.min
 */
@SinceKotlin("1.2")
public actual fun min(a: Int, b: Int): Int = if (a < b) a else b

/**
 * Returns the greater of two values.
 *
 * @sample samples.math.MathSamples.Ints.max
 */
@SinceKotlin("1.2")
public actual fun max(a: Int, b: Int): Int = if (a > b) a else b

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs function
 * @sample samples.math.MathSamples.Ints.absoluteValue
 */
@SinceKotlin("1.2")
public actual val Int.absoluteValue: Int get() = abs(this)

/**
 * Returns the smaller of two values.
 *
 * @sample samples.math.MathSamples.Longs.min
 */
@SinceKotlin("1.2")
public actual fun min(a: Long, b: Long): Long = if (a <= b) a else b

/**
 * Returns the greater of two values.
 *
 * @sample samples.math.MathSamples.Longs.max
 */
@SinceKotlin("1.2")
public actual fun max(a: Long, b: Long): Long = if (a >= b) a else b

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 * @sample samples.math.MathSamples.Longs.absoluteValue
 */
@SinceKotlin("1.2")
public actual val Long.absoluteValue: Long get() = abs(this)

// endregion
