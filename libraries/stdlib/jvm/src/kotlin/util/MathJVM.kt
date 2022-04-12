/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("MathKt")

package kotlin.math

import kotlin.internal.InlineOnly
import kotlin.math.Constants.LN2
import kotlin.math.Constants.taylor_2_bound
import kotlin.math.Constants.taylor_n_bound
import kotlin.math.Constants.upper_taylor_2_bound
import kotlin.math.Constants.upper_taylor_n_bound

import java.lang.Math as nativeMath

private object Constants {
// constants
    /** Natural logarithm of 2.0, used to compute [log2] function */
    @JvmField
    internal val LN2: Double = ln(2.0)

    @JvmField
    internal val epsilon: Double = nativeMath.ulp(1.0)
    @JvmField
    internal val taylor_2_bound = nativeMath.sqrt(epsilon)
    @JvmField
    internal val taylor_n_bound = nativeMath.sqrt(taylor_2_bound)
    @JvmField
    internal val upper_taylor_2_bound = 1 / taylor_2_bound
    @JvmField
    internal val upper_taylor_n_bound = 1 / taylor_n_bound

}

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
 *    - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asin(x: Double): Double = nativeMath.asin(x)

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
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
 *   - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
 *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
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
public actual inline fun sinh(x: Double): Double = nativeMath.sinh(x)

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cosh(x: Double): Double = nativeMath.cosh(x)

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
public actual inline fun tanh(x: Double): Double = nativeMath.tanh(x)


// Inverse hyperbolic function implementations derived from boost special math functions,
// Copyright Eric Ford & Hubert Holin 2001.

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
public actual fun asinh(x: Double): Double =
    when {
        x >= +taylor_n_bound ->
            if (x > upper_taylor_n_bound) {
                if (x > upper_taylor_2_bound) {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 0
                    nativeMath.log(x) + LN2
                } else {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 1
                    nativeMath.log(x * 2 + (1 / (x * 2)))
                }
            } else {
                nativeMath.log(x + nativeMath.sqrt(x * x + 1))
            }
        x <= -taylor_n_bound -> -asinh(-x)
        else -> {
            // approximation by taylor series in x at 0 up to order 2
            var result = x;
            if (nativeMath.abs(x) >= taylor_2_bound) {
                // approximation by taylor series in x at 0 up to order 4
                result -= (x * x * x) / 6
            }
            result
        }
    }


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
public actual fun acosh(x: Double): Double =
    when {
        x < 1 -> Double.NaN

        x > upper_taylor_2_bound ->
            // approximation by laurent series in 1/x at 0+ order from -1 to 0
            nativeMath.log(x) + LN2

        x - 1 >= taylor_n_bound ->
            nativeMath.log(x + nativeMath.sqrt(x * x - 1))

        else -> {
            val y = nativeMath.sqrt(x - 1)
            // approximation by taylor series in y at 0 up to order 2
            var result = y
            if (y >= taylor_2_bound) {
                // approximation by taylor series in y at 0 up to order 4
                result -= (y * y * y) / 12
            }

            nativeMath.sqrt(2.0) * result
        }
    }

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
public actual fun atanh(x: Double): Double {
    if (nativeMath.abs(x) < taylor_n_bound) {
        var result = x
        if (nativeMath.abs(x) > taylor_2_bound) {
            result += (x * x * x) / 3
        }
        return result
    }
    return nativeMath.log((1 + x) / (1 - x)) / 2
}

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun hypot(x: Double, y: Double): Double = nativeMath.hypot(x, y)

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
public actual inline fun expm1(x: Double): Double = nativeMath.expm1(x)

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
public actual inline fun log10(x: Double): Double = nativeMath.log10(x)

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log2(x: Double): Double = nativeMath.log(x) / LN2

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
public actual inline fun ln1p(x: Double): Double = nativeMath.log1p(x)

/**
 * Rounds the given value [x] to an integer towards positive infinity.

 * @return the smallest double value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ceil(x: Double): Double = nativeMath.ceil(x)

/**
 * Rounds the given value [x] to an integer towards negative infinity.

 * @return the largest double value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun floor(x: Double): Double = nativeMath.floor(x)

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun truncate(x: Double): Double = when {
    x.isNaN() || x.isInfinite() -> x
    x > 0 -> floor(x)
    else -> ceil(x)
}

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun round(x: Double): Double = nativeMath.rint(x)


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
public actual inline fun sign(x: Double): Double = nativeMath.signum(x)



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
 * Returns the cube root of [x]. For positive finite `x`, `cbrt(-x) == -cbrt(x)`;
 * that is, the cube root of a negative value is the negative of the cube root
 * of that value's magnitude. Special cases:
 *
 * Special cases:
 *   - If the argument is `NaN`, then the result is `NaN`.
 *   - If the argument is infinite, then the result is an infinity with the same sign as the argument.
 *   - If the argument is zero, then the result is a zero with the same sign as the argument.
 */
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
 * Computes the remainder of division of this value by the [divisor] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * divisor)` where `q` is the quotient of division rounded to the nearest integer,
 * `q = round(this / other)`.
 *
 * Special cases:
 *    - `x.IEEErem(y)` is `NaN`, when `x` is `NaN` or `y` is `NaN` or `x` is `+Inf|-Inf` or `y` is zero.
 *    - `x.IEEErem(y) == x` when `x` is finite and `y` is infinite.
 *
 * @see round
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.IEEErem(divisor: Double): Double = nativeMath.IEEEremainder(this, divisor)

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
public actual inline val Double.sign: Double get() = nativeMath.signum(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.withSign(sign: Double): Double = nativeMath.copySign(this, sign)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.withSign(sign: Int): Double = nativeMath.copySign(this, sign.toDouble())

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
@InlineOnly
public actual inline val Double.ulp: Double get() = nativeMath.ulp(this)

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.nextUp(): Double = nativeMath.nextUp(this)

/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.nextDown(): Double = nativeMath.nextAfter(this, Double.NEGATIVE_INFINITY)

/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Double.nextTowards(to: Double): Double = nativeMath.nextAfter(this, to)

/**
 * Rounds this [Double] value to the nearest integer and converts the result to [Int].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *   - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
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
 */
@SinceKotlin("1.2")
public actual fun Double.roundToLong(): Long =
    if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)

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
 *    - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun asin(x: Float): Float = nativeMath.asin(x.toDouble()).toFloat()

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
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
 *   - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
 *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
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
public actual inline fun sinh(x: Float): Float = nativeMath.sinh(x.toDouble()).toFloat()

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun cosh(x: Float): Float = nativeMath.cosh(x.toDouble()).toFloat()

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
public actual inline fun tanh(x: Float): Float = nativeMath.tanh(x.toDouble()).toFloat()

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
public actual inline fun asinh(x: Float): Float = asinh(x.toDouble()).toFloat()

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
public actual inline fun acosh(x: Float): Float = acosh(x.toDouble()).toFloat()

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
public actual inline fun atanh(x: Float): Float = atanh(x.toDouble()).toFloat()

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun hypot(x: Float, y: Float): Float = nativeMath.hypot(x.toDouble(), y.toDouble()).toFloat()

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
public actual inline fun expm1(x: Float): Float = nativeMath.expm1(x.toDouble()).toFloat()

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
public actual fun log(x: Float, base: Float): Float {
    if (base <= 0.0F || base == 1.0F) return Float.NaN
    return (nativeMath.log(x.toDouble()) / nativeMath.log(base.toDouble())).toFloat()
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
public actual inline fun ln(x: Float): Float = nativeMath.log(x.toDouble()).toFloat()

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun log10(x: Float): Float = nativeMath.log10(x.toDouble()).toFloat()

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log2(x: Float): Float = (nativeMath.log(x.toDouble()) / LN2).toFloat()

/**
 * Computes `ln(a + 1)`.
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
public actual inline fun ln1p(x: Float): Float = nativeMath.log1p(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards positive infinity.

 * @return the smallest Float value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun ceil(x: Float): Float = nativeMath.ceil(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards negative infinity.

 * @return the largest Float value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun floor(x: Float): Float = nativeMath.floor(x.toDouble()).toFloat()

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun truncate(x: Float): Float = when {
    x.isNaN() || x.isInfinite() -> x
    x > 0 -> floor(x)
    else -> ceil(x)
}

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun round(x: Float): Float = nativeMath.rint(x.toDouble()).toFloat()


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
public actual inline fun abs(x: Float): Float = nativeMath.abs(x)

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
public actual inline fun sign(x: Float): Float = nativeMath.signum(x)



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
 * Returns the cube root of [x]. For positive finite `x`, `cbrt(-x) == -cbrt(x)`;
 * that is, the cube root of a negative value is the negative of the cube root
 * of that value's magnitude. Special cases:
 *
 * Special cases:
 *   - If the argument is `NaN`, then the result is `NaN`.
 *   - If the argument is infinite, then the result is an infinity with the same sign as the argument.
 *   - If the argument is zero, then the result is a zero with the same sign as the argument.
 */
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
 * Computes the remainder of division of this value by the [divisor] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * divisor)` where `q` is the quotient of division rounded to the nearest integer,
 * `q = round(this / other)`.
 *
 * Special cases:
 *    - `x.IEEErem(y)` is `NaN`, when `x` is `NaN` or `y` is `NaN` or `x` is `+Inf|-Inf` or `y` is zero.
 *    - `x.IEEErem(y) == x` when `x` is finite and `y` is infinite.
 *
 * @see round
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.IEEErem(divisor: Float): Float = nativeMath.IEEEremainder(this.toDouble(), divisor.toDouble()).toFloat()

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
public actual inline val Float.absoluteValue: Float get() = nativeMath.abs(this)

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
public actual inline val Float.sign: Float get() = nativeMath.signum(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.withSign(sign: Float): Float = nativeMath.copySign(this, sign)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun Float.withSign(sign: Int): Float = nativeMath.copySign(this, sign.toFloat())

/**
 * Returns the ulp of this value.
 *
 * An ulp is a positive distance between this value and the next nearest [Float] value larger in magnitude.
 *
 * Special Cases:
 *   - `NaN.ulp` is `NaN`
 *   - `x.ulp` is `+Inf` when `x` is `+Inf` or `-Inf`
 *   - `0.0.ulp` is `Float.MIN_VALUE`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Float.ulp: Float get() = nativeMath.ulp(this)

/**
 * Returns the [Float] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.nextUp(): Float = nativeMath.nextUp(this)

/**
 * Returns the [Float] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.nextDown(): Float = nativeMath.nextAfter(this, Double.NEGATIVE_INFINITY)

/**
 * Returns the [Float] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.nextTowards(to: Float): Float = nativeMath.nextAfter(this, to.toDouble())

/**
 * Rounds this [Float] value to the nearest integer and converts the result to [Int].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *   - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public actual fun Float.roundToInt(): Int =
    if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)

/**
 * Rounds this [Float] value to the nearest integer and converts the result to [Long].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *   - `x.roundToLong() == Long.MAX_VALUE` when `x > Long.MAX_VALUE`
 *   - `x.roundToLong() == Long.MIN_VALUE` when `x < Long.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public actual fun Float.roundToLong(): Long = toDouble().roundToLong()


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
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun abs(n: Int): Int = nativeMath.abs(n)

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
public actual inline val Int.absoluteValue: Int get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Int.sign: Int get() = when {
    this < 0 -> -1
    this > 0 -> 1
    else -> 0
}



/**
 * Returns the absolute value of the given value [n].
 *
 * Special cases:
 *   - `abs(Long.MIN_VALUE)` is `Long.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Long]
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun abs(n: Long): Long = nativeMath.abs(n)

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun min(a: Long, b: Long): Long = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public actual inline fun max(a: Long, b: Long): Long = nativeMath.max(a, b)

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
public actual inline val Long.absoluteValue: Long get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Long.sign: Int get() = when {
    this < 0 -> -1
    this > 0 -> 1
    else -> 0
}


// endregion
