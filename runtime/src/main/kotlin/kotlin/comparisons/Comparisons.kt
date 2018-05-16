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

package kotlin.comparisons

// Implements expects from _Comparisions.kt.

/**
 * Returns the greater of two values.
 * If values are equal, returns the first one.
 */
@SinceKotlin("1.1")
public actual fun <T: Comparable<T>> maxOf(a: T, b: T): T {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Byte, b: Byte): Byte {
    return maxOf(a.toInt(), b.toInt()).toByte()
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Short, b: Short): Short {
    return maxOf(a.toInt(), b.toInt()).toShort()
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Int, b: Int): Int {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Long, b: Long): Long {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Float, b: Float): Float {
    // According to http://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#max-float-float-
    // return NaN if one of the args is NaN.
    // TODO: Check +/-0.0
    return when {
        a.isNaN() -> a
        b.isNaN() -> b
        else -> if (a >= b) a else b
    }
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Double, b: Double): Double {
    return when {
        a.isNaN() -> a
        b.isNaN() -> b
        else -> if (a >= b) a else b
    }
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
public actual fun <T: Comparable<T>> maxOf(a: T, b: T, c: T): T {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Byte, b: Byte, c: Byte): Byte {
    return maxOf(a.toInt(), maxOf(b.toInt(), c.toInt())).toByte()
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Short, b: Short, c: Short): Short {
    return maxOf(a.toInt(), maxOf(b.toInt(), c.toInt())).toShort()
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Int, b: Int, c: Int): Int {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Long, b: Long, c: Long): Long {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Float, b: Float, c: Float): Float {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun maxOf(a: Double, b: Double, c: Double): Double {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the smaller of two values.
 * If values are equal, returns the first one.
 */
@SinceKotlin("1.1")
public actual fun <T: Comparable<T>> minOf(a: T, b: T): T {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Byte, b: Byte): Byte {
    return minOf(a.toInt(), b.toInt()).toByte()
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Short, b: Short): Short {
    return minOf(a.toInt(), b.toInt()).toShort()
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Int, b: Int): Int {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Long, b: Long): Long {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Float, b: Float): Float {
    // According to http://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#min-float-float-
    // return NaN if one of the args is NaN.
    return when {
        a.isNaN() -> a
        b.isNaN() -> b
        else -> if (a <= b) a else b
    }
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Double, b: Double): Double {
    return when {
        a.isNaN() -> a
        b.isNaN() -> b
        else -> if (a <= b) a else b
    }
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
public actual fun <T: Comparable<T>> minOf(a: T, b: T, c: T): T {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Byte, b: Byte, c: Byte): Byte {
    return minOf(a.toInt(), minOf(b.toInt(), c.toInt())).toByte()
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Short, b: Short, c: Short): Short {
    return minOf(a.toInt(), minOf(b.toInt(), c.toInt())).toShort()
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Int, b: Int, c: Int): Int {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Long, b: Long, c: Long): Long {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Float, b: Float, c: Float): Float {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline actual fun minOf(a: Double, b: Double, c: Double): Double {
    return minOf(a, minOf(b, c))
}

