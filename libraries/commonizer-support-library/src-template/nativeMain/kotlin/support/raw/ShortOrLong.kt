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
expect class ShortOrLong : Number, Comparable<ShortOrLong> {
    /** Include contents of [support.raw.bases.SignedNumber] */

    operator fun plus(other: Long): Long
    operator fun plus(other: Float): Float
    operator fun plus(other: Double): Double

    operator fun minus(other: Long): Long
    operator fun minus(other: Float): Float
    operator fun minus(other: Double): Double

    operator fun times(other: Long): Long
    operator fun times(other: Float): Float
    operator fun times(other: Double): Double

    operator fun div(other: Long): Long
    operator fun div(other: Float): Float
    operator fun div(other: Double): Double

    operator fun rem(other: Long): Long
    operator fun rem(other: Float): Float
    operator fun rem(other: Double): Double

    operator fun rangeTo(other: Long): LongRange
    operator fun rangeUntil(other: Long): LongRange
}
