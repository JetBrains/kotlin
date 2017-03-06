@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")
@file:kotlin.jvm.JvmVersion

package kotlin

import java.math.BigInteger
import java.math.BigDecimal

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


@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun BigInteger.inv(): BigInteger = this.not()

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.and(other: BigInteger): BigInteger = this.and(other)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.or(other: BigInteger): BigInteger = this.or(other)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.xor(other: BigInteger): BigInteger = this.xor(other)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.shl(n: Int): BigInteger = this.shiftLeft(n)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline infix fun BigInteger.shr(n: Int): BigInteger = this.shiftRight(n)


@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigInteger(): BigInteger = BigInteger(this)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Int.toBigInteger(): BigInteger = BigInteger.valueOf(this.toLong())

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)


@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun BigInteger.toBigDecimal(): BigDecimal = BigDecimal(this)