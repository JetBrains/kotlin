package kotlin.math

import java.math.BigInteger
import java.math.BigDecimal

public fun BigInteger.plus(other: BigInteger) : BigInteger = this.add(other)

public fun BigInteger.minus(other: BigInteger) : BigInteger = this.subtract(other)

public fun BigInteger.times(other: BigInteger) : BigInteger = this.multiply(other)

public fun BigInteger.div(other: BigInteger) : BigInteger = this.divide(other)

public fun BigInteger.minus() : BigInteger = this.negate()


public fun BigDecimal.plus(other: BigDecimal) : BigDecimal = this.add(other)

public fun BigDecimal.minus(other: BigDecimal) : BigDecimal = this.subtract(other)

public fun BigDecimal.times(other: BigDecimal) : BigDecimal = this.multiply(other)

public fun BigDecimal.div(other: BigDecimal) : BigDecimal = this.divide(other)

public fun BigDecimal.mod(other: BigDecimal) : BigDecimal = this.remainder(other)

public fun BigDecimal.minus() : BigDecimal = this.negate()