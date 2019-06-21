/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Double_isNaN")
public actual external fun Double.isNaN(): Boolean

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Float_isNaN")
public actual external fun Float.isNaN(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Double_isInfinite")
public actual external fun Double.isInfinite(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Float_isInfinite")
public actual external fun Float.isInfinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Double_isFinite")
public actual external fun Double.isFinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Float_isFinite")
public actual external fun Float.isFinite(): Boolean

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.toBits(): Long = if (isNaN()) Double.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.toRawBits(): Long = bits()

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.Companion.fromBits(bits: Long): Double = kotlin.fromBits(bits)

@PublishedApi
@TypedIntrinsic(IntrinsicType.REINTERPRET)
internal external fun fromBits(bits: Long): Double

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.toBits(): Int = if (isNaN()) Float.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.toRawBits(): Int = bits()

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.Companion.fromBits(bits: Int): Float = kotlin.fromBits(bits)

@PublishedApi
@TypedIntrinsic(IntrinsicType.REINTERPRET)
internal external fun fromBits(bits: Int): Float


/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countOneBits(): Int {
    var v = this
    v = (v and 0x55555555) + (v.ushr(1) and 0x55555555)
    v = (v and 0x33333333) + (v.ushr(2) and 0x33333333)
    v = (v and 0x0F0F0F0F) + (v.ushr(4) and 0x0F0F0F0F)
    v = (v and 0x00FF00FF) + (v.ushr(8) and 0x00FF00FF)
    v = (v and 0x0000FFFF) + (v.ushr(16))
    return v
}

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countLeadingZeroBits(): Int = TODO()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.countTrailingZeroBits(): Int = TODO()
//        Int.SIZE_BITS - (this or -this).inv().countLeadingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.takeHighestOneBit(): Int =
        if (this == 0) 0 else 1.shl(Int.SIZE_BITS - 1 - countLeadingZeroBits())

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Int.takeLowestOneBit(): Int =
        this and -this

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
public actual fun Int.rotateLeft(bitCount: Int): Int =
        shl(bitCount) or ushr(Int.SIZE_BITS - bitCount)


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
public actual fun Int.rotateRight(bitCount: Int): Int =
        shl(Int.SIZE_BITS - bitCount) or ushr(bitCount)


/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countOneBits(): Int = TODO()
//        high.countOneBits() + low.countOneBits()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countLeadingZeroBits(): Int = TODO()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.countTrailingZeroBits(): Int = TODO()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.takeHighestOneBit(): Long = TODO()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun Long.takeLowestOneBit(): Long =
        this and -this

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
public actual fun Long.rotateLeft(bitCount: Int): Long =
        shl(bitCount) or ushr(Long.SIZE_BITS - bitCount)

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
@kotlin.internal.InlineOnly
public actual inline fun Long.rotateRight(bitCount: Int): Long =
        shl(Long.SIZE_BITS - bitCount) or ushr(bitCount)
