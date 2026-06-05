/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import support.raw.bases.UnsignedNumberRange

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect value class UIntOrULong : Comparable<UIntOrULong> {
    /** Include contents of [support.raw.bases.UnsignedNumber] */

    operator fun plus(other: UIntOrULong): UIntOrULong
    operator fun plus(other: UByte): UIntOrULong
    operator fun plus(other: UShort): UIntOrULong
    operator fun plus(other: UInt): UIntOrULong
    operator fun plus(other: ULong): ULong

    operator fun minus(other: UIntOrULong): UIntOrULong
    operator fun minus(other: UByte): UIntOrULong
    operator fun minus(other: UShort): UIntOrULong
    operator fun minus(other: UInt): UIntOrULong
    operator fun minus(other: ULong): ULong

    operator fun times(other: UIntOrULong): UIntOrULong
    operator fun times(other: UByte): UIntOrULong
    operator fun times(other: UShort): UIntOrULong
    operator fun times(other: UInt): UIntOrULong
    operator fun times(other: ULong): ULong

    operator fun div(other: UIntOrULong): UIntOrULong
    operator fun div(other: UByte): UIntOrULong
    operator fun div(other: UShort): UIntOrULong
    operator fun div(other: UInt): UIntOrULong
    operator fun div(other: ULong): ULong

    operator fun rem(other: UIntOrULong): UIntOrULong
    operator fun rem(other: UByte): UIntOrULong
    operator fun rem(other: UShort): UIntOrULong
    operator fun rem(other: UInt): UIntOrULong
    operator fun rem(other: ULong): ULong

    fun floorDiv(other: UIntOrULong): UIntOrULong
    fun floorDiv(other: UByte): UIntOrULong
    fun floorDiv(other: UShort): UIntOrULong
    fun floorDiv(other: UInt): UIntOrULong
    fun floorDiv(other: ULong): ULong

    fun mod(other: UIntOrULong): UIntOrULong
    fun mod(other: UByte): UByte
    fun mod(other: UShort): UShort
    fun mod(other: UInt): UInt
    fun mod(other: ULong): ULong

    operator fun rangeTo(other: UIntOrULong): UnsignedNumberRange
    operator fun rangeUntil(other: UIntOrULong): UnsignedNumberRange
}
