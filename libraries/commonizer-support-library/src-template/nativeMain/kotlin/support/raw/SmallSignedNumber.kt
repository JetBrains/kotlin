/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect class SmallSignedNumber : Number, Comparable<SmallSignedNumber> {
    /** Include contents of [support.raw.bases.SignedNumber] */

    operator fun plus(other: SmallSignedNumber): Int
    operator fun plus(other: Byte): Int
    operator fun plus(other: Short): Int
    operator fun plus(other: Int): Int
    operator fun plus(other: Long): Long
    operator fun plus(other: Float): Float
    operator fun plus(other: Double): Double

    operator fun minus(other: SmallSignedNumber): Int
    operator fun minus(other: Byte): Int
    operator fun minus(other: Short): Int
    operator fun minus(other: Int): Int
    operator fun minus(other: Long): Long
    operator fun minus(other: Float): Float
    operator fun minus(other: Double): Double

    operator fun times(other: SmallSignedNumber): Int
    operator fun times(other: Byte): Int
    operator fun times(other: Short): Int
    operator fun times(other: Int): Int
    operator fun times(other: Long): Long
    operator fun times(other: Float): Float
    operator fun times(other: Double): Double

    operator fun div(other: SmallSignedNumber): Int
    operator fun div(other: Byte): Int
    operator fun div(other: Short): Int
    operator fun div(other: Int): Int
    operator fun div(other: Long): Long
    operator fun div(other: Float): Float
    operator fun div(other: Double): Double

    operator fun rem(other: SmallSignedNumber): Int
    operator fun rem(other: Byte): Int
    operator fun rem(other: Short): Int
    operator fun rem(other: Int): Int
    operator fun rem(other: Long): Long
    operator fun rem(other: Float): Float
    operator fun rem(other: Double): Double

    operator fun unaryPlus(): Int
    operator fun unaryMinus(): Int

    operator fun rangeTo(other: Byte): IntRange
    operator fun rangeTo(other: Short): IntRange
    operator fun rangeTo(other: Int): IntRange
    operator fun rangeTo(other: Long): LongRange

    operator fun rangeUntil(other: Byte): IntRange
    operator fun rangeUntil(other: Short): IntRange
    operator fun rangeUntil(other: Int): IntRange
    operator fun rangeUntil(other: Long): LongRange
}
