/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.internal.GCCritical
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Double_isNaN")
@GCCritical
public actual external fun Double.isNaN(): Boolean

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Float_isNaN")
@GCCritical
public actual external fun Float.isNaN(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Double_isInfinite")
@GCCritical
public actual external fun Double.isInfinite(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Float_isInfinite")
@GCCritical
public actual external fun Float.isInfinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Double_isFinite")
@GCCritical
public actual external fun Double.isFinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Float_isFinite")
@GCCritical
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

// TODO: Replace 32 and 64 literals with Int/Long.SIZE_BITS constants when constant propagation is working

/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
@SymbolName("Kotlin_Int_countOneBits")
@GCCritical
public actual external fun Int.countOneBits(): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of [Int] [value].
 * Returns undefined result for zero [value].
 */
@SymbolName("Kotlin_Int_countLeadingZeroBits")
@GCCritical
private external fun countLeadingZeroBits(value: Int): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Int.countLeadingZeroBits(): Int =
        if (this == 0) 32 else countLeadingZeroBits(this)

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of [Int] [value].
 * Returns undefined result for zero [value].
 */
@SymbolName("Kotlin_Int_countTrailingZeroBits")
@GCCritical
private external fun countTrailingZeroBits(value: Int): Int

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Int.countTrailingZeroBits(): Int =
        if (this == 0) 32 else countTrailingZeroBits(this)

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Int.takeHighestOneBit(): Int =
        if (this == 0) 0 else 1.shl(32 - 1 - countLeadingZeroBits(this))

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
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
        shl(bitCount) or ushr(32 - bitCount)


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
        shl(32 - bitCount) or ushr(bitCount)


/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
@SymbolName("Kotlin_Long_countOneBits")
@GCCritical
public actual external fun Long.countOneBits(): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of [Long] [value].
 * Returns undefined result for zero [value].
 */
@SymbolName("Kotlin_Long_countLeadingZeroBits")
@GCCritical
private external fun countLeadingZeroBits(value: Long): Int

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Long.countLeadingZeroBits(): Int =
        if (this == 0L) 64 else countLeadingZeroBits(this)

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of [Long] [value].
 * Returns undefined result for zero [value].
 */
@SymbolName("Kotlin_Long_countTrailingZeroBits")
@GCCritical
private external fun countTrailingZeroBits(value: Long): Int

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Long.countTrailingZeroBits(): Int =
        if (this == 0L) 64 else countTrailingZeroBits(this)

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Long.takeHighestOneBit(): Long =
        if (this == 0L) 0L else 1L.shl(64 - 1 - countLeadingZeroBits(this))

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
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
        shl(bitCount) or ushr(64 - bitCount)

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
        shl(64 - bitCount) or ushr(bitCount)
