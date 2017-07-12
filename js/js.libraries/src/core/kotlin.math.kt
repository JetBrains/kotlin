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


import kotlin.js.Math as nativeMath

// constants, can't use them from nativeMath as they are not constants there

/** Ratio of the circumference of a circle to its diameter, approximately 3.14159. */
public const val PI: Double = 3.141592653589793
/** Base of the natural logarithms, approximately 2.71828. */
public const val E: Double = 2.718281828459045

// Double

inline fun sin(a: Double): Double = nativeMath.sin(a)
inline fun cos(a: Double): Double = nativeMath.cos(a)
inline fun tan(a: Double): Double = nativeMath.tan(a)

inline fun asin(a: Double): Double = nativeMath.asin(a)
inline fun acos(a: Double): Double = nativeMath.acos(a)
inline fun atan(a: Double): Double = nativeMath.atan(a)
inline fun atan2(y: Double, x: Double): Double = nativeMath.atan2(y, x)

// TODO: Polyfill
/*
inline fun sinh(a: Double): Double = nativeMath.sinh(a)
inline fun cosh(a: Double): Double = nativeMath.cosh(a)
inline fun tanh(a: Double): Double = nativeMath.tanh(a)

inline fun hypot(x: Double, y: Double): Double = nativeMath.hypot(x, y)
*/

inline fun pow(a: Double, b: Double): Double = nativeMath.pow(a, b)
inline fun pow(a: Double, b: Int): Double = nativeMath.pow(a, b.toDouble())

inline fun sqrt(a: Double): Double = nativeMath.sqrt(a)

inline fun exp(a: Double): Double = nativeMath.exp(a)
// inline fun expm1(a: Double): Double = nativeMath.expm1(a) // polyfill

inline fun log(a: Double): Double = nativeMath.log(a)
fun log(a: Double, base: Double): Double = nativeMath.log(a) / nativeMath.log(base)
//inline fun log10(a: Double): Double = nativeMath.log10(a) // polyfill
//inline fun log1p(a: Double): Double = nativeMath.log1p(a) // polyfill

inline fun ceil(a: Double): Double = nativeMath.ceil(a).unsafeCast<Double>() // TODO: Remove unsafe cast after removing public js.math
inline fun floor(a: Double): Double = nativeMath.floor(a).unsafeCast<Double>()
inline fun truncate(a: Double): Double = nativeMath.trunc(a) // polyfill

// also as extension val [absoluteValue]
inline fun abs(a: Double): Double = nativeMath.abs(a)
// also as extension val [sign]
inline fun sgn(a: Double): Double = nativeMath.sign(a)

inline fun min(a: Double, b: Double): Double = nativeMath.min(a, b)
inline fun max(a: Double, b: Double): Double = nativeMath.max(a, b)

// extensions

inline fun Double.pow(other: Double): Double = nativeMath.pow(this, other)
inline fun Double.pow(other: Int): Double = nativeMath.pow(this, other.toDouble())

inline val Double.absoluteValue: Double get() = nativeMath.abs(this)
inline val Double.sign: Double get() = nativeMath.sign(this)

// TODO: Reimplement here
fun Double.withSign(sign: Double): Double = this.absoluteValue * sign.sign
inline fun Double.withSign(sign: Int): Double = this.withSign(sign.toDouble())
//inline fun Double.adjustExponent(scaleFactor: Int): Double = nativeMath.scalb(this, scaleFactor)


fun Double.roundToLong(): Long = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this).unsafeCast<Double>().toLong()



// Float

// also as extension val [absoluteValue]
inline fun abs(a: Float): Float = nativeMath.abs(a.toDouble()).toFloat()
// also as extension val [sign]
inline fun sgn(a: Float): Float = nativeMath.sign(a.toDouble()).toFloat()

inline fun max(a: Float, b: Float): Float = nativeMath.max(a, b)
inline fun min(a: Float, b: Float): Float = nativeMath.min(a, b)

inline val Float.absoluteValue: Float get() = nativeMath.abs(this.toDouble()).toFloat()
inline val Float.sign: Float get() = nativeMath.sign(this.toDouble()).toFloat()
//inline val Float.exponent: Int get() = nativeMath.getExponent(this)

// TODO: Reimplement
inline fun Float.withSign(sign: Float): Float = this.toDouble().withSign(sign.toDouble()).toFloat()
inline fun Float.withSign(sign: Int): Float = this.toDouble().withSign(sign.toDouble()).toFloat()
//inline fun Float.adjustExponent(scaleFactor: Int): Float = nativeMath.scalb(this, scaleFactor)
//fun Float.withExponent(exponent: Int): Float = nativeMath.scalb(this, exponent - this.exponent)


fun Float.roundToInt(): Int = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)
fun Float.roundToLong(): Long = toDouble().roundToLong()


// Int
// also as extension val [absoluteValue]
fun abs(a: Int): Int = if (a < 0) -a else a

inline fun min(a: Int, b: Int): Int = minOf(a, b)
inline fun max(a: Int, b: Int): Int = maxOf(a, b)

inline val Int.absoluteValue: Int get() = abs(this)


// Long
// also as extension val [absoluteValue]
fun abs(a: Long): Long = if (a < 0) -a else a

inline fun min(a: Long, b: Long): Long = minOf(a, b)
inline fun max(a: Long, b: Long): Long = maxOf(a, b)

inline val Long.absoluteValue: Long get() = abs(this)
