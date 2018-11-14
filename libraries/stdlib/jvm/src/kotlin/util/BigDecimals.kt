/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")

package kotlin

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.plus(other: BigDecimal): BigDecimal = this.add(other)

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.minus(other: BigDecimal): BigDecimal = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.times(other: BigDecimal): BigDecimal = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of `this` (divident), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.div(other: BigDecimal): BigDecimal = this.divide(other, RoundingMode.HALF_EVEN)

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
@Deprecated("Use rem(other) instead", ReplaceWith("rem(other)"), DeprecationLevel.ERROR)
public inline operator fun BigDecimal.mod(other: BigDecimal): BigDecimal = this.remainder(other)

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.rem(other: BigDecimal): BigDecimal = this.remainder(other)

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.unaryMinus(): BigDecimal = this.negate()

/**
 * Enables the use of the unary `++` operator for [BigDecimal] instances.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.inc(): BigDecimal = this.add(BigDecimal.ONE)

/**
 * Enables the use of the unary `--` operator for [BigDecimal] instances.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.dec(): BigDecimal = this.subtract(BigDecimal.ONE)

/**
 * Returns the value of this [Int] number as a [BigDecimal].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Int.toBigDecimal(): BigDecimal = BigDecimal.valueOf(this.toLong())


/**
 * Returns the value of this [Int] number as a [BigDecimal].
 * @param mathContext specifies the precision and the rounding mode.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Int.toBigDecimal(mathContext: MathContext): BigDecimal = BigDecimal(this, mathContext)

/**
 * Returns the value of this [Long] number as a [BigDecimal].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Long.toBigDecimal(): BigDecimal = BigDecimal.valueOf(this)

/**
 * Returns the value of this [Long] number as a [BigDecimal].
 * @param mathContext specifies the precision and the rounding mode.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Long.toBigDecimal(mathContext: MathContext): BigDecimal = BigDecimal(this, mathContext)


/**
 * Returns the value of this [Float] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.toBigDecimal(): BigDecimal = BigDecimal(this.toString())

/**
 * Returns the value of this [Float] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 *
 * @param mathContext specifies the precision and the rounding mode.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.toBigDecimal(mathContext: MathContext): BigDecimal = BigDecimal(this.toString(), mathContext)

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.toBigDecimal(): BigDecimal = BigDecimal(this.toString())

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 *
 * @param mathContext specifies the precision and the rounding mode.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.toBigDecimal(mathContext: MathContext): BigDecimal = BigDecimal(this.toString(), mathContext)
