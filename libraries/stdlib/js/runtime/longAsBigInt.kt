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
internal annotation class LongAsBigIntApi

/**
 * @see kotlin.js.internal.boxedLong.toNumber
 */
@LongAsBigIntApi
internal fun Long.toNumber(): Double = unsafeCast<BigInt>().toNumber().unsafeCast<Double>()

private fun Long.truncating(bitSize: Int): Number = BigInt.asIntN(bitSize, unsafeCast<BigInt>()).toNumber()

/**
 * @see kotlin.js.internal.boxedLong.convertToByte
 */
@LongAsBigIntApi
internal fun Long.convertToByte(): Byte = truncating(Byte.SIZE_BITS).unsafeCast<Byte>()

/**
 * @see kotlin.js.internal.boxedLong.convertToChar
 */
@LongAsBigIntApi
internal fun Long.convertToChar(): Char = truncating(Char.SIZE_BITS).unsafeCast<Int>().toChar()

/**
 * @see kotlin.js.internal.boxedLong.convertToShort
 */
@LongAsBigIntApi
internal fun Long.convertToShort(): Short = truncating(Short.SIZE_BITS).unsafeCast<Short>()

/**
 * @see kotlin.js.internal.boxedLong.convertToInt
 */
@LongAsBigIntApi
internal fun Long.convertToInt(): Int = truncating(Int.SIZE_BITS).unsafeCast<Int>()

/**
 * @see kotlin.js.internal.boxedLong.toStringImpl
 */
@LongAsBigIntApi
internal fun Long.toStringImpl(radix: Int): String {
    return this.unsafeCast<BigInt>().toString(radix)
}

@LongAsBigIntApi
private inline fun wrappingArithmetic(lhs: Long, rhs: Long, op: (l: BigInt, r: BigInt) -> BigInt): Long =
    BigInt.asIntN(Long.SIZE_BITS, op(lhs.unsafeCast<BigInt>(), rhs.unsafeCast<BigInt>())).unsafeCast<Long>()

/**
 * @see kotlin.js.internal.boxedLong.negate
 */
@LongAsBigIntApi
internal fun Long.negate(): Long =
    BigInt.asIntN(Long.SIZE_BITS, -unsafeCast<BigInt>()).unsafeCast<Long>()

/**
 * @see kotlin.js.internal.boxedLong.add
 */
@LongAsBigIntApi
internal fun Long.add(other: Long): Long = wrappingArithmetic(this, other, BigInt::plus)

/**
 * @see kotlin.js.internal.boxedLong.subtract
 */
@LongAsBigIntApi
internal fun Long.subtract(other: Long): Long = wrappingArithmetic(this, other, BigInt::minus)

/**
 * @see kotlin.js.internal.boxedLong.multiply
 */
@LongAsBigIntApi
internal fun Long.multiply(other: Long): Long = wrappingArithmetic(this, other, BigInt::times)

/**
 * @see kotlin.js.internal.boxedLong.divide
 */
@LongAsBigIntApi
internal fun Long.divide(other: Long): Long = wrappingArithmetic(this, other, BigInt::div)

/**
 * @see kotlin.js.internal.boxedLong.modulo
 */
@LongAsBigIntApi
internal fun Long.modulo(other: Long): Long = wrappingArithmetic(this, other, BigInt::rem)

// In JavaScript, shifting a bigint by a negative N is equivalent to shifting it by -N in the opposite direction.
// This is not the semantics we want from Long, so we emulate what JavaScript does for Numbers, namely, take `numBits` modulo 64.
private fun sanitizeBitShiftRHS(numBits: Int): Int = numBits and (Long.SIZE_BITS - 1)

/**
 * @see kotlin.js.internal.boxedLong.shiftLeft
 */
@LongAsBigIntApi
internal fun Long.shiftLeft(numBits: Int): Long = wrappingArithmetic(this, sanitizeBitShiftRHS(numBits).toLong(), BigInt::shl)

/**
 * @see kotlin.js.internal.boxedLong.shiftRight
 */
@LongAsBigIntApi
internal fun Long.shiftRight(numBits: Int): Long = wrappingArithmetic(this, sanitizeBitShiftRHS(numBits).toLong(), BigInt::shr)

/**
 * @see kotlin.js.internal.boxedLong.shiftRightUnsigned
 */
@LongAsBigIntApi
internal fun Long.shiftRightUnsigned(numBits: Int): Long = wrappingArithmetic(this, sanitizeBitShiftRHS(numBits).toLong()) { lhs, rhs ->
    // BigInt doesn't natively support unsigned shift right.
    BigInt.asUintN(Long.SIZE_BITS, lhs) shr rhs
}

/**
 * @see kotlin.js.internal.boxedLong.fromInt
 */
@LongAsBigIntApi
internal fun fromInt(value: dynamic): Long = BigInt(value).unsafeCast<Long>()

/**
 * @see kotlin.js.internal.boxedLong.numberToLong
 */
@LongAsBigIntApi
internal fun numberToLong(value: dynamic): Long = value as? Long ?: fromNumber(value.unsafeCast<Double>())

/**
 * Converts this [Double] value to [Long].
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is `NaN`, [Long.MIN_VALUE] if it's less than `Long.MIN_VALUE`,
 * [Long.MAX_VALUE] if it's bigger than `Long.MAX_VALUE`.
 *
 * @see kotlin.js.internal.boxedLong.fromNumber
 */
@LongAsBigIntApi
internal fun fromNumber(value: Double): Long = when {
    value.isNaN() -> 0L
    value <= -TWO_PWR_63_DBL_ -> Long.MIN_VALUE
    value + 1 >= TWO_PWR_63_DBL_ -> Long.MAX_VALUE
    else -> BigInt(nativeTrunc(value)).unsafeCast<Long>()
}

//private val TWO_PWR_63_DBL_ = TWO_PWR_64_DBL_ / 2
private const val TWO_PWR_63_DBL_ = (((1 shl 16).toDouble() * (1 shl 16).toDouble()) * ((1 shl 16).toDouble() * (1 shl 16).toDouble())) / 2

// TODO(KT-70480): Remove this function when we drop the ES5 target
@LongAsBigIntApi
internal fun longFromTwoInts(low: Int, high: Int): Long = high.toLong() shl 32 or (low.toLong() and 0xFFFFFFFFL)

// TODO(KT-70480): Remove this function when we drop the ES5 target
@LongAsBigIntApi
internal fun Long.lowBits(): Int = toInt()

// TODO(KT-70480): Remove this function when we drop the ES5 target
@LongAsBigIntApi
internal fun Long.highBits(): Int = (this shr 32).toInt()
