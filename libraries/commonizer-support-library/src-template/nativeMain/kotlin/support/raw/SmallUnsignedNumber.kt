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
expect value class SmallUnsignedNumber : Comparable<SmallUnsignedNumber> {
    /** Include contents of [support.raw.bases.UnsignedNumber] */

    operator fun plus(other: SmallUnsignedNumber): UInt
    operator fun plus(other: UByte): UInt
    operator fun plus(other: UShort): UInt
    operator fun plus(other: UInt): UInt
    operator fun plus(other: ULong): ULong

    operator fun minus(other: SmallUnsignedNumber): UInt
    operator fun minus(other: UByte): UInt
    operator fun minus(other: UShort): UInt
    operator fun minus(other: UInt): UInt
    operator fun minus(other: ULong): ULong

    operator fun times(other: SmallUnsignedNumber): UInt
    operator fun times(other: UByte): UInt
    operator fun times(other: UShort): UInt
    operator fun times(other: UInt): UInt
    operator fun times(other: ULong): ULong

    operator fun div(other: SmallUnsignedNumber): UInt
    operator fun div(other: UByte): UInt
    operator fun div(other: UShort): UInt
    operator fun div(other: UInt): UInt
    operator fun div(other: ULong): ULong

    operator fun rem(other: SmallUnsignedNumber): UInt
    operator fun rem(other: UByte): UInt
    operator fun rem(other: UShort): UInt
    operator fun rem(other: UInt): UInt
    operator fun rem(other: ULong): ULong

    fun floorDiv(other: SmallUnsignedNumber): UInt
    fun floorDiv(other: UByte): UInt
    fun floorDiv(other: UShort): UInt
    fun floorDiv(other: UInt): UInt
    fun floorDiv(other: ULong): ULong

    fun mod(other: SmallUnsignedNumber): SmallUnsignedNumber
    fun mod(other: UByte): UByte
    fun mod(other: UShort): UShort
    fun mod(other: UInt): UInt
    fun mod(other: ULong): ULong

    operator fun rangeTo(other: SmallUnsignedNumber): UIntRange
    operator fun rangeUntil(other: SmallUnsignedNumber): UIntRange
}
