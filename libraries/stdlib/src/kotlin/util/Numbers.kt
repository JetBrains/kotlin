/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("NumbersKt")
package kotlin

/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
public expect fun Int.countOneBits(): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
public expect fun Int.countLeadingZeroBits(): Int

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
public expect fun Int.countTrailingZeroBits(): Int

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public expect fun Int.takeHighestOneBit(): Int

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public expect fun Int.takeLowestOneBit(): Int

/**
 * Rotates the binary representation of this [Int] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [Int.SIZE_BITS] (32) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 32)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun Int.rotateLeft(bitCount: Int): Int


/**
 * Rotates the binary representation of this [Int] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [Int.SIZE_BITS] (32) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 32)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun Int.rotateRight(bitCount: Int): Int


/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public expect fun Long.countOneBits(): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public expect fun Long.countLeadingZeroBits(): Int

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public expect fun Long.countTrailingZeroBits(): Int

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public expect fun Long.takeHighestOneBit(): Long

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public expect fun Long.takeLowestOneBit(): Long

/**
 * Rotates the binary representation of this [Long] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [Long.SIZE_BITS] (64) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 64)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun Long.rotateLeft(bitCount: Int): Long

/**
 * Rotates the binary representation of this [Long] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [Long.SIZE_BITS] (64) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 64)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun Long.rotateRight(bitCount: Int): Long

/**
 * Counts the number of set bits in the binary representation of this [Byte] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Byte.countOneBits(): Int = (toInt() and 0xFF).countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Byte] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Byte.countLeadingZeroBits(): Int = (toInt() and 0xFF).countLeadingZeroBits() - (Int.SIZE_BITS - Byte.SIZE_BITS)

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Byte] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Byte.countTrailingZeroBits(): Int = (toInt() or 0x100).countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Byte] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Byte.takeHighestOneBit(): Byte = (toInt() and 0xFF).takeHighestOneBit().toByte()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Byte] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Byte.takeLowestOneBit(): Byte = toInt().takeLowestOneBit().toByte()


/**
 * Rotates the binary representation of this [Byte] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [Byte.SIZE_BITS] (8) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 8)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public fun Byte.rotateLeft(bitCount: Int): Byte =
    (toInt().shl(bitCount and 7) or (toInt() and 0xFF).ushr(8 - (bitCount and 7))).toByte()

/**
 * Rotates the binary representation of this [Byte] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [Byte.SIZE_BITS] (8) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 8)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public fun Byte.rotateRight(bitCount: Int): Byte =
    (toInt().shl(8 - (bitCount and 7)) or (toInt() and 0xFF).ushr(bitCount and 7)).toByte()

/**
 * Counts the number of set bits in the binary representation of this [Short] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Short.countOneBits(): Int = (toInt() and 0xFFFF).countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Short] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Short.countLeadingZeroBits(): Int =
    (toInt() and 0xFFFF).countLeadingZeroBits() - (Int.SIZE_BITS - Short.SIZE_BITS)

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Short] number.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Short.countTrailingZeroBits(): Int = (toInt() or 0x10000).countTrailingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Short] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Short.takeHighestOneBit(): Short = (toInt() and 0xFFFF).takeHighestOneBit().toShort()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Short] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Short.takeLowestOneBit(): Short = toInt().takeLowestOneBit().toShort()


/**
 * Rotates the binary representation of this [Short] number left by the specified [bitCount] number of bits.
 * The most significant bits pushed out from the left side reenter the number as the least significant bits on the right side.
 *
 * Rotating the number left by a negative bit count is the same as rotating it right by the negated bit count:
 * `number.rotateLeft(-n) == number.rotateRight(n)`
 *
 * Rotating by a multiple of [Short.SIZE_BITS] (16) returns the same number, or more generally
 * `number.rotateLeft(n) == number.rotateLeft(n % 16)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public fun Short.rotateLeft(bitCount: Int): Short =
    (toInt().shl(bitCount and 15) or (toInt() and 0xFFFF).ushr(16 - (bitCount and 15))).toShort()

/**
 * Rotates the binary representation of this [Short] number right by the specified [bitCount] number of bits.
 * The least significant bits pushed out from the right side reenter the number as the most significant bits on the left side.
 *
 * Rotating the number right by a negative bit count is the same as rotating it left by the negated bit count:
 * `number.rotateRight(-n) == number.rotateLeft(n)`
 *
 * Rotating by a multiple of [Short.SIZE_BITS] (16) returns the same number, or more generally
 * `number.rotateRight(n) == number.rotateRight(n % 16)`
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public fun Short.rotateRight(bitCount: Int): Short =
    (toInt().shl(16 - (bitCount and 15)) or (toInt() and 0xFFFF).ushr(bitCount and 15)).toShort()
