/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("UNumbersKt")
package kotlin

/**
 * Counts the number of set bits in the binary representation of this [UInt] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.countOneBits(): Int = toInt().countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [UInt] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.countLeadingZeroBits(): Int = toInt().countLeadingZeroBits()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [UInt] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.countTrailingZeroBits(): Int = toInt().countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [UInt] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.takeHighestOneBit(): UInt = toInt().takeHighestOneBit().toUInt()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [UInt] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.takeLowestOneBit(): UInt = toInt().takeLowestOneBit().toUInt()

/**
 * Rotates the binary representation of this [UInt] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [UInt.SIZE_BITS] (32) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 32)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.rotateLeft(bitCount: Int): UInt = toInt().rotateLeft(bitCount).toUInt()


/**
 * Rotates the binary representation of this [UInt] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [UInt.SIZE_BITS] (32) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 32)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UInt.rotateRight(bitCount: Int): UInt = toInt().rotateRight(bitCount).toUInt()


/**
 * Counts the number of set bits in the binary representation of this [ULong] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.countOneBits(): Int = toLong().countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [ULong] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.countLeadingZeroBits(): Int = toLong().countLeadingZeroBits()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [ULong] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.countTrailingZeroBits(): Int = toLong().countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [ULong] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.takeHighestOneBit(): ULong = toLong().takeHighestOneBit().toULong()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [ULong] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.takeLowestOneBit(): ULong = toLong().takeLowestOneBit().toULong()

/**
 * Rotates the binary representation of this [ULong] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [ULong.SIZE_BITS] (64) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 64)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.rotateLeft(bitCount: Int): ULong = toLong().rotateLeft(bitCount).toULong()

/**
 * Rotates the binary representation of this [ULong] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [ULong.SIZE_BITS] (64) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 64)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun ULong.rotateRight(bitCount: Int): ULong = toLong().rotateRight(bitCount).toULong()

/**
 * Counts the number of set bits in the binary representation of this [UByte] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.countOneBits(): Int = toUInt().countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [UByte] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.countLeadingZeroBits(): Int = toByte().countLeadingZeroBits()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [UByte] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.countTrailingZeroBits(): Int = toByte().countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [UByte] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.takeHighestOneBit(): UByte = toInt().takeHighestOneBit().toUByte()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [UByte] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.takeLowestOneBit(): UByte = toInt().takeLowestOneBit().toUByte()


/**
 * Rotates the binary representation of this [UByte] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [UByte.SIZE_BITS] (8) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 8)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.rotateLeft(bitCount: Int): UByte = toByte().rotateLeft(bitCount).toUByte()

/**
 * Rotates the binary representation of this [UByte] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [UByte.SIZE_BITS] (8) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 8)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UByte.rotateRight(bitCount: Int): UByte = toByte().rotateRight(bitCount).toUByte()

/**
 * Counts the number of set bits in the binary representation of this [UShort] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.countOneBits(): Int = toUInt().countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [UShort] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.countLeadingZeroBits(): Int = toShort().countLeadingZeroBits()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [UShort] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.countTrailingZeroBits(): Int = toShort().countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [UShort] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.takeHighestOneBit(): UShort = toInt().takeHighestOneBit().toUShort()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [UShort] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.takeLowestOneBit(): UShort = toInt().takeLowestOneBit().toUShort()


/**
 * Rotates the binary representation of this [UShort] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [UShort.SIZE_BITS] (16) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 16)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.rotateLeft(bitCount: Int): UShort = toShort().rotateLeft(bitCount).toUShort()

/**
 * Rotates the binary representation of this [UShort] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [UShort.SIZE_BITS] (16) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 16)`
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun UShort.rotateRight(bitCount: Int): UShort = toShort().rotateRight(bitCount).toUShort()
