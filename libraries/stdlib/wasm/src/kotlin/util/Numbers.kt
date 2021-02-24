/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countOneBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countLeadingZeroBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countTrailingZeroBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.takeHighestOneBit(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.takeLowestOneBit(): Int = TODO("Wasm stdlib: Numbers")

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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.rotateLeft(bitCount: Int): Int = TODO("Wasm stdlib: Numbers")


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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.rotateRight(bitCount: Int): Int = TODO("Wasm stdlib: Numbers")


/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countOneBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countLeadingZeroBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countTrailingZeroBits(): Int = TODO("Wasm stdlib: Numbers")

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.takeHighestOneBit(): Long = TODO("Wasm stdlib: Numbers")

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.takeLowestOneBit(): Long = TODO("Wasm stdlib: Numbers")

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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.rotateLeft(bitCount: Int): Long = TODO("Wasm stdlib: Numbers")

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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.rotateRight(bitCount: Int): Long = TODO("Wasm stdlib: Numbers")
