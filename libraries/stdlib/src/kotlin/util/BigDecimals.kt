@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")
@file:kotlin.jvm.JvmVersion

package kotlin

import java.math.BigDecimal
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
@Deprecated("Use rem(other) instead", ReplaceWith("rem(other)"), DeprecationLevel.WARNING)
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


@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun String.toBigDecimal(): BigDecimal = BigDecimal(this)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Int.toBigDecimal(): BigDecimal = BigDecimal(this)

@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Long.toBigDecimal(): BigDecimal = BigDecimal(this)

/*
 * JDK documentation recommends using `BigDecimal(String)` instead of `BigDecimal(double)`
 * For more details consult http://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Float.toBigDecimal(): BigDecimal = BigDecimal(this.toString())

/*
 * JDK documentation recommends using `BigDecimal(String)` instead of `BigDecimal(double)`
 * For more details consult http://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun Double.toBigDecimal(): BigDecimal = BigDecimal(this.toString())