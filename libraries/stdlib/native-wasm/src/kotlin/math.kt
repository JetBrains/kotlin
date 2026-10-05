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
