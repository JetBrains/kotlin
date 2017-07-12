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

import java.lang.Math as nativeMath

// constants
/** Ratio of the circumference of a circle to its diameter, approximately 3.14159. */
const val PI: Double = nativeMath.PI
/** Base of the natural logarithms, approximately 2.71828. */
const val E: Double = nativeMath.E

// Double

inline fun sin(a: Double): Double = nativeMath.sin(a)
inline fun cos(a: Double): Double = nativeMath.cos(a)
inline fun tan(a: Double): Double = nativeMath.tan(a)

inline fun asin(a: Double): Double = nativeMath.asin(a)
inline fun acos(a: Double): Double = nativeMath.acos(a)
inline fun atan(a: Double): Double = nativeMath.atan(a)
inline fun atan2(y: Double, x: Double): Double = nativeMath.atan2(y, x)

inline fun sinh(a: Double): Double = nativeMath.sinh(a)
inline fun cosh(a: Double): Double = nativeMath.cosh(a)
inline fun tanh(a: Double): Double = nativeMath.tanh(a)

inline fun hypot(x: Double, y: Double): Double = nativeMath.hypot(x, y)

inline fun pow(a: Double, b: Double): Double = nativeMath.pow(a, b)
inline fun pow(a: Double, b: Int): Double = nativeMath.pow(a, b.toDouble())

inline fun sqrt(a: Double): Double = nativeMath.sqrt(a)

inline fun exp(a: Double): Double = nativeMath.exp(a)
inline fun expm1(a: Double): Double = nativeMath.expm1(a)

inline fun log(a: Double): Double = nativeMath.log(a)
fun log(a: Double, base: Double): Double = nativeMath.log(a) / nativeMath.log(base)
inline fun log10(a: Double): Double = nativeMath.log10(a)
inline fun log1p(a: Double): Double = nativeMath.log1p(a)

inline fun ceil(a: Double): Double = nativeMath.ceil(a)
inline fun floor(a: Double): Double = nativeMath.floor(a)
inline fun truncate(a: Double): Double = nativeMath.rint(a)

// also as extension val [absoluteValue]
inline fun abs(a: Double): Double = nativeMath.abs(a)
// also as extension val [sign]
inline fun sgn(a: Double): Double = nativeMath.signum(a)




inline fun min(a: Double, b: Double): Double = nativeMath.min(a, b)
inline fun max(a: Double, b: Double): Double = nativeMath.max(a, b)

// extensions

@JvmName("power")
inline fun Double.pow(other: Double): Double = nativeMath.pow(this, other)
@JvmName("power")
inline fun Double.pow(other: Int): Double = nativeMath.pow(this, other.toDouble())


inline fun Double.IEEErem(other: Double): Double = nativeMath.IEEEremainder(this, other)
inline val Double.absoluteValue: Double get() = nativeMath.abs(this)
inline val Double.sign: Double get() = nativeMath.signum(this)
inline val Double.exponent: Int get() = nativeMath.getExponent(this)

inline fun Double.withSign(sign: Double): Double = nativeMath.copySign(this, sign)
inline fun Double.withSign(sign: Int): Double = nativeMath.copySign(this, sign.toDouble())
inline fun Double.adjustExponent(scaleFactor: Int): Double = nativeMath.scalb(this, scaleFactor)
fun Double.withExponent(exponent: Int): Double = nativeMath.scalb(this, exponent - this.exponent)

inline val Double.ulp: Double get() = nativeMath.ulp(this)
inline fun Double.nextUp(): Double = nativeMath.nextUp(this)
inline fun Double.nextDown(): Double = nativeMath.nextAfter(this, Double.NEGATIVE_INFINITY)
inline fun Double.nextTowards(to: Double): Double = nativeMath.nextAfter(this, to)

fun Double.roundToLong(): Long = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)


// Float

// also as extension val [absoluteValue]
inline fun abs(a: Float): Float = nativeMath.abs(a)
// also as extension val [sign]
inline fun sgn(a: Float): Float = nativeMath.signum(a)

inline fun max(a: Float, b: Float): Float = nativeMath.max(a, b)
inline fun min(a: Float, b: Float): Float = nativeMath.min(a, b)

inline val Float.absoluteValue: Float get() = nativeMath.abs(this)
inline val Float.sign: Float get() = nativeMath.signum(this)
inline val Float.exponent: Int get() = nativeMath.getExponent(this)

inline fun Float.withSign(sign: Float): Float = nativeMath.copySign(this, sign)
inline fun Float.withSign(sign: Int): Float = nativeMath.copySign(this, sign.toFloat())
inline fun Float.adjustExponent(scaleFactor: Int): Float = nativeMath.scalb(this, scaleFactor)
fun Float.withExponent(exponent: Int): Float = nativeMath.scalb(this, exponent - this.exponent)


inline val Float.ulp: Float get() = nativeMath.ulp(this)
inline fun Float.nextUp(): Float = nativeMath.nextUp(this)
inline fun Float.nextDown(): Float = nativeMath.nextAfter(this, Double.NEGATIVE_INFINITY)
inline fun Float.nextTowards(to: Double): Float = nativeMath.nextAfter(this, to)

fun Float.roundToInt(): Int = if (isNaN()) throw IllegalArgumentException("Cannot round NaN value.") else nativeMath.round(this)
fun Float.roundToLong(): Long = toDouble().roundToLong()


// Int
// also as extension val [absoluteValue]
inline fun abs(a: Int): Int = nativeMath.abs(a)

inline fun min(a: Int, b: Int): Int = nativeMath.min(a, b)
inline fun max(a: Int, b: Int): Int = nativeMath.max(a, b)

inline val Int.absoluteValue: Int get() = nativeMath.abs(this)


// Long
// also as extension val [absoluteValue]
inline fun abs(a: Long): Long = nativeMath.abs(a)

inline fun min(a: Long, b: Long): Long = nativeMath.min(a, b)
inline fun max(a: Long, b: Long): Long = nativeMath.max(a, b)

inline val Long.absoluteValue: Long get() = nativeMath.abs(this)
