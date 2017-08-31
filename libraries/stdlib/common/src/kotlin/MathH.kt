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

package kotlin.math



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
public header fun sin(a: Double): Double

/** Computes the cosine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun cos(a: Double): Double

/** Computes the tangent of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun tan(a: Double): Double

/**
 * Computes the arc sine of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
public header fun asin(a: Double): Double

/**
 * Computes the arc cosine of the value [a];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
public header fun acos(a: Double): Double

/**
 * Computes the arc tangent of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *     - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun atan(a: Double): Double

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
public header fun atan2(y: Double, x: Double): Double

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
public header fun sinh(a: Double): Double

/**
 * Computes the hyperbolic cosine of the value [a].
 *
 * Special cases:
 *
 *     - `cosh(NaN)` is `NaN`
 *     - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
public header fun cosh(a: Double): Double

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
public header fun tanh(a: Double): Double

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
public header fun asinh(a: Double): Double

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
public header fun acosh(a: Double): Double

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
public header fun atanh(a: Double): Double

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *     - returns `+Inf` if any of arguments is infinite
 *     - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
public header fun hypot(x: Double, y: Double): Double

/**
 * Computes the positive square root of the value [a].
 *
 * Special cases:
 *     - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
public header fun sqrt(a: Double): Double

/**
 * Computes Euler's number `e` raised to the power of the value [a].
 *
 * Special cases:
 *     - `exp(NaN)` is `NaN`
 *     - `exp(+Inf)` is `+Inf`
 *     - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
public header fun exp(a: Double): Double

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
public header fun expm1(a: Double): Double

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
public header fun log(a: Double, base: Double): Double

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
public header fun ln(a: Double): Double

/**
 * Computes the common logarithm (base 10) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public header fun log10(a: Double): Double

/**
 * Computes the binary logarithm (base 2) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public header fun log2(a: Double): Double

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
public header fun ln1p(a: Double): Double

/**
 * Rounds the given value [a] to an integer towards positive infinity.

 * @return the smallest double value that is greater than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun ceil(a: Double): Double

/**
 * Rounds the given value [a] to an integer towards negative infinity.

 * @return the largest double value that is smaller than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun floor(a: Double): Double

/**
 * Rounds the given value [a] to an integer towards zero.
 *
 * @return the value [a] having its fractional part truncated.
 *
 * Special cases:
 *     - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun truncate(a: Double): Double

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun round(a: Double): Double

/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Double]
 */
@SinceKotlin("1.2")
public header fun abs(a: Double): Double

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
public header fun sign(a: Double): Double


/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public header fun min(a: Double, b: Double): Double
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public header fun max(a: Double, b: Double): Double

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
public header fun Double.pow(other: Double): Double

/**
 * Raises this value to the integer power [other].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public header fun Double.pow(other: Int): Double

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public header val Double.absoluteValue: Double

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
public header val Double.sign: Double

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public header fun Double.withSign(sign: Double): Double

/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public header fun Double.withSign(sign: Int): Double

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
public header val Double.ulp: Double

/**
 * Returns the [Double] value nearest to this value in direction of positive infinity.
 */
@SinceKotlin("1.2")
public header fun Double.nextUp(): Double
/**
 * Returns the [Double] value nearest to this value in direction of negative infinity.
 */
@SinceKotlin("1.2")
public header fun Double.nextDown(): Double

/**
 * Returns the [Double] value nearest to this value in direction from this value towards the value [to].
 *
 * Special cases:
 *     - `x.nextTowards(y)` is `NaN` if either `x` or `y` are `NaN`
 *     - `x.nextTowards(x) == x`
 *
 */
@SinceKotlin("1.2")
public header fun Double.nextTowards(to: Double): Double

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
public header fun Double.roundToInt(): Int

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
public header fun Double.roundToLong(): Long




// ================ Float Math ========================================

/** Computes the sine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `sin(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun sin(a: Float): Float

/** Computes the cosine of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `cos(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun cos(a: Float): Float

/** Computes the tangent of the angle [a] given in radians.
 *
 *  Special cases:
 *
 *   - `tan(NaN|+Inf|-Inf)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun tan(a: Float): Float

/**
 * Computes the arc sine of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *    - `asin(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
public header fun asin(a: Float): Float

/**
 * Computes the arc cosine of the value [a];
 * the returned value is an angle in the range from `0.0` to `PI` radians.
 *
 * Special cases:
 *    - `acos(v)` is `NaN`, when `abs(v) > 1` or v is `NaN`
 */
@SinceKotlin("1.2")
public header fun acos(a: Float): Float

/**
 * Computes the arc tangent of the value [a];
 * the returned value is an angle in the range from `-PI/2` to `PI/2` radians.
 *
 * Special cases:
 *     - `atan(NaN)` is `NaN`
 */
@SinceKotlin("1.2")
public header fun atan(a: Float): Float

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
public header fun atan2(y: Float, x: Float): Float

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
public header fun sinh(a: Float): Float

/**
 * Computes the hyperbolic cosine of the value [a].
 *
 * Special cases:
 *
 *     - `cosh(NaN)` is `NaN`
 *     - `cosh(+Inf|-Inf)` is `+Inf`
 */
@SinceKotlin("1.2")
public header fun cosh(a: Float): Float

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
public header fun tanh(a: Float): Float

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
public header fun asinh(a: Float): Float

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
public header fun acosh(a: Float): Float

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
public header fun atanh(a: Float): Float

/**
 * Computes `sqrt(x^2 + y^2)` without intermediate overflow or underflow.
 *
 * Special cases:
 *     - returns `+Inf` if any of arguments is infinite
 *     - returns `NaN` if any of arguments is `NaN` and the other is not infinite
 */
@SinceKotlin("1.2")
public header fun hypot(x: Float, y: Float): Float

/**
 * Computes the positive square root of the value [a].
 *
 * Special cases:
 *     - `sqrt(x)` is `NaN` when `x < 0` or `x` is `NaN`
 */
@SinceKotlin("1.2")
public header fun sqrt(a: Float): Float

/**
 * Computes Euler's number `e` raised to the power of the value [a].
 *
 * Special cases:
 *     - `exp(NaN)` is `NaN`
 *     - `exp(+Inf)` is `+Inf`
 *     - `exp(-Inf)` is `0.0`
 */
@SinceKotlin("1.2")
public header fun exp(a: Float): Float

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
public header fun expm1(a: Float): Float

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
public header fun log(a: Float, base: Float): Float

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
public header fun ln(a: Float): Float

/**
 * Computes the common logarithm (base 10) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public header fun log10(a: Float): Float

/**
 * Computes the binary logarithm (base 2) of the value [a].
 *
 * @see [ln] function for special cases.
 */
@SinceKotlin("1.2")
public header fun log2(a: Float): Float

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
public header fun ln1p(a: Float): Float

/**
 * Rounds the given value [a] to an integer towards positive infinity.

 * @return the smallest Float value that is greater than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `ceil(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun ceil(a: Float): Float

/**
 * Rounds the given value [a] to an integer towards negative infinity.

 * @return the largest Float value that is smaller than the given value [a] and is a mathematical integer.
 *
 * Special cases:
 *     - `floor(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun floor(a: Float): Float

/**
 * Rounds the given value [a] to an integer towards zero.
 *
 * @return the value [a] having its fractional part truncated.
 *
 * Special cases:
 *     - `truncate(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun truncate(a: Float): Float

/**
 * Rounds the given value [a] towards the closest integer with ties rounded towards even integer.
 *
 * Special cases:
 *     - `round(x)` is `x` where `x` is `NaN` or `+Inf` or `-Inf` or already a mathematical integer.
 */
@SinceKotlin("1.2")
public header fun round(a: Float): Float


/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(NaN)` is `NaN`
 *
 * @see absoluteValue extension property for [Float]
 */
@SinceKotlin("1.2")
public header fun abs(a: Float): Float

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
public header fun sign(a: Float): Float



/**
 * Returns the smaller of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public header fun min(a: Float, b: Float): Float
/**
 * Returns the greater of two values.
 *
 * If either value is `NaN`, then the result is `NaN`.
 */
@SinceKotlin("1.2")
public header fun max(a: Float, b: Float): Float

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
public header fun Float.pow(other: Float): Float

/**
 * Raises this value to the integer power [other].
 *
 * See the other overload of [pow] for details.
 */
@SinceKotlin("1.2")
public header fun Float.pow(other: Int): Float

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `NaN.absoluteValue` is `NaN`
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public header val Float.absoluteValue: Float

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
public header val Float.sign: Float

/**
 * Returns this value with the sign bit same as of the [sign] value.
 *
 * If [sign] is `NaN` the sign of the result is undefined.
 */
@SinceKotlin("1.2")
public header fun Float.withSign(sign: Float): Float
/**
 * Returns this value with the sign bit same as of the [sign] value.
 */
@SinceKotlin("1.2")
public header fun Float.withSign(sign: Int): Float


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
public header fun Float.roundToInt(): Int

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
public header fun Float.roundToLong(): Long




/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(Int.MIN_VALUE)` is `Int.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Int]
 */
@SinceKotlin("1.2")
public header fun abs(a: Int): Int

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public header fun min(a: Int, b: Int): Int

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public header fun max(a: Int, b: Int): Int

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `Int.MIN_VALUE.absoluteValue` is `Int.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public header val Int.absoluteValue: Int

/**
 * Returns the sign of this value:
 *     - `-1` if the value is negative,
 *     - `0` if the value is zero,
 *     - `1` if the value is positive
 */
@SinceKotlin("1.2")
public header val Int.sign: Int



/**
 * Returns the absolute value of the given value [a].
 *
 * Special cases:
 *     - `abs(Long.MIN_VALUE)` is `Long.MIN_VALUE` due to an overflow
 *
 * @see absoluteValue extension property for [Long]
 */
@SinceKotlin("1.2")
public header fun abs(a: Long): Long

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.2")
public header fun min(a: Long, b: Long): Long

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.2")
public header fun max(a: Long, b: Long): Long

/**
 * Returns the absolute value of this value.
 *
 * Special cases:
 *     - `Long.MIN_VALUE.absoluteValue` is `Long.MIN_VALUE` due to an overflow
 *
 * @see abs function
 */
@SinceKotlin("1.2")
public header val Long.absoluteValue: Long

/**
 * Returns the sign of this value:
 *     - `-1` if the value is negative,
 *     - `0` if the value is zero,
 *     - `1` if the value is positive
 */
@SinceKotlin("1.2")
public header val Long.sign: Int




