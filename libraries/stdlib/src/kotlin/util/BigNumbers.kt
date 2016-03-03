@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")
@file:kotlin.jvm.JvmVersion
package kotlin

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.plus(other: BigInteger) : BigInteger = this.add(other)

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.minus(other: BigInteger) : BigInteger = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.times(other: BigInteger) : BigInteger = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.div(other: BigInteger) : BigInteger = this.divide(other)

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigInteger.unaryMinus() : BigInteger = this.negate()


/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.plus(other: BigDecimal) : BigDecimal = this.add(other)

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.minus(other: BigDecimal) : BigDecimal = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.times(other: BigDecimal) : BigDecimal = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of `this` (divident), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.div(other: BigDecimal) : BigDecimal = this.divide(other, RoundingMode.HALF_EVEN)

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.mod(other: BigDecimal) : BigDecimal = this.remainder(other)

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
@kotlin.internal.InlineOnly
public inline operator fun BigDecimal.unaryMinus() : BigDecimal = this.negate()
