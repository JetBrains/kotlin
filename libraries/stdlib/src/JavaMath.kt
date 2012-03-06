package kotlin.math

import java.math.BigInteger
import java.math.BigDecimal

fun BigInteger.plus(other: BigInteger) = this.add(other).sure()

fun BigInteger.minus(other: BigInteger) = this.subtract(other).sure()

fun BigInteger.times(other: BigInteger) = this.multiply(other).sure()

fun BigInteger.div(other: BigInteger) = this.divide(other).sure()

fun BigInteger.minus() = this.negate().sure()


fun BigDecimal.plus(other: BigDecimal) = this.add(other).sure()

fun BigDecimal.minus(other: BigDecimal) = this.subtract(other).sure()

fun BigDecimal.times(other: BigDecimal) = this.multiply(other).sure()

fun BigDecimal.div(other: BigDecimal) = this.divide(other).sure()

fun BigDecimal.mod(other: BigDecimal) = this.remainder(other).sure()

fun BigDecimal.minus() = this.negate().sure()