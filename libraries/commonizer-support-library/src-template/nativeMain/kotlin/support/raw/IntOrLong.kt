/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import support.raw.bases.SignedNumberRange

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect class IntOrLong : Number, Comparable<IntOrLong> {
    /** Include contents of [support.raw.bases.SignedNumber] */

    operator fun plus(other: IntOrLong): IntOrLong
    operator fun plus(other: Byte): IntOrLong
    operator fun plus(other: Short): IntOrLong
    operator fun plus(other: Int): IntOrLong
    operator fun plus(other: Long): Long
    operator fun plus(other: Float): Float
    operator fun plus(other: Double): Double

    operator fun minus(other: IntOrLong): IntOrLong
    operator fun minus(other: Byte): IntOrLong
    operator fun minus(other: Short): IntOrLong
    operator fun minus(other: Int): IntOrLong
    operator fun minus(other: Long): Long
    operator fun minus(other: Float): Float
    operator fun minus(other: Double): Double

    operator fun times(other: IntOrLong): IntOrLong
    operator fun times(other: Byte): IntOrLong
    operator fun times(other: Short): IntOrLong
    operator fun times(other: Int): IntOrLong
    operator fun times(other: Long): Long
    operator fun times(other: Float): Float
    operator fun times(other: Double): Double

    operator fun div(other: IntOrLong): IntOrLong
    operator fun div(other: Byte): IntOrLong
    operator fun div(other: Short): IntOrLong
    operator fun div(other: Int): IntOrLong
    operator fun div(other: Long): Long
    operator fun div(other: Float): Float
    operator fun div(other: Double): Double

    operator fun rem(other: IntOrLong): IntOrLong
    operator fun rem(other: Byte): IntOrLong
    operator fun rem(other: Short): IntOrLong
    operator fun rem(other: Int): IntOrLong
    operator fun rem(other: Long): Long
    operator fun rem(other: Float): Float
    operator fun rem(other: Double): Double

    operator fun unaryPlus(): IntOrLong
    operator fun unaryMinus(): IntOrLong

    operator fun rangeTo(other: Byte): SignedNumberRange
    operator fun rangeTo(other: Short): SignedNumberRange
    operator fun rangeTo(other: Int): SignedNumberRange
    operator fun rangeTo(other: Long): LongRange

    operator fun rangeUntil(other: Byte): SignedNumberRange
    operator fun rangeUntil(other: Short): SignedNumberRange
    operator fun rangeUntil(other: Int): SignedNumberRange
    operator fun rangeUntil(other: Long): LongRange

    infix fun shl(bitCount: Int): IntOrLong
    infix fun shr(bitCount: Int): IntOrLong
    infix fun ushr(bitCount: Int): IntOrLong

    infix fun and(other: IntOrLong): IntOrLong
    infix fun or(other: IntOrLong): IntOrLong
    infix fun xor(other: IntOrLong): IntOrLong

    fun inv(): IntOrLong
}
