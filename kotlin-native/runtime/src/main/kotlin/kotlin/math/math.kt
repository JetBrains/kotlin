/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.math

import kotlin.native.internal.GCUnsafeCall

// region ================ Double Math ========================================

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sin")
external public actual fun sin(x: Double): Double

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_cos")
external public actual fun cos(x: Double): Double

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_tan")
external public actual fun tan(x: Double): Double

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_asin")
external public actual fun asin(x: Double): Double

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *   - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_acos")
external public actual fun acos(x: Double): Double

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_atan")
external public actual fun atan(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_atan2")
external public actual fun atan2(y: Double, x: Double): Double

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sinh")
external public actual fun sinh(x: Double): Double

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_cosh")
external public actual fun cosh(x: Double): Double

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_tanh")
external public actual fun tanh(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_asinh")
external public actual fun asinh(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_acosh")
external public actual fun acosh(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_atanh")
external public actual fun atanh(x: Double): Double

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_hypot")
external public actual fun hypot(x: Double, y: Double): Double

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sqrt")
external public actual fun sqrt(x: Double): Double

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_exp")
external public actual fun exp(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_expm1")
external public actual fun expm1(x: Double): Double

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
    return ln(x) / ln(base)
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
@GCUnsafeCall("Kotlin_math_ln")
external public actual fun ln(x: Double): Double

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] actual function for special cases.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_log10")
external public actual fun log10(x: Double): Double

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] actual function for special cases.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_log2")
external public actual fun log2(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_ln1p")
external public actual fun ln1p(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_ceil")
external public actual fun ceil(x: Double): Double

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
@GCUnsafeCall("Kotlin_math_floor")
external public actual fun floor(x: Double): Double

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
 *
 * @sample samples.math.MathSamples.Doubles.round
 * @sample samples.math.MathSamples.Doubles.roundingModes
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_round")
external public actual fun round(x: Double): Double

/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Double]
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_abs")
external public actual fun abs(x: Double): Double

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
public actual fun sign(x: Double): Double = when {
    x.isNaN() -> Double.NaN
    x > 0 -> 1.0
    x < 0 -> -1.0
    else -> x
}

/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun min(a: Double, b: Double): Double = when {
    a.isNaN() || b.isNaN() -> Double.NaN
    a == 0.0 && b == 0.0 -> if (a.signBit()) a else b   // -0.0 < +0.0
    else -> if (a < b) a else b
}
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun max(a: Double, b: Double): Double = when {
    a.isNaN() || b.isNaN() -> Double.NaN
    a == 0.0 && b == 0.0 -> if (!a.signBit()) a else b  // -0.0 < +0.0
    else -> if (a > b) a else b
}


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
@GCUnsafeCall("Kotlin_math_cbrt")
public external actual fun cbrt(x: Double): Double


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
@GCUnsafeCall("Kotlin_math_Double_pow")
external public actual fun Double.pow(x: Double): Double

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public actual fun Double.pow(n: Int): Double = pow(n.toDouble())

/**
 * Computes the remainder of division of this value by the [divisor] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * divisor)` where `q` is the quotient of division rounded to the nearest integer,
 * `q = round(this / other)`.
 *
 * Special cases:
 *   - `x.IEEErem(y)` is `NaN`, when `x` is `NaN` or `y` is `NaN` or `x` is `+Inf|-Inf` or `y` is zero.
 *   - `x.IEEErem(y) == x` when `x` is finite and `y` is infinite.
 *
 * @see round
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Double_IEEErem")
external public fun Double.IEEErem(divisor: Double): Double

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs actual function
 */
@SinceKotlin("1.2")
public actual val Double.absoluteValue: Double
    get() = abs(this)

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
public actual val Double.sign: Double
    get() = sign(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Double_withSign")
external public actual fun Double.withSign(sign: Double): Double

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public actual fun Double.withSign(sign: Int): Double = withSign(sign.toDouble())

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
public actual val Double.ulp: Double
    get() = when {
        isNaN() -> Double.NaN
        isInfinite() -> Double.POSITIVE_INFINITY
        this == Double.MAX_VALUE || this == -Double.MAX_VALUE -> 2.0.pow(971)
        else -> {
            val d = absoluteValue
            d.nextUp() - d
        }
    }

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Double_nextUp")
external public actual fun Double.nextUp(): Double
/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Double_nextDown")
external public actual fun Double.nextDown(): Double

/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Double_nextTowards")
external public actual fun Double.nextTowards(to: Double): Double

/**
 * Returns true if the sign of [this] value is negative and false otherwise
 */
@GCUnsafeCall("Kotlin_math_Double_signBit")
external private fun Double.signBit(): Boolean

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

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sinf")
external public actual fun sin(x: Float): Float

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_cosf")
external public actual fun cos(x: Float): Float

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_tanf")
external public actual fun tan(x: Float): Float

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_asinf")
external public actual fun asin(x: Float): Float

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *   - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_acosf")
external public actual fun acos(x: Float): Float

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_atanf")
external public actual fun atan(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_atan2f")
external public actual fun atan2(y: Float, x: Float): Float

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sinhf")
external public actual fun sinh(x: Float): Float

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_coshf")
external public actual fun cosh(x: Float): Float

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_tanhf")
external public actual fun tanh(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_asinhf")
external public actual fun asinh(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_acoshf")
external public actual fun acosh(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_atanhf")
external public actual fun atanh(x: Float): Float

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_hypotf")
external public actual fun hypot(x: Float, y: Float): Float

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_sqrtf")
external public actual fun sqrt(x: Float): Float

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_expf")
external public actual fun exp(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_expm1f")
external public actual fun expm1(x: Float): Float

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
    return ln(x) / ln(base)
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
@GCUnsafeCall("Kotlin_math_lnf")
external public actual fun ln(x: Float): Float

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_log10f")
external public actual fun log10(x: Float): Float

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_log2f")
external public actual fun log2(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_ln1pf")
external public actual fun ln1p(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_ceilf")
external public actual fun ceil(x: Float): Float

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
@GCUnsafeCall("Kotlin_math_floorf")
external public actual fun floor(x: Float): Float

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
 *
 * @sample samples.math.MathSamples.Floats.round
 * @sample samples.math.MathSamples.Floats.roundingModes
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_roundf")
external public actual fun round(x: Float): Float


/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Float]
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_absf")
external public actual fun abs(x: Float): Float

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
public actual fun sign(x: Float): Float = when {
    x.isNaN() -> Float.NaN
    x > 0 -> 1.0f
    x < 0 -> -1.0f
    else -> x
}

/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun min(a: Float, b: Float): Float = when {
    a.isNaN() || b.isNaN() -> Float.NaN
    a == 0.0f && b == 0.0f -> if (a.signBit()) a else b   // -0.0 < +0.0
    else -> if (a < b) a else b
}
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun max(a: Float, b: Float): Float = when {
    a.isNaN() || b.isNaN() -> Float.NaN
    a == 0.0f && b == 0.0f -> if (!a.signBit()) a else b   // -0.0 < +0.0
    else -> if (a > b) a else b
}


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
@GCUnsafeCall("Kotlin_math_cbrtf")
public external actual fun cbrt(x: Float): Float


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
@GCUnsafeCall("Kotlin_math_Float_pow")
external public actual fun Float.pow(x: Float): Float

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public actual fun Float.pow(n: Int): Float = pow(n.toFloat())

/**
 * Computes the remainder of division of this value by the [divisor] value according to the IEEE 754 standard.
 *
 * The result is computed as `r = this - (q * divisor)` where `q` is the quotient of division rounded to the nearest integer,
 * `q = round(this / other)`.
 *
 * Special cases:
 *   - `x.IEEErem(y)` is `NaN`, when `x` is `NaN` or `y` is `NaN` or `x` is `+Inf|-Inf` or `y` is zero.
 *   - `x.IEEErem(y) == x` when `x` is finite and `y` is infinite.
 *
 * @see round
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Float_IEEErem")
external public fun Float.IEEErem(divisor: Float): Float

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Float.absoluteValue: Float
    get() = abs(this)

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
public actual val Float.sign: Float
    get() = sign(this)

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Float_withSign")
external public actual fun Float.withSign(sign: Float): Float
/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public actual fun Float.withSign(sign: Int): Float = withSign(sign.toFloat())

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
public val Float.ulp: Float
    get() = when {
        isNaN() -> Float.NaN
        isInfinite() -> Float.POSITIVE_INFINITY
        this == Float.MAX_VALUE || this == -Float.MAX_VALUE -> 2.0f.pow(104)
        else -> {
            val d = absoluteValue
            d.nextUp() - d
        }
    }

/**
 * Returns the [Float] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Float_nextUp")
external public fun Float.nextUp(): Float
/**
 * Returns the [Float] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Float_nextDown")
external public fun Float.nextDown(): Float

/**
 * Returns the [Float] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_Float_nextTowards")
external public fun Float.nextTowards(to: Float): Float

/**
 * Returns true if the sign of [this] value is negative and false otherwise
 */
@GCUnsafeCall("Kotlin_math_Float_signBit")
external private fun Float.signBit(): Boolean

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
 * Returns the absolute value of the given value [n].
 *
 * Special cases:
 *   - `abs(Int.MIN_VALUE)` is `Int.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Int]
 */
@SinceKotlin("1.2")
@GCUnsafeCall("Kotlin_math_absi")
external public actual fun abs(n: Int): Int

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public actual fun min(a: Int, b: Int): Int = if (a < b) a else b

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public actual fun max(a: Int, b: Int): Int = if (a > b) a else b

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs actual function
 */
@SinceKotlin("1.2")
public actual val Int.absoluteValue: Int
    get() = abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Int.sign: Int
    // implementation note: use a naive implementation here as LLVM is capable of optimizing it later.
    get() = when {
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
@GCUnsafeCall("Kotlin_math_absl")
external public actual fun abs(n: Long): Long

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public actual fun min(a: Long, b: Long): Long = if (a < b) a else b

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public actual fun max(a: Long, b: Long): Long = if (a > b) a else b

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Long.absoluteValue: Long
    get() = abs(this)

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Long.sign: Int
    // implementation note: use a naive implementation here as LLVM is capable of optimizing it later.
    get() = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }

// endregion
