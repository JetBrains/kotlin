/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package kotlin.js.internal.longAsBigInt

import kotlin.js.internal.*

/**
 * Marks the stdlib functions that implement the BigInt-backed [Long] operations.
 *
 * If you use a function annotated with this annotation, you assume that [Long] is just a JavaScript BigInt.
 * Don't do it unless you are sure that you also handle the boxed Long values.
 *
 * This annotation will need to be removed when we drop the ES5 target completely (KT-70480).
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
internal annotation class BigIntLongImplementation

/**
 * @see kotlin.js.internal.boxedLong.toNumber
 */
@BigIntLongImplementation
internal fun Long.toNumber(): Double {
    @Suppress("UnusedVariable") val long = this
    return js("Number(long)").unsafeCast<Double>()
}

/**
 * @see kotlin.js.internal.boxedLong.hashCode
 */
internal fun hashCode(l: Long): Int = ((l ushr 32) xor l).toInt()

/**
 * @see kotlin.js.internal.boxedLong.toStringImpl
 */
@BigIntLongImplementation
internal fun Long.toStringImpl(radix: Int): String {
    return this.unsafeCast<BigInt>().toString(radix)
}

@BigIntLongImplementation
private inline fun binop(lhs: Long, rhs: Long, op: (l: BigInt, r: BigInt) -> BigInt): Long =
    BigInt.asIntN(64, op(lhs.unsafeCast<BigInt>(), rhs.unsafeCast<BigInt>())).unsafeCast<Long>()

/**
 * @see kotlin.js.internal.boxedLong.add
 */
@BigIntLongImplementation
internal fun Long.add(other: Long): Long = binop(this, other, BigInt::plus)

/**
 * @see kotlin.js.internal.boxedLong.subtract
 */
@BigIntLongImplementation
internal fun Long.subtract(other: Long): Long = binop(this, other, BigInt::minus)

/**
 * @see kotlin.js.internal.boxedLong.multiply
 */
@BigIntLongImplementation
internal fun Long.multiply(other: Long): Long = binop(this, other, BigInt::times)

/**
 * @see kotlin.js.internal.boxedLong.divide
 */
@BigIntLongImplementation
internal fun Long.divide(other: Long): Long {
    // No need to do BigInt.asIntN(64, ...) because division can't result in an overflow
    return (unsafeCast<BigInt>() / other.unsafeCast<BigInt>()).unsafeCast<Long>()
}

/**
 * @see kotlin.js.internal.boxedLong.modulo
 */
@BigIntLongImplementation
internal fun Long.modulo(other: Long): Long {
    // No need to do BigInt.asIntN(64, ...) because remainder can't result in an overflow
    return (unsafeCast<BigInt>() % other.unsafeCast<BigInt>()).unsafeCast<Long>()
}

/**
 * @see kotlin.js.internal.boxedLong.shiftLeft
 */
@BigIntLongImplementation
internal fun Long.shiftLeft(numBits: Int): Long = binop(this, numBits.toLong(), BigInt::shl)

/**
 * @see kotlin.js.internal.boxedLong.shiftRight
 */
@BigIntLongImplementation
internal fun Long.shiftRight(numBits: Int): Long = binop(this, numBits.toLong(), BigInt::shr)

/**
 * @see kotlin.js.internal.boxedLong.shiftRightUnsigned
 */
@BigIntLongImplementation
internal fun Long.shiftRightUnsigned(numBits: Int): Long = binop(this, numBits.toLong()) { lhs, rhs ->
    // BigInt doesn't natively support unsigned shift right.
    BigInt.asUintN(64, lhs) shr rhs
}

/**
 * Converts this [Double] value to [Long].
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
 * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
 *
 * @see kotlin.js.internal.boxedLong.fromNumber
 */
@BigIntLongImplementation
internal fun fromNumber(value: Double): Long = when {
    value.isNaN() -> 0L
    value <= -TWO_PWR_63_DBL_ -> Long.MIN_VALUE
    value + 1 >= TWO_PWR_63_DBL_ -> Long.MAX_VALUE
    else -> BigInt(value).unsafeCast<Long>()
}

//private val TWO_PWR_63_DBL_ = TWO_PWR_64_DBL_ / 2
private const val TWO_PWR_63_DBL_ = (((1 shl 16).toDouble() * (1 shl 16).toDouble()) * ((1 shl 16).toDouble() * (1 shl 16).toDouble())) / 2
