/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw.bases

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect value class UnsignedNumber {
    /** Include contents of [AnyNumber] */

    operator fun compareTo(other: UByte): Int
    operator fun compareTo(other: UShort): Int
    operator fun compareTo(other: UInt): Int
    operator fun compareTo(other: ULong): Int

    infix fun and(other: UnsignedNumber): UnsignedNumber
    infix fun or(other: UnsignedNumber): UnsignedNumber
    infix fun xor(other: UnsignedNumber): UnsignedNumber

    fun inv(): UnsignedNumber

    fun toUByte(): UByte
    fun toUShort(): UShort
    fun toUInt(): UInt
    fun toULong(): ULong
    fun toFloat(): Float
    fun toDouble(): Double

    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
}
