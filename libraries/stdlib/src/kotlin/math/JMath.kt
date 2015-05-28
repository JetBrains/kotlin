package kotlin.math

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
public fun BigInteger.plus(other: BigInteger) : BigInteger = this.add(other)

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
public fun BigInteger.minus(other: BigInteger) : BigInteger = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public fun BigInteger.times(other: BigInteger) : BigInteger = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigInteger] instances.
 */
public fun BigInteger.div(other: BigInteger) : BigInteger = this.divide(other)

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
public fun BigInteger.minus() : BigInteger = this.negate()


/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
public fun BigDecimal.plus(other: BigDecimal) : BigDecimal = this.add(other)

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
public fun BigDecimal.minus(other: BigDecimal) : BigDecimal = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
public fun BigDecimal.times(other: BigDecimal) : BigDecimal = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 */
public fun BigDecimal.div(other: BigDecimal) : BigDecimal = this.divide(other)

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
public fun BigDecimal.mod(other: BigDecimal) : BigDecimal = this.remainder(other)

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
public fun BigDecimal.minus() : BigDecimal = this.negate()
