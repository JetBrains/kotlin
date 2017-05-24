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

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")
@file:kotlin.jvm.JvmVersion

package kotlin

import java.math.BigInteger
import java.math.BigDecimal
import java.math.MathContext

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.plus(other: BigInteger): BigInteger = this.add(other)

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.minus(other: BigInteger): BigInteger = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.times(other: BigInteger): BigInteger = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.div(other: BigInteger): BigInteger = this.divide(other)

/**
 * Enables the use of the `%` operator for [BigInteger] instances.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.rem(other: BigInteger): BigInteger = this.remainder(other)

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.unaryMinus(): BigInteger = this.negate()

/**
 * Enables the use of the `++` operator for [BigInteger] instances.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.inc(): BigInteger = this.add(BigInteger.ONE)

/**
 * Enables the use of the `--` operator for [BigInteger] instances.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.dec(): BigInteger = this.subtract(BigInteger.ONE)

/** Inverts the bits including the sign bit in this value. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun BigInteger.inv(): BigInteger = this.not()

/** Performs a bitwise AND operation between the two values. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.and(other: BigInteger): BigInteger = this.and(other)

/** Performs a bitwise OR operation between the two values. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.or(other: BigInteger): BigInteger = this.or(other)

/** Performs a bitwise XOR operation between the two values. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.xor(other: BigInteger): BigInteger = this.xor(other)

/** Shifts this value left by [bits]. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.shl(n: Int): BigInteger = this.shiftLeft(n)

/** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.shr(n: Int): BigInteger = this.shiftRight(n)


/**
 * Returns the value of this [Int] number as a [BigInteger].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Int.toBigInteger(): BigInteger = BigInteger.valueOf(this.toLong())

/**
 * Returns the value of this [Long] number as a [BigInteger].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun BigInteger.toBigDecimal(): BigDecimal = BigDecimal(this)

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal]
 * scaled according to the specified [scale] and rounded according to the settings specified with [mathContext].
 *
 * @param scale the scale of the resulting [BigDecimal], i.e. number of decimal places of the fractional part.
 * By default 0.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun BigInteger.toBigDecimal(scale: Int = 0, mathContext: MathContext = MathContext.UNLIMITED): BigDecimal =
        BigDecimal(this, scale, mathContext)

