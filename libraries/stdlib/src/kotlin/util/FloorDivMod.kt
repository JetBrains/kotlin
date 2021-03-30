/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

@file:kotlin.jvm.JvmName("NumbersKt")
@file:kotlin.jvm.JvmMultifileClass
package kotlin

import kotlin.math.sign

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.floorDiv(other: Byte): Int = 
    this.toInt().floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.mod(other: Byte): Byte = 
    this.toInt().mod(other.toInt()).toByte()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.floorDiv(other: Short): Int = 
    this.toInt().floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.mod(other: Short): Short = 
    this.toInt().mod(other.toInt()).toShort()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.floorDiv(other: Int): Int = 
    this.toInt().floorDiv(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.mod(other: Int): Int = 
    this.toInt().mod(other)

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.floorDiv(other: Long): Long = 
    this.toLong().floorDiv(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Byte.mod(other: Long): Long = 
    this.toLong().mod(other)

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.floorDiv(other: Byte): Int = 
    this.toInt().floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.mod(other: Byte): Byte = 
    this.toInt().mod(other.toInt()).toByte()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.floorDiv(other: Short): Int = 
    this.toInt().floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.mod(other: Short): Short = 
    this.toInt().mod(other.toInt()).toShort()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.floorDiv(other: Int): Int = 
    this.toInt().floorDiv(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.mod(other: Int): Int = 
    this.toInt().mod(other)

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.floorDiv(other: Long): Long = 
    this.toLong().floorDiv(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Short.mod(other: Long): Long = 
    this.toLong().mod(other)

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.floorDiv(other: Byte): Int = 
    this.floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.mod(other: Byte): Byte = 
    this.mod(other.toInt()).toByte()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.floorDiv(other: Short): Int = 
    this.floorDiv(other.toInt())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.mod(other: Short): Short = 
    this.mod(other.toInt()).toShort()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.floorDiv(other: Int): Int {
    var q = this / other
    if (this xor other < 0 && q * other != this) q-- 
    return q
}

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.mod(other: Int): Int {
    val r = this % other
    return r + (other and (((r xor other) and (r or -r)) shr 31))
}

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.floorDiv(other: Long): Long = 
    this.toLong().floorDiv(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Int.mod(other: Long): Long = 
    this.toLong().mod(other)

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.floorDiv(other: Byte): Long = 
    this.floorDiv(other.toLong())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.mod(other: Byte): Byte = 
    this.mod(other.toLong()).toByte()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.floorDiv(other: Short): Long = 
    this.floorDiv(other.toLong())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.mod(other: Short): Short = 
    this.mod(other.toLong()).toShort()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.floorDiv(other: Int): Long = 
    this.floorDiv(other.toLong())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.mod(other: Int): Int = 
    this.mod(other.toLong()).toInt()

/** Divides this value by the other value, flooring the result to an integer that is closer to negative infinity. */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.floorDiv(other: Long): Long {
    var q = this / other
    if (this xor other < 0 && q * other != this) q-- 
    return q
}

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Long.mod(other: Long): Long {
    val r = this % other
    return r + (other and (((r xor other) and (r or -r)) shr 63))
}

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 * 
 * If the result cannot be represented exactly, it is rounded to the nearest representable number. In this case the absolute value of the result can be less than or _equal to_ the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Float.mod(other: Float): Float {
    val r = this % other
    return if (r != 0.0.toFloat() && r.sign != other.sign) r + other else r
}

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 * 
 * If the result cannot be represented exactly, it is rounded to the nearest representable number. In this case the absolute value of the result can be less than or _equal to_ the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Float.mod(other: Double): Double = 
    this.toDouble().mod(other)

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 * 
 * If the result cannot be represented exactly, it is rounded to the nearest representable number. In this case the absolute value of the result can be less than or _equal to_ the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Double.mod(other: Float): Double = 
    this.mod(other.toDouble())

/**
 * Calculates the remainder of flooring division of this value by the other value.
 * 
 * The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
 * 
 * If the result cannot be represented exactly, it is rounded to the nearest representable number. In this case the absolute value of the result can be less than or _equal to_ the absolute value of the divisor.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun Double.mod(other: Double): Double {
    val r = this % other
    return if (r != 0.0 && r.sign != other.sign) r + other else r
}

