/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public actual fun Double.isNaN(): Boolean = this != this

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public actual fun Float.isNaN(): Boolean = this != this

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public actual fun Double.isInfinite(): Boolean = this == Double.POSITIVE_INFINITY || this == Double.NEGATIVE_INFINITY

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public actual fun Float.isInfinite(): Boolean = this == Float.POSITIVE_INFINITY || this == Float.NEGATIVE_INFINITY

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public actual fun Double.isFinite(): Boolean = !isInfinite() && !isNaN()

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public actual fun Float.isFinite(): Boolean = !isInfinite() && !isNaN()


/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toBits(): Long =
    doubleToRawBits(if (this.isNaN()) Double.NaN else this)

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toRawBits(): Long =
    doubleToRawBits(this)

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.Companion.fromBits(bits: Long): Double =
    doubleFromBits(bits)

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
public actual fun Float.toBits(): Int =
    floatToRawBits(if (this.isNaN()) Float.NaN else this)

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
public actual fun Float.toRawBits(): Int =
    floatToRawBits(this)

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.Companion.fromBits(bits: Int): Float =
    floatFromBits(bits)


/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
public actual fun Int.countOneBits(): Int {
    // Hacker's Delight 5-1 algorithm
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
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun Int.countLeadingZeroBits(): Int = nativeClz32(this)

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@SinceKotlin("1.4")
public actual fun Int.countTrailingZeroBits(): Int =
    // Hacker's Delight 5-4 algorithm for expressing countTrailingZeroBits with countLeadingZeroBits
    Int.SIZE_BITS - (this or -this).inv().countLeadingZeroBits()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public actual fun Int.takeHighestOneBit(): Int =
    if (this == 0) 0 else 1.shl(Int.SIZE_BITS - 1 - countLeadingZeroBits())

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public actual fun Int.takeLowestOneBit(): Int =
    // Hacker's Delight 2-1 algorithm for isolating rightmost 1-bit
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Int.rotateRight(bitCount: Int): Int =
    shl(Int.SIZE_BITS - bitCount) or ushr(bitCount)


/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public actual fun Long.countOneBits(): Int {
    // Hacker's Delight 5-1 algorithm
    var v = this
    v -= (v shr 1) and 0x5555555555555555L
    v = (v and 0x3333333333333333L) + ((v shr 2) and 0x5555555555555555L)
    v = (v + (v shr 4) and 0x0f0f0f0f0f0f0f0fL)
    v += v shr 8
    v += v shr 16
    v += v shr 32
    v = ((v and 0x00000000ffffffffL) * 0x022fdd63cc95386dL) shr 58
    return v.toInt()
}

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public actual fun Long.countLeadingZeroBits(): Int {
    for (i in 63 downTo 0) {
        if ((this shr i) and 1L == 0L) {
            return 63 - i
        }
    }
    return 64
}

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@SinceKotlin("1.4")
public actual fun Long.countTrailingZeroBits(): Int {
    if (this == 0L) return 64
    var count = 0
    var value = this
    while ((value and 1L) == 0L) {
        value = value shr 1
        count++
    }
    return count
}

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
public actual fun Long.takeHighestOneBit(): Long =
    if (this == 0L) 0 else 1L.shl(64 - 1 - countLeadingZeroBits())

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
@SinceKotlin("1.4")
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun Long.rotateRight(bitCount: Int): Long = rotateLeft(-bitCount)
