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
@file:Suppress("DEPRECATION")
package kotlin.math


import kotlin.internal.InlineOnly
import kotlin.js.Math as nativeMath

// constants, can't use them from nativeMath as they are not constants there

/** Ratio of the circumference of a circle to its diameter, approximately 3.14159. */
@SinceKotlin("1.2")
public const val PI: Double = 3.141592653589793
/** Base of the natural logarithms, approximately 2.71828. */
@SinceKotlin("1.2")
public const val E: Double = 2.718281828459045

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
public inline fun asinh(a: Double): Double = nativeMath.asinh(a)

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
public inline fun acosh(a: Double): Double = nativeMath.acosh(a)

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
public inline fun atanh(a: Double): Double = nativeMath.atanh(a)

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
@InlineOnly
public inline fun log2(a: Double): Double = nativeMath.log2(a)

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
public inline fun ceil(a: Double): Double = nativeMath.ceil(a).unsafeCast<Double>() // TODO: Remove unsafe cast after removing public js.math

/**
 * Rounds the given value [a] to an integer towards negative infinity.

 * @return the largest double value that is smaller than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun floor(a: Double): Double = nativeMath.floor(a).unsafeCast<Double>()

/**
 * Rounds the given value [a] to an integer towards zero.
 *
 * @return the value [a] having its fractional part truncated.
 *
 * Special cases:
 *     - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun truncate(a: Double): Double = nativeMath.trunc(a)

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public fun round(a: Double): Double {
    if (a % 0.5 != 0.0) {
        return nativeMath.round(a).unsafeCast<Double>()
    }
    val floor = floor(a)
    return if (floor % 2 == 0.0) floor else ceil(a)
}

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
public inline fun sign(a: Double): Double = nativeMath.sign(a)


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
public inline val Double.sign: Double get() = nativeMath.sign(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public fun Double.withSign(sign: Double): Double {
    val thisSignBit = js("Kotlin").doubleSignBit(this).unsafeCast<Int>()
    val newSignBit = js("Kotlin").doubleSignBit(sign).unsafeCast<Int>()
    return if (thisSignBit == newSignBit) this else -this
}

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Double.withSign(sign: Int): Double = this.withSign(sign.toDouble())

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
public val Double.ulp: Double get() = when {
    this < 0 -> (-this).ulp
    this.isNaN() || this == Double.POSITIVE_INFINITY -> this
    this == Double.MAX_VALUE -> this - this.nextDown()
    else -> this.nextUp() - this
}

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
public fun Double.nextUp(): Double = when {
    this.isNaN() || this == Double.POSITIVE_INFINITY -> this
    this == 0.0 -> Double.MIN_VALUE
    else -> Double.fromBits(this.toRawBits() + if (this > 0) 1 else -1)
}

/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
public fun Double.nextDown(): Double = when {
    this.isNaN() || this == Double.NEGATIVE_INFINITY -> this
    this == 0.0 -> -Double.MIN_VALUE
    else -> Double.fromBits(this.toRawBits() + if (this > 0) -1 else 1)
}


/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *     - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *     - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
public fun Double.nextTowards(to: Double): Double = when {
    this.isNaN() || to.isNaN() -> Double.NaN
    to == this -> to
    to > this -> this.nextUp()
    else /* to < this */-> this.nextDown()
}


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
    else -> nativeMath.round(this).unsafeCast<Double>().toInt()
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
public fun Double.roundToLong(): Long = when {
    isNaN() -> throw IllegalArgumentException("Cannot round NaN value.")
    this > Long.MAX_VALUE -> Long.MAX_VALUE
    this < Long.MIN_VALUE -> Long.MIN_VALUE
    else -> nativeMath.round(this).unsafeCast<Double>().toLong()
}




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
public inline fun asinh(a: Float): Float = nativeMath.asinh(a.toDouble()).toFloat()

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
public inline fun acosh(a: Float): Float = nativeMath.acosh(a.toDouble()).toFloat()

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
public inline fun atanh(a: Float): Float = nativeMath.atanh(a.toDouble()).toFloat()

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
@InlineOnly
public inline fun log(a: Float, base: Float): Float = log(a.toDouble(), base.toDouble()).toFloat()

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
@InlineOnly
public inline fun log2(a: Float): Float = nativeMath.log2(a.toDouble()).toFloat()

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
@InlineOnly
public inline fun truncate(a: Float): Float = truncate(a.toDouble()).toFloat()

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun round(a: Float): Float = round(a.toDouble()).toFloat()


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
public inline fun abs(a: Float): Float = nativeMath.abs(a.toDouble()).toFloat()

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
public inline fun sign(a: Float): Float = nativeMath.sign(a.toDouble()).toFloat()



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
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
@InlineOnly
public inline val Float.absoluteValue: Float get() = nativeMath.abs(this.toDouble()).toFloat()

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
public inline val Float.sign: Float get() = nativeMath.sign(this.toDouble()).toFloat()

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.withSign(sign: Float): Float = this.toDouble().withSign(sign.toDouble()).toFloat()
/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun Float.withSign(sign: Int): Float = this.toDouble().withSign(sign.toDouble()).toFloat()


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
@InlineOnly
public inline fun Float.roundToInt(): Int = toDouble().roundToInt()

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
@InlineOnly
public inline fun Float.roundToLong(): Long = toDouble().roundToLong()




/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(Int.MIN_VALUE)` is `Int.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Int]
 */
// TODO: remove manual 'or' when KT-19290 is fixed
@SinceKotlin("1.2")
public fun abs(a: Int): Int = if (a < 0) (-a or 0) else a

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Int, b: Int): Int = minOf(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Int, b: Int): Int = maxOf(a, b)

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
public inline val Int.absoluteValue: Int get() = abs(this)

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
public fun abs(a: Long): Long = if (a < 0) -a else a

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun min(a: Long, b: Long): Long = minOf(a, b)

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
@InlineOnly
public inline fun max(a: Long, b: Long): Long = maxOf(a, b)

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
public inline val Long.absoluteValue: Long get() = abs(this)

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


