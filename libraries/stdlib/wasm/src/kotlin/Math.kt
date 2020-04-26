/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.math


// region ================ Double Math ========================================

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun sin(x: Double): Double = TODO("Wasm stdlib: Math")

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun cos(x: Double): Double = TODO("Wasm stdlib: Math")

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun tan(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
public actual fun asin(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
public actual fun acos(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun atan(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun atan2(y: Double, x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
public actual fun sinh(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
public actual fun cosh(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
public actual fun tanh(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun asinh(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun acosh(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun atanh(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
public actual fun hypot(x: Double, y: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun sqrt(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
public actual fun exp(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun expm1(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun log(x: Double, base: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun ln(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log10(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log2(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun ln1p(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards positive infinity.

 * @return the smallest double value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun ceil(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards negative infinity.

 * @return the largest double value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun floor(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun truncate(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun round(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Double]
 */
@SinceKotlin("1.2")
public actual fun abs(x: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun sign(x: Double): Double = TODO("Wasm stdlib: Math")


/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun min(a: Double, b: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun max(a: Double, b: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun Double.pow(x: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public actual fun Double.pow(n: Int): Double = TODO("Wasm stdlib: Math")

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Double.absoluteValue: Double get() = TODO("Wasm stdlib: Math")

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
public actual val Double.sign: Double get() = TODO("Wasm stdlib: Math")

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public actual fun Double.withSign(sign: Double): Double = TODO("Wasm stdlib: Math")

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public actual fun Double.withSign(sign: Int): Double = TODO("Wasm stdlib: Math")

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
public actual val Double.ulp: Double get() = TODO("Wasm stdlib: Math")

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
public actual fun Double.nextUp(): Double = TODO("Wasm stdlib: Math")

/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
public actual fun Double.nextDown(): Double = TODO("Wasm stdlib: Math")

/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *   - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *   - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
public actual fun Double.nextTowards(to: Double): Double = TODO("Wasm stdlib: Math")

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
public actual fun Double.roundToInt(): Int = TODO("Wasm stdlib: Math")

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
public actual fun Double.roundToLong(): Long = TODO("Wasm stdlib: Math")

// endregion



// region ================ Float Math ========================================

/** Computes the sine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun sin(x: Float): Float = TODO("Wasm stdlib: Math")

/** Computes the cosine of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun cos(x: Float): Float = TODO("Wasm stdlib: Math")

/** Computes the tangent of the angle [x] given in radians.
 *
 *  Special cases:
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun tan(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the arc sine of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
public actual fun asin(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the arc cosine of the value [x];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(x)` is `NaN`, when `abs(x) > 1` or x is `NaN`
 */
@SinceKotlin("1.2")
public actual fun acos(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the arc tangent of the value [x];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *   - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun atan(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun atan2(y: Float, x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic sine of the value [x].
 *
 * Special cases:
 *   - `sinh(NaN)` is `NaN`
 *   - `sinh(+Inf)` is `+Inf`
 *   - `sinh(-Inf)` is `-Inf`
 */
@SinceKotlin("1.2")
public actual fun sinh(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic cosine of the value [x].
 *
 * Special cases:
 *   - `cosh(NaN)` is `NaN`
 *   - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
public actual fun cosh(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the hyperbolic tangent of the value [x].
 *
 * Special cases:
 *   - `tanh(NaN)` is `NaN`
 *   - `tanh(+Inf)` is `1.0`
 *   - `tanh(-Inf)` is `-1.0`
 */
@SinceKotlin("1.2")
public actual fun tanh(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun asinh(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun acosh(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun atanh(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *   - returns `+Inf` if any of arguments is infinite
 *   - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
public actual fun hypot(x: Float, y: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the positive square root of the value [x].
 *
 * Special cases:
 *   - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
public actual fun sqrt(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes Euler's number `e` raised to the power of the value [x].
 *
 * Special cases:
 *   - `exp(NaN)` is `NaN`
 *   - `exp(+Inf)` is `+Inf`
 *   - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
public actual fun exp(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun expm1(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun log(x: Float, base: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun ln(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the common logarithm (base 10) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log10(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Computes the binary logarithm (base 2) of the value [x].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public actual fun log2(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun ln1p(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards positive infinity.

 * @return the smallest Float value that is greater than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun ceil(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards negative infinity.

 * @return the largest Float value that is smaller than or equal to the given value [x] and is a mathematical integer.
 *
 * Special cases:
 *   - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun floor(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] to an integer towards zero.
 *
 * @return the value [x] having its fractional part truncated.
 *
 * Special cases:
 *   - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun truncate(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Rounds the given value [x] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *   - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public actual fun round(x: Float): Float = TODO("Wasm stdlib: Math")


/**
 * Returns the absolute value of the given value [x].
 *
 * Special cases:
 *   - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Float]
 */
@SinceKotlin("1.2")
public actual fun abs(x: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun sign(x: Float): Float = TODO("Wasm stdlib: Math")



/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun min(a: Float, b: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public actual fun max(a: Float, b: Float): Float = TODO("Wasm stdlib: Math")

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
public actual fun Float.pow(x: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Raises this value to the integer power [n].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public actual fun Float.pow(n: Int): Float = TODO("Wasm stdlib: Math")

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Float.absoluteValue: Float get() = TODO("Wasm stdlib: Math")

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
public actual val Float.sign: Float get() = TODO("Wasm stdlib: Math")

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public actual fun Float.withSign(sign: Float): Float = TODO("Wasm stdlib: Math")

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public actual fun Float.withSign(sign: Int): Float = TODO("Wasm stdlib: Math")


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
public actual fun Float.roundToInt(): Int = TODO("Wasm stdlib: Math")

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
public actual fun Float.roundToLong(): Long = TODO("Wasm stdlib: Math")


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
public actual fun abs(n: Int): Int = TODO("Wasm stdlib: Math")

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public actual fun min(a: Int, b: Int): Int = TODO("Wasm stdlib: Math")

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public actual fun max(a: Int, b: Int): Int = TODO("Wasm stdlib: Math")

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Int.absoluteValue: Int get() = TODO("Wasm stdlib: Math")

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Int.sign: Int get() = TODO("Wasm stdlib: Math")



/**
 * Returns the absolute value of the given value [n].
 *
 * Special cases:
 *   - `abs(Long.MIN_VALUE)` is `Long.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Long]
 */
@SinceKotlin("1.2")
public actual fun abs(n: Long): Long = TODO("Wasm stdlib: Math")

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public actual fun min(a: Long, b: Long): Long = TODO("Wasm stdlib: Math")

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public actual fun max(a: Long, b: Long): Long = TODO("Wasm stdlib: Math")

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *   - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public actual val Long.absoluteValue: Long get() = TODO("Wasm stdlib: Math")

/**
 * Returns the sign of this value:
 *   - `-1` if the value is negative,
 *   - `0` if the value is zero,
 *   - `1` if the value is positive
 */
@SinceKotlin("1.2")
public actual val Long.sign: Int get() = TODO("Wasm stdlib: Math")


// endregion
