/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.math


import kotlin.internal.InlineOnly
import kotlin.js.JsMath as nativeMath


// region ================ Double Math ========================================

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sin(x: Double): Double = nativeMath.sin(x)

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cos(x: Double): Double = nativeMath.cos(x)

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun tan(x: Double): Double = nativeMath.tan(x)

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asin(x: Double): Double = nativeMath.asin(x)

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *   - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun acos(x: Double): Double = nativeMath.acos(x)

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atan(x: Double): Double = nativeMath.atan(x)

/**
 * Returns the angle `theta` of the polar coordinates `(r, theta)` that correspond
 * to the rectangular coordinates `(x, y)` by computing the arc tangent of the value [y] / [x];
 * the returned value is an angle in the range from `-PI` to `PI` radians.
 *
 * Special cases:
 *   - `atan2(0.0, 0.0)` is `0.0`
 *   - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
 *   - `atan2(-0.0, x)` is `-0.0` for `x > 0` and `-PI` for `x < 0`
 *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for `-Inf < y < 0`
 *   - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
 *   - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
 *   - `atan2(+Inf, x)` is `PI/2` for finite `x`y
 *   - `atan2(-Inf, x)` is `-PI/2` for finite `x`
 *   - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atan2(y: Double, x: Double): Double = nativeMath.atan2(y, x)

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sinh(x: Double): Double = nativeSinh(x)

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cosh(x: Double): Double = nativeCosh(x)

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun tanh(x: Double): Double = nativeTanh(x)

/**
 * Computes the inverse hyperbolic sine of the value [x].
 *
 * The returned value is `y` such that `sinh(y) == x`.
 *
 * Special cases:
 *   - `asinh(NaN)` is `NaN`
 *   - `asinh(+Inf)` is `+Inf`
 *   - `asinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asinh(x: Double): Double = nativeAsinh(x)

/**
 * Computes the inverse hyperbolic cosine of the value [x].
 *
 * The returned value is positive `y` such that `cosh(y) == x`.
 *
 * Special cases:
 *   - `acosh(NaN)` is `NaN`
 *   - `acosh(x)` is `NaN` when `x < 1`
 *   - `acosh(+Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun acosh(x: Double): Double = nativeAcosh(x)

/**
 * Computes the inverse hyperbolic tangent of the value [x].
 *
 * The returned value is `y` such that `tanh(y) == x`.
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(x)` is `NaN` when `x > 1` or `x < -1`
 *   - `tanh(1.0)` is `+Inf`
 *   - `tanh(-1.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atanh(x: Double): Double = nativeAtanh(x)

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun hypot(x: Double, y: Double): Double = nativeHypot(x, y)

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sqrt(x: Double): Double = nativeMath.sqrt(x)

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun exp(x: Double): Double = nativeMath.exp(x)

/**
 * Computes `exp(x) - 1`.
 *
 * This function can be implemented to produce more precise result for [x] near zero.
 *
 * Special cases:
 *   - `expm1(NaN)` is `NaN`
 *   - `expm1(+Inf)` is `+Inf`
 *   - `expm1(-Inf)` is `-1.0`
 *
 * @see [exp] function.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun expm1(x: Double): Double = nativeExpm1(x)

/**
 * Computes the logarithm of the value [x] to the given [base].
 *
 * Special cases:
 *   - `log(x, b)` is `NaN` if either `x` or `b` are `NaN`
 *   - `log(x, b)` is `NaN` when `x < 0` or `b <= 0` or `b == 1.0`
 *   - `log(+Inf, +Inf)` is `NaN`
 *   - `log(+Inf, b)` is `+Inf` for `b > 1` and `-Inf` for `b < 1`
 *   - `log(0.0, b)` is `-Inf` for `b > 1` and `+Inf` for `b > 1`
 *
 * See also logarithm functions for common fixed bases: [ln], [log10] and [log2].
 */
@SinceKotlin("1.2")
public actual fun log(x: Double, base: Double): Double {
    if (base <= 0.0 || base == 1.0) return Double.NaN
    return nativeMath.log(x) / nativeMath.log(base)
}

/**
 * Computes the natural logarithm (base `E`) of the value [x].
 *
 * Special cases:
 *   - `ln(NaN)` is `NaN`
 *   - `ln(x)` is `NaN` when `x < 0.0`
 *   - `ln(+Inf)` is `+Inf`
 *   - `ln(0.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ln(x: Double): Double = nativeMath.log(x)

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log10(x: Double): Double = nativeLog10(x)

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log2(x: Double): Double = nativeLog2(x)

/**
 * Computes `ln(x + 1)`.
 *
 * This function can be implemented to produce more precise result for [x] near zero.
 *
 * Special cases:
 *   - `ln1p(NaN)` is `NaN`
 *   - `ln1p(x)` is `NaN` where `x < -1.0`
 *   - `ln1p(-1.0)` is `-Inf`
 *   - `ln1p(+Inf)` is `+Inf`
 *
 * @see [ln] function
 * @see [expm1] function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ln1p(x: Double): Double = nativeLog1p(x)

/**
 * Rounds the given value [x] to an integer towards positive infinity.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the smallest double value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * @sample samples.math.MathSamples.Doubles.ceil
 * @sample samples.math.MathSamples.Doubles.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ceil(x: Double): Double = nativeMath.ceil(x)

/**
 * Rounds the given value [x] to an integer towards negative infinity.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the largest double value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * @sample samples.math.MathSamples.Doubles.floor
 * @sample samples.math.MathSamples.Doubles.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun floor(x: Double): Double = nativeMath.floor(x)

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * @sample samples.math.MathSamples.Doubles.truncate
 * @sample samples.math.MathSamples.Doubles.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun truncate(x: Double): Double = nativeTrunc(x)

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @sample samples.math.MathSamples.Doubles.round
 * @sample samples.math.MathSamples.Doubles.roundingModes
 */
@SinceKotlin("1.2")
public actual fun round(x: Double): Double {
    if (x % 0.5 != 0.0) {
        return nativeMath.round(x)
    }
    val floor = floor(x)
    return if (floor % 2 == 0.0) floor else ceil(x)
}

/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Double]
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun abs(x: Double): Double = nativeMath.abs(x)

/**
 * Returns the sign of the given value [x]:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `sign(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sign(x: Double): Double = nativeSign(x)


/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun min(a: Double, b: Double): Double = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun max(a: Double, b: Double): Double = nativeMath.max(a, b)


/**
 * Returns the cube root of [x]. For any `x`, `cbrt(-x) == -cbrt(x)`;
 * that is, the cube root of a negative value is the negative of the cube root
 * of that value's magnitude. Special cases:
 *
 * Special cases:
 *   - If the argument is `NaN`, then the result is `NaN`.
 *   - If the argument is infinite, then the result is an infinity with the same sign as the argument.
 *   - If the argument is zero, then the result is a zero with the same sign as the argument.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
@InlineOnly
public actual inline fun cbrt(x: Double): Double = nativeMath.cbrt(x)


// extensions

/**
 * Raises this value to the power [x].
 *
 * Special cases:
 *   - `b.pow(0.0)` is `1.0`
 *   - `b.pow(1.0) == b`
 *   - `b.pow(NaN)` is `NaN`
 *   - `NaN.pow(x)` is `NaN` for `x != 0.0`
 *   - `b.pow(Inf)` is `NaN` for `abs(b) == 1.0`
 *   - `b.pow(x)` is `NaN` for `b < 0` and `x` is finite and not an integer
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.pow(x: Double): Double = nativeMath.pow(this, x)

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.pow(n: Int): Double = nativeMath.pow(this, n.toDouble())

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Double.absoluteValue: Double get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `NaN.sign` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Double.sign: Double get() = nativeSign(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public actual fun Double.withSign(sign: Double): Double {
    val thisSignBit = doubleSignBit(this)
    val newSignBit = doubleSignBit(sign)
    return if (thisSignBit == newSignBit) this else -this
}

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.withSign(sign: Int): Double = this.withSign(sign.toDouble())

/**
 * Returns the ulp (unit in the last place) of this value.
 *
 * An ulp is a positive distance between this value and the next nearest [Double] value larger in magnitude.
 *
 * Special Cases:
 *   - `NaN.ulp` is `NaN`
 *   - `x.ulp` is `+Inf` when `x` is `+Inf` or `-Inf`
 *   - `0.0.ulp` is `Double.MIN_VALUE`
 */
@SinceKotlin("1.2")
public actual val Double.ulp: Double get() = when {
    this < 0 -> (-this).ulp
    this.isNaN() || this == Double.POSITIVE_INFINITY -> this
    this == Double.MAX_VALUE -> this - this.nextDown()
    else -> this.nextUp() - this
}

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
public actual fun Double.nextUp(): Double = when {
    this.isNaN() || this == Double.POSITIVE_INFINITY -> this
    this == 0.0 -> Double.MIN_VALUE
    else -> Double.fromBits(this.toRawBits() + if (this > 0) 1 else -1)
}

/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
public actual fun Double.nextDown(): Double = when {
    this.isNaN() || this == Double.NEGATIVE_INFINITY -> this
    this == 0.0 -> -Double.MIN_VALUE
    else -> Double.fromBits(this.toRawBits() + if (this > 0) -1 else 1)
}


/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
public actual fun Double.nextTowards(to: Double): Double = when {
    this.isNaN() || to.isNaN() -> Double.NaN
    to == this -> to
    to > this -> this.nextUp()
    else /* to < this */ -> this.nextDown()
}


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
    else -> nativeMath.round(this).toInt()
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
    else -> nativeMath.round(this).toLong()
}

// endregion



// region ================ Float Math ========================================

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sin(x: Float): Float = nativeMath.sin(x.toDouble()).toFloat()

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cos(x: Float): Float = nativeMath.cos(x.toDouble()).toFloat()

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun tan(x: Float): Float = nativeMath.tan(x.toDouble()).toFloat()

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asin(x: Float): Float = nativeMath.asin(x.toDouble()).toFloat()

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *   - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun acos(x: Float): Float = nativeMath.acos(x.toDouble()).toFloat()

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atan(x: Float): Float = nativeMath.atan(x.toDouble()).toFloat()

/**
 * Returns the angle `theta` of the polar coordinates `(r, theta)` that correspond
 * to the rectangular coordinates `(x, y)` by computing the arc tangent of the value [y] / [x];
 * the returned value is an angle in the range from `-PI` to `PI` radians.
 *
 * Special cases:
 *   - `atan2(0.0, 0.0)` is `0.0`
 *   - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
 *   - `atan2(-0.0, x)` is `-0.0` for `x > 0` and `-PI` for `x < 0`
 *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for `-Inf < y < 0`
 *   - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
 *   - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
 *   - `atan2(+Inf, x)` is `PI/2` for finite `x`y
 *   - `atan2(-Inf, x)` is `-PI/2` for finite `x`
 *   - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atan2(y: Float, x: Float): Float = nativeMath.atan2(y.toDouble(), x.toDouble()).toFloat()

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sinh(x: Float): Float = nativeSinh(x.toDouble()).toFloat()

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cosh(x: Float): Float = nativeCosh(x.toDouble()).toFloat()

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun tanh(x: Float): Float = nativeTanh(x.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic sine of the value [x].
 *
 * The returned value is `y` such that `sinh(y) == x`.
 *
 * Special cases:
 *   - `asinh(NaN)` is `NaN`
 *   - `asinh(+Inf)` is `+Inf`
 *   - `asinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asinh(x: Float): Float = nativeAsinh(x.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic cosine of the value [x].
 *
 * The returned value is positive `y` such that `cosh(y) == x`.
 *
 * Special cases:
 *   - `acosh(NaN)` is `NaN`
 *   - `acosh(x)` is `NaN` when `x < 1`
 *   - `acosh(+Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun acosh(x: Float): Float = nativeAcosh(x.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic tangent of the value [x].
 *
 * The returned value is `y` such that `tanh(y) == x`.
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(x)` is `NaN` when `x > 1` or `x < -1`
 *   - `tanh(1.0)` is `+Inf`
 *   - `tanh(-1.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun atanh(x: Float): Float = nativeAtanh(x.toDouble()).toFloat()

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun hypot(x: Float, y: Float): Float = nativeHypot(x.toDouble(), y.toDouble()).toFloat()

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sqrt(x: Float): Float = nativeMath.sqrt(x.toDouble()).toFloat()

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun exp(x: Float): Float = nativeMath.exp(x.toDouble()).toFloat()

/**
 * Computes `exp(x) - 1`.
 *
 * This function can be implemented to produce more precise result for [x] near zero.
 *
 * Special cases:
 *   - `expm1(NaN)` is `NaN`
 *   - `expm1(+Inf)` is `+Inf`
 *   - `expm1(-Inf)` is `-1.0`
 *
 * @see [exp] function.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun expm1(x: Float): Float = nativeExpm1(x.toDouble()).toFloat()

/**
 * Computes the logarithm of the value [x] to the given [base].
 *
 * Special cases:
 *   - `log(x, b)` is `NaN` if either `x` or `b` are `NaN`
 *   - `log(x, b)` is `NaN` when `x < 0` or `b <= 0` or `b == 1.0`
 *   - `log(+Inf, +Inf)` is `NaN`
 *   - `log(+Inf, b)` is `+Inf` for `b > 1` and `-Inf` for `b < 1`
 *   - `log(0.0, b)` is `-Inf` for `b > 1` and `+Inf` for `b > 1`
 *
 * See also logarithm functions for common fixed bases: [ln], [log10] and [log2].
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log(x: Float, base: Float): Float = log(x.toDouble(), base.toDouble()).toFloat()

/**
 * Computes the natural logarithm (base `E`) of the value [x].
 *
 * Special cases:
 *   - `ln(NaN)` is `NaN`
 *   - `ln(x)` is `NaN` when `x < 0.0`
 *   - `ln(+Inf)` is `+Inf`
 *   - `ln(0.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ln(x: Float): Float = nativeMath.log(x.toDouble()).toFloat()

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log10(x: Float): Float = nativeLog10(x.toDouble()).toFloat()

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log2(x: Float): Float = nativeLog2(x.toDouble()).toFloat()

/**
 * Computes `ln(x + 1)`.
 *
 * This function can be implemented to produce more precise result for [x] near zero.
 *
 * Special cases:
 *   - `ln1p(NaN)` is `NaN`
 *   - `ln1p(x)` is `NaN` where `x < -1.0`
 *   - `ln1p(-1.0)` is `-Inf`
 *   - `ln1p(+Inf)` is `+Inf`
 *
 * @see [ln] function
 * @see [expm1] function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ln1p(x: Float): Float = nativeLog1p(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards positive infinity.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the smallest Float value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * @sample samples.math.MathSamples.Floats.ceil
 * @sample samples.math.MathSamples.Floats.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ceil(x: Float): Float = nativeMath.ceil(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards negative infinity.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the largest Float value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * @sample samples.math.MathSamples.Floats.floor
 * @sample samples.math.MathSamples.Floats.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun floor(x: Float): Float = nativeMath.floor(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * @sample samples.math.MathSamples.Floats.truncate
 * @sample samples.math.MathSamples.Floats.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun truncate(x: Float): Float = truncate(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 *
 * @sample samples.math.MathSamples.Floats.round
 * @sample samples.math.MathSamples.Floats.roundingModes
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun round(x: Float): Float = round(x.toDouble()).toFloat()


/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Float]
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun abs(x: Float): Float = nativeMath.abs(x.toDouble()).toFloat()

/**
 * Returns the sign of the given value [x]:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `sign(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun sign(x: Float): Float = nativeSign(x.toDouble()).toFloat()



/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun min(a: Float, b: Float): Float = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun max(a: Float, b: Float): Float = nativeMath.max(a, b)


/**
 * Returns the cube root of [x]. For any `x`, `cbrt(-x) == -cbrt(x)`;
 * that is, the cube root of a negative value is the negative of the cube root
 * of that value's magnitude. Special cases:
 *
 * Special cases:
 *   - If the argument is `NaN`, then the result is `NaN`.
 *   - If the argument is infinite, then the result is an infinity with the same sign as the argument.
 *   - If the argument is zero, then the result is a zero with the same sign as the argument.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
@InlineOnly
public actual inline fun cbrt(x: Float): Float = nativeMath.cbrt(x.toDouble()).toFloat()


// extensions


/**
 * Raises this value to the power [x].
 *
 * Special cases:
 *   - `b.pow(0.0)` is `1.0`
 *   - `b.pow(1.0) == b`
 *   - `b.pow(NaN)` is `NaN`
 *   - `NaN.pow(x)` is `NaN` for `x != 0.0`
 *   - `b.pow(Inf)` is `NaN` for `abs(b) == 1.0`
 *   - `b.pow(x)` is `NaN` for `b < 0` and `x` is finite and not an integer
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.pow(x: Float): Float = nativeMath.pow(this.toDouble(), x.toDouble()).toFloat()

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.pow(n: Int): Float = nativeMath.pow(this.toDouble(), n.toDouble()).toFloat()

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Float.absoluteValue: Float get() = nativeMath.abs(this.toDouble()).toFloat()

/**
 * Returns the sign of this value:
 *   - `-1.0` if the value is negative,
 *   - zero if the value is zero,
 *   - `1.0` if the value is positive
 *
 * Special case:
 *   - `NaN.sign` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Float.sign: Float get() = nativeSign(this.toDouble()).toFloat()

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.withSign(sign: Float): Float = this.toDouble().withSign(sign.toDouble()).toFloat()

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.withSign(sign: Int): Float = this.toDouble().withSign(sign.toDouble()).toFloat()


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
@InlineOnly
public actual inline fun Float.roundToInt(): Int = toDouble().roundToInt()

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
@InlineOnly
public actual inline fun Float.roundToLong(): Long = toDouble().roundToLong()


// endregion

// region ================ Integer Math ========================================


/**
 * Returns the absolute value of the given value [n].
 *
 * Special cases:
 *   - `abs(Int.MIN_VALUE)` is `Int.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Int]
 */
// TODO: remove manual 'or' when KT-19290 is fixed
@SinceKotlin("1.2")
public actual fun abs(n: Int): Int = if (n < 0) (-n or 0) else n

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun min(a: Int, b: Int): Int = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun max(a: Int, b: Int): Int = nativeMath.max(a, b)

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Int.absoluteValue: Int get() = abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Int.sign: Int get() = (this shr (Int.SIZE_BITS - 1)) or (-this ushr (Int.SIZE_BITS - 1))

/**
 * Returns the absolute value of the given value [n].
 *
 * Special cases:
 *   - `abs(Long.MIN_VALUE)` is `Long.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Long]
 */
@SinceKotlin("1.2")
public actual fun abs(n: Long): Long = if (n < 0) -n else n

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@Suppress("NOTHING_TO_INLINE")
public actual inline fun min(a: Long, b: Long): Long = if (a <= b) a else b

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@Suppress("NOTHING_TO_INLINE")
public actual inline fun max(a: Long, b: Long): Long = if (a >= b) a else b

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline val Long.absoluteValue: Long get() = abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Long.sign: Int get() = ((this shr (Long.SIZE_BITS - 1)) or (-this ushr (Long.SIZE_BITS - 1))).toInt()


// endregion
