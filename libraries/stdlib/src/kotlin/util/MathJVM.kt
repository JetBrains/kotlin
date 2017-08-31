/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmVersion
@file:JvmName("MathKt")

package kotlin.math

import kotlin.internal.InlineOnly
import java.lang.Math as nativeMath

// constants
/** Ratio of the circumference of a circle to its diameter, approximately 3.14159. */
@SinceKotlin("1.2")
public const val PI: Double = nativeMath.PI
/** Base of the natural logarithms, approximately 2.71828. */
@SinceKotlin("1.2")
public const val E: Double = nativeMath.E
/** Natural logarithm of 2.0, used to compute [log2] function */
private val LN2: Double = ln(2.0)

private val epsilon: Double = nativeMath.ulp(1.0)
private val taylor_2_bound = nativeMath.sqrt(epsilon)
private val taylor_n_bound = nativeMath.sqrt(taylor_2_bound)
private val upper_taylor_2_bound = 1 / taylor_2_bound
private val upper_taylor_n_bound = 1 / taylor_n_bound

// ================ Double Math ========================================

/** Computes the sine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sin(a: Double): Double = nativeMath.sin(a)

/** Computes the cosine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun cos(a: Double): Double = nativeMath.cos(a)

/** Computes the tangent of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun tan(a: Double): Double = nativeMath.tan(a)

/**
 * Computes the arc sine of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun asin(a: Double): Double = nativeMath.asin(a)

/**
 * Computes the arc cosine of the value [a];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun acos(a: Double): Double = nativeMath.acos(a)

/**
 * Computes the arc tangent of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *     - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun atan(a: Double): Double = nativeMath.atan(a)

/**
 * Returns the angle `theta` of the polar coordinates `(r, theta)` that correspond
 * to the rectangular coordinates `(x, y)` by computing the arc tangent of the value [y] / [x];
 * the returned value is an angle in the range from `-PI` to `PI` radians.
 *
 * Special cases:
 *     - `atan2(0.0, 0.0)` is `0.0`
 *     - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
 *     - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
 *     - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
 *     - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
 *     - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
 *     - `atan2(+Inf, x)` is `PI/2` for finite `x`y
 *     - `atan2(-Inf, x)` is `-PI/2` for finite `x`
 *     - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun atan2(y: Double, x: Double): Double = nativeMath.atan2(y, x)

/**
 * Computes the hyperbolic sine of the value [a].
 *
 * Special cases:
 *
 *     - `sinh(NaN)` is `NaN`
 *     - `sinh(+Inf)` is `+Inf`
 *     - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sinh(a: Double): Double = nativeMath.sinh(a)

/**
 * Computes the hyperbolic cosine of the value [a].
 *
 * Special cases:
 *
 *     - `cosh(NaN)` is `NaN`
 *     - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun cosh(a: Double): Double = nativeMath.cosh(a)

/**
 * Computes the hyperbolic tangent of the value [a].
 *
 * Special cases:
 *
 *     - `tanh(NaN)` is `NaN`
 *     - `tanh(+Inf)` is `1.0`
 *     - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun tanh(a: Double): Double = nativeMath.tanh(a)


// Inverse hyperbolic function implementations derived from boost special math functions,
// Copyright Eric Ford & Hubert Holin 2001.

/**
 * Computes the inverse hyperbolic sine of the value [a].
 *
 * The returned value is `x` such that `sinh(x) == a`.
 *
 * Special cases:
 *
 *     - `asinh(NaN)` is `NaN`
 *     - `asinh(+Inf)` is `+Inf`
 *     - `asinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
public fun asinh(a: Double): Double =
    when {
        a >= +taylor_n_bound ->
            if (a > upper_taylor_n_bound) {
                if (a > upper_taylor_2_bound) {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 0
                    nativeMath.log(a) + LN2
                } else {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 1
                    nativeMath.log(a * 2 + (1 / (a * 2)))
                }
            } else {
                nativeMath.log(a + nativeMath.sqrt(a * a + 1))
            }
        a <= -taylor_n_bound -> -asinh(-a)
        else -> {
            // approximation by taylor series in x at 0 up to order 2
            var result = a;
            if (nativeMath.abs(a) >= taylor_2_bound) {
                // approximation by taylor series in x at 0 up to order 4
                result -= (a * a * a) / 6
            }
            result
        }
    }


/**
 * Computes the inverse hyperbolic cosine of the value [a].
 *
 * The returned value is positive `x` such that `cosh(x) == a`.
 *
 * Special cases:
 *
 *     - `acosh(NaN)` is `NaN`
 *     - `acosh(x)` is `NaN` when `x < 1`
 *     - `acosh(+Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
public fun acosh(a: Double): Double =
    when {
        a < 1 -> Double.NaN

        a > upper_taylor_2_bound ->
            // approximation by laurent series in 1/x at 0+ order from -1 to 0
            nativeMath.log(a) + LN2

        a - 1 >= taylor_n_bound ->
            nativeMath.log(a + nativeMath.sqrt(a * a - 1))

        else -> {
            val y = nativeMath.sqrt(a - 1)
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
 * Computes the inverse hyperbolic tangent of the value [a].
 *
 * The returned value is `x` such that `tanh(x) == a`.
 *
 * Special cases:
 *
 *     - `tanh(NaN)` is `NaN`
 *     - `tanh(x)` is `NaN` when `x > 1` or `x < -1`
 *     - `tanh(1.0)` is `+Inf`
 *     - `tanh(-1.0)` is `-Inf`
 */
@SinceKotlin("1.2")
public fun atanh(x: Double): Double {
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
 *     - returns `+Inf` if any of arguments is infinite
 *     - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun hypot(x: Double, y: Double): Double = nativeMath.hypot(x, y)

/**
 * Computes the positive square root of the value [a].
 *
 * Special cases:
 *     - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sqrt(a: Double): Double = nativeMath.sqrt(a)

/**
 * Computes Euler's number `e` raised to the power of the value [a].
 *
 * Special cases:
 *     - `exp(NaN)` is `NaN`
 *     - `exp(+Inf)` is `+Inf`
 *     - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun exp(a: Double): Double = nativeMath.exp(a)

/**
 * Computes `exp(a) - 1`.
 *
 * This function can be implemented to produce more precise result for [a] near zero.
 *
 * Special cases:
 *     - `expm1(NaN)` is `NaN`
 *     - `expm1(+Inf)` is `+Inf`
 *     - `expm1(-Inf)` is `-1.0`
 *
 * @see [exp] function.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun expm1(a: Double): Double = nativeMath.expm1(a)

/**
 * Computes the logarithm of the value [a] to the given [base].
 *
 * Special cases:
 *     - `log(a, b)` is `NaN` if either `a` or `b` are `NaN`
 *     - `log(a, b)` is `NaN` when `a < 0` or `b <= 0` or `b == 1.0`
 *     - `log(+Inf, +Inf)` is `NaN`
 *     - `log(+Inf, b)` is `+Inf` for `b > 1` and `-Inf` for `b < 1`
 *     - `log(0.0, b)` is `-Inf` for `b > 1` and `+Inf` for `b > 1`
 */
@SinceKotlin("1.2")
public fun log(a: Double, base: Double): Double {
    if (base <= 0.0 || base == 1.0) return Double.NaN
    return nativeMath.log(a) / nativeMath.log(base)
}

/**
 * Computes the natural logarithm (base `E`) of the value [a].
 *
 * Special cases:
 *     - `ln(NaN)` is `NaN`
 *     - `ln(x)` is `NaN` when `x < 0.0`
 *     - `ln(+Inf)` is `+Inf`
 *     - `ln(0.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ln(a: Double): Double = nativeMath.log(a)

/**
 * Computes the common logarithm (base 10) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun log10(a: Double): Double = nativeMath.log10(a)

/**
 * Computes the binary logarithm (base 2) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public fun log2(a: Double): Double = nativeMath.log(a) / LN2

/**
 * Computes `ln(a + 1)`.
 *
 * This function can be implemented to produce more precise result for [a] near zero.
 *
 * Special cases:
 *     - `ln1p(NaN)` is `NaN`
 *     - `ln1p(x)` is `NaN` where `x < -1.0`
 *     - `ln1p(-1.0)` is `-Inf`
 *     - `ln1p(+Inf)` is `+Inf`
 *
 * @see [ln] function
 * @see [expm1] function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ln1p(a: Double): Double = nativeMath.log1p(a)

/**
 * Rounds the given value [a] to an integer towards positive infinity.

 * @return the smallest double value that is greater than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ceil(a: Double): Double = nativeMath.ceil(a)

/**
 * Rounds the given value [a] to an integer towards negative infinity.

 * @return the largest double value that is smaller than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun floor(a: Double): Double = nativeMath.floor(a)

/**
 * Rounds the given value [a] to an integer towards zero.
 *
 * @return the value [a] having its fractional part truncated.
 *
 * Special cases:
 *     - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public fun truncate(a: Double): Double = when {
    a.isNaN() || a.isInfinite() -> a
    a > 0 -> floor(a)
    else -> ceil(a)
}

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun round(a: Double): Double = nativeMath.rint(a)


/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Double]
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun abs(a: Double): Double = nativeMath.abs(a)

/**
 * Returns the sign of the given value [a]:
 *     - `-1.0` if the value is negative,
 *     - zero if the value is zero,
 *     - `1.0` if the value is positive
 *
 * Special case:
 *     - `sign(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sign(a: Double): Double = nativeMath.signum(a)



/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Double, b: Double): Double = nativeMath.min(a, b)
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Double, b: Double): Double = nativeMath.max(a, b)

// extensions


/**
 * Raises this value to the power [other].
 *
 * Special cases:
 *     - `x.pow(0.0)` is `1.0`
 *     - `x.pow(1.0) == x`
 *     - `x.pow(NaN)` is `NaN`
 *     - `NaN.pow(x)` is `NaN` for `x != 0.0`
 *     - `x.pow(Inf)` is `NaN` for `abs(x) == 1.0`
 *     - `x.pow(y)` is `NaN` for `x < 0` and `y` is finite and not an integer
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.pow(other: Double): Double = nativeMath.pow(this, other)

/**
 * Raises this value to the integer power [other].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.pow(other: Int): Double = nativeMath.pow(this, other.toDouble())

/**
 * Computes the remainder of division of this value by the [other] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * other)` where `q` is the quotient of division rounded to the nearest integer,
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
public inline fun Double.IEEErem(other: Double): Double = nativeMath.IEEEremainder(this, other)

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Double.absoluteValue: Double get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *     - `-1.0` if the value is negative,
 *     - zero if the value is zero,
 *     - `1.0` if the value is positive
 *
 * Special case:
 *     - `NaN.sign` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Double.sign: Double get() = nativeMath.signum(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.withSign(sign: Double): Double = nativeMath.copySign(this, sign)
/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.withSign(sign: Int): Double = nativeMath.copySign(this, sign.toDouble())

/**
 * Returns the ulp (unit in the last place) of this value.
 *
 * An ulp is a positive distance between this value and the next nearest [Double] value larger in magnitude.
 *
 * Special Cases:
 *     - `NaN.ulp` is `NaN`
 *     - `x.ulp` is `+Inf` when `x` is `+Inf` or `-Inf`
 *     - `0.0.ulp` is `Double.MIN_VALUE`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Double.ulp: Double get() = nativeMath.ulp(this)

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.nextUp(): Double = nativeMath.nextUp(this)
/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.nextDown(): Double = nativeMath.nextAfter(this, Double.NEGATIVE_INFINITY)

/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *     - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *     - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.nextTowards(to: Double): Double = nativeMath.nextAfter(this, to)

/**
 * Rounds this [Double] value to the nearest integer and converts the result to [Int].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *     - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *     - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public fun Double.roundToInt(): Int = when {
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
 *     - `x.roundToLong() == Long.MAX_VALUE` when `x > Long.MAX_VALUE`
 *     - `x.roundToLong() == Long.MIN_VALUE` when `x < Long.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public fun Double.roundToLong(): Long = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)



// ================ Float Math ========================================

/** Computes the sine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sin(a: Float): Float = nativeMath.sin(a.toDouble()).toFloat()

/** Computes the cosine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun cos(a: Float): Float = nativeMath.cos(a.toDouble()).toFloat()

/** Computes the tangent of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun tan(a: Float): Float = nativeMath.tan(a.toDouble()).toFloat()

/**
 * Computes the arc sine of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun asin(a: Float): Float = nativeMath.asin(a.toDouble()).toFloat()

/**
 * Computes the arc cosine of the value [a];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun acos(a: Float): Float = nativeMath.acos(a.toDouble()).toFloat()

/**
 * Computes the arc tangent of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *     - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun atan(a: Float): Float = nativeMath.atan(a.toDouble()).toFloat()

/**
 * Returns the angle `theta` of the polar coordinates `(r, theta)` that correspond
 * to the rectangular coordinates `(x, y)` by computing the arc tangent of the value [y] / [x];
 * the returned value is an angle in the range from `-PI` to `PI` radians.
 *
 * Special cases:
 *     - `atan2(0.0, 0.0)` is `0.0`
 *     - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
 *     - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
 *     - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
 *     - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
 *     - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
 *     - `atan2(+Inf, x)` is `PI/2` for finite `x`y
 *     - `atan2(-Inf, x)` is `-PI/2` for finite `x`
 *     - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun atan2(y: Float, x: Float): Float = nativeMath.atan2(y.toDouble(), x.toDouble()).toFloat()

/**
 * Computes the hyperbolic sine of the value [a].
 *
 * Special cases:
 *
 *     - `sinh(NaN)` is `NaN`
 *     - `sinh(+Inf)` is `+Inf`
 *     - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sinh(a: Float): Float = nativeMath.sinh(a.toDouble()).toFloat()

/**
 * Computes the hyperbolic cosine of the value [a].
 *
 * Special cases:
 *
 *     - `cosh(NaN)` is `NaN`
 *     - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun cosh(a: Float): Float = nativeMath.cosh(a.toDouble()).toFloat()

/**
 * Computes the hyperbolic tangent of the value [a].
 *
 * Special cases:
 *
 *     - `tanh(NaN)` is `NaN`
 *     - `tanh(+Inf)` is `1.0`
 *     - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun tanh(a: Float): Float = nativeMath.tanh(a.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic sine of the value [a].
 *
 * The returned value is `x` such that `sinh(x) == a`.
 *
 * Special cases:
 *
 *     - `asinh(NaN)` is `NaN`
 *     - `asinh(+Inf)` is `+Inf`
 *     - `asinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun asinh(a: Float): Float = asinh(a.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic cosine of the value [a].
 *
 * The returned value is positive `x` such that `cosh(x) == a`.
 *
 * Special cases:
 *
 *     - `acosh(NaN)` is `NaN`
 *     - `acosh(x)` is `NaN` when `x < 1`
 *     - `acosh(+Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun acosh(a: Float): Float = acosh(a.toDouble()).toFloat()

/**
 * Computes the inverse hyperbolic tangent of the value [a].
 *
 * The returned value is `x` such that `tanh(x) == a`.
 *
 * Special cases:
 *
 *     - `tanh(NaN)` is `NaN`
 *     - `tanh(x)` is `NaN` when `x > 1` or `x < -1`
 *     - `tanh(1.0)` is `+Inf`
 *     - `tanh(-1.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun atanh(a: Float): Float = atanh(a.toDouble()).toFloat()

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *     - returns `+Inf` if any of arguments is infinite
 *     - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun hypot(x: Float, y: Float): Float = nativeMath.hypot(x.toDouble(), y.toDouble()).toFloat()

/**
 * Computes the positive square root of the value [a].
 *
 * Special cases:
 *     - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sqrt(a: Float): Float = nativeMath.sqrt(a.toDouble()).toFloat()

/**
 * Computes Euler's number `e` raised to the power of the value [a].
 *
 * Special cases:
 *     - `exp(NaN)` is `NaN`
 *     - `exp(+Inf)` is `+Inf`
 *     - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun exp(a: Float): Float = nativeMath.exp(a.toDouble()).toFloat()

/**
 * Computes `exp(a) - 1`.
 *
 * This function can be implemented to produce more precise result for [a] near zero.
 *
 * Special cases:
 *     - `expm1(NaN)` is `NaN`
 *     - `expm1(+Inf)` is `+Inf`
 *     - `expm1(-Inf)` is `-1.0`
 *
 * @see [exp] function.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun expm1(a: Float): Float = nativeMath.expm1(a.toDouble()).toFloat()

/**
 * Computes the logarithm of the value [a] to the given [base].
 *
 * Special cases:
 *     - `log(a, b)` is `NaN` if either `a` or `b` are `NaN`
 *     - `log(a, b)` is `NaN` when `a < 0` or `b <= 0` or `b == 1.0`
 *     - `log(+Inf, +Inf)` is `NaN`
 *     - `log(+Inf, b)` is `+Inf` for `b > 1` and `-Inf` for `b < 1`
 *     - `log(0.0, b)` is `-Inf` for `b > 1` and `+Inf` for `b > 1`
 */
@SinceKotlin("1.2")
public fun log(a: Float, base: Float): Float {
    if (base <= 0.0F || base == 1.0F) return Float.NaN
    return (nativeMath.log(a.toDouble()) / nativeMath.log(base.toDouble())).toFloat()
}

/**
 * Computes the natural logarithm (base `E`) of the value [a].
 *
 * Special cases:
 *     - `ln(NaN)` is `NaN`
 *     - `ln(x)` is `NaN` when `x < 0.0`
 *     - `ln(+Inf)` is `+Inf`
 *     - `ln(0.0)` is `-Inf`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ln(a: Float): Float = nativeMath.log(a.toDouble()).toFloat()

/**
 * Computes the common logarithm (base 10) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun log10(a: Float): Float = nativeMath.log10(a.toDouble()).toFloat()

/**
 * Computes the binary logarithm (base 2) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public fun log2(a: Float): Float = (nativeMath.log(a.toDouble()) / LN2).toFloat()

/**
 * Computes `ln(a + 1)`.
 *
 * This function can be implemented to produce more precise result for [a] near zero.
 *
 * Special cases:
 *     - `ln1p(NaN)` is `NaN`
 *     - `ln1p(x)` is `NaN` where `x < -1.0`
 *     - `ln1p(-1.0)` is `-Inf`
 *     - `ln1p(+Inf)` is `+Inf`
 *
 * @see [ln] function
 * @see [expm1] function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ln1p(a: Float): Float = nativeMath.log1p(a.toDouble()).toFloat()

/**
 * Rounds the given value [a] to an integer towards positive infinity.

 * @return the smallest Float value that is greater than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun ceil(a: Float): Float = nativeMath.ceil(a.toDouble()).toFloat()

/**
 * Rounds the given value [a] to an integer towards negative infinity.

 * @return the largest Float value that is smaller than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun floor(a: Float): Float = nativeMath.floor(a.toDouble()).toFloat()

/**
 * Rounds the given value [a] to an integer towards zero.
 *
 * @return the value [a] having its fractional part truncated.
 *
 * Special cases:
 *     - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public fun truncate(a: Float): Float = when {
    a.isNaN() || a.isInfinite() -> a
    a > 0 -> floor(a)
    else -> ceil(a)
}

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun round(a: Float): Float = nativeMath.rint(a.toDouble()).toFloat()


/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Float]
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun abs(a: Float): Float = nativeMath.abs(a)

/**
 * Returns the sign of the given value [a]:
 *     - `-1.0` if the value is negative,
 *     - zero if the value is zero,
 *     - `1.0` if the value is positive
 *
 * Special case:
 *     - `sign(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun sign(a: Float): Float = nativeMath.signum(a)



/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Float, b: Float): Float = nativeMath.min(a, b)
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Float, b: Float): Float = nativeMath.max(a, b)

// extensions


/**
 * Raises this value to the power [other].
 *
 * Special cases:
 *     - `x.pow(0.0)` is `1.0`
 *     - `x.pow(1.0) == x`
 *     - `x.pow(NaN)` is `NaN`
 *     - `NaN.pow(x)` is `NaN` for `x != 0.0`
 *     - `x.pow(Inf)` is `NaN` for `abs(x) == 1.0`
 *     - `x.pow(y)` is `NaN` for `x < 0` and `y` is finite and not an integer
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.pow(other: Float): Float = nativeMath.pow(this.toDouble(), other.toDouble()).toFloat()

/**
 * Raises this value to the integer power [other].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.pow(other: Int): Float = nativeMath.pow(this.toDouble(), other.toDouble()).toFloat()

/**
 * Computes the remainder of division of this value by the [other] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * other)` where `q` is the quotient of division rounded to the nearest integer,
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
public inline fun Float.IEEErem(other: Float): Float = nativeMath.IEEEremainder(this.toDouble(), other.toDouble()).toFloat()

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Float.absoluteValue: Float get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *     - `-1.0` if the value is negative,
 *     - zero if the value is zero,
 *     - `1.0` if the value is positive
 *
 * Special case:
 *     - `NaN.sign` is `NaN`
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Float.sign: Float get() = nativeMath.signum(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.withSign(sign: Float): Float = nativeMath.copySign(this, sign)
/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.withSign(sign: Int): Float = nativeMath.copySign(this, sign.toFloat())

/**
 * Returns the ulp of this value.
 *
 * An ulp is a positive distance between this value and the next nearest [Float] value larger in magnitude.
 *
 * Special Cases:
 *     - `NaN.ulp` is `NaN`
 *     - `x.ulp` is `+Inf` when `x` is `+Inf` or `-Inf`
 *     - `0.0.ulp` is `Float.NIN_VALUE`
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
 *     - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *     - `x.nextTowards(x) == x`
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
 *     - `x.roundToInt() == Int.MAX_VALUE` when `x > Int.MAX_VALUE`
 *     - `x.roundToInt() == Int.MIN_VALUE` when `x < Int.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public fun Float.roundToInt(): Int = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)

/**
 * Rounds this [Float] value to the nearest integer and converts the result to [Long].
 * Ties are rounded towards positive infinity.
 *
 * Special cases:
 *     - `x.roundToLong() == Long.MAX_VALUE` when `x > Long.MAX_VALUE`
 *     - `x.roundToLong() == Long.MIN_VALUE` when `x < Long.MIN_VALUE`
 *
 * @throws IllegalArgumentException when this value is `NaN`
 */
@SinceKotlin("1.2")
public fun Float.roundToLong(): Long = toDouble().roundToLong()



// ================== Integer math functions =====================================

/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(Int.MIN_VALUE)` is `Int.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Int]
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun abs(a: Int): Int = nativeMath.abs(a)

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Int, b: Int): Int = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Int, b: Int): Int = nativeMath.max(a, b)

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Int.absoluteValue: Int get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *     - `-1` if the value is negative,
 *     - `0` if the value is zero,
 *     - `1` if the value is positive
 */
@SinceKotlin("1.2")
public val Int.sign: Int get() = when {
    this < 0 -> -1
    this > 0 -> 1
    else -> 0
}



/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(Long.MIN_VALUE)` is `Long.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Long]
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun abs(a: Long): Long = nativeMath.abs(a)

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Long, b: Long): Long = nativeMath.min(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Long, b: Long): Long = nativeMath.max(a, b)

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Long.absoluteValue: Long get() = nativeMath.abs(this)

/**
 * Returns the sign of this value:
 *     - `-1` if the value is negative,
 *     - `0` if the value is zero,
 *     - `1` if the value is positive
 */
@SinceKotlin("1.2")
public val Long.sign: Int get() = when {
    this < 0 -> -1
    this > 0 -> 1
    else -> 0
}

