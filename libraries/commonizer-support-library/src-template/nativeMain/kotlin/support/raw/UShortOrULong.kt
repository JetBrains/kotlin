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
expect value class UShortOrULong : Comparable<UShortOrULong> {
    /** Include contents of [support.raw.bases.UnsignedNumber] */

    operator fun plus(other: ULong): ULong

    operator fun minus(other: ULong): ULong

    operator fun times(other: ULong): ULong

    operator fun div(other: ULong): ULong

    operator fun rem(other: ULong): ULong

    fun floorDiv(other: ULong): ULong

    fun mod(other: ULong): ULong
}
