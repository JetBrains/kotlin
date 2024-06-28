/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

/**
 * Counts the number of set bits in the binary representation of this [Int] number.
 */
@WasmOp(WasmOp.I32_POPCNT)
public actual fun Int.countOneBits(): Int =
    implementedAsIntrinsic

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Int] number.
 */
@WasmOp(WasmOp.I32_CLZ)
public actual fun Int.countLeadingZeroBits(): Int =
    implementedAsIntrinsic

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Int] number.
 */
@WasmOp(WasmOp.I32_CTZ)
public actual fun Int.countTrailingZeroBits(): Int =
    implementedAsIntrinsic

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
public actual fun Int.takeHighestOneBit(): Int =
    if (this == 0) 0 else 1.shl(32 - 1 - countLeadingZeroBits())

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Int] number,
 * or zero, if this number is zero.
 */
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
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
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Int.rotateRight(bitCount: Int): Int =
    shl(32 - bitCount) or ushr(bitCount)

/**
 * Counts the number of set bits in the binary representation of this [Long] number.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Long.countOneBits(): Int =
    wasm_i64_popcnt(this).toInt()

/**
 * Counts the number of consecutive most significant bits that are zero in the binary representation of this [Long] number.
 */
public actual fun Long.countLeadingZeroBits(): Int = wasm_i64_clz(this).toInt()

/**
 * Counts the number of consecutive least significant bits that are zero in the binary representation of this [Long] number.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Long.countTrailingZeroBits(): Int =
    wasm_i64_ctz(this).toInt()

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
public actual fun Long.takeHighestOneBit(): Long =
    if (this == 0L) 0L else 1L.shl(64 - 1 - countLeadingZeroBits())

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [Long] number,
 * or zero, if this number is zero.
 */
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
public actual inline fun Long.rotateRight(bitCount: Int): Long =
    shl(64 - bitCount) or ushr(bitCount)

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
public actual fun Double.isInfinite(): Boolean = (this == Double.POSITIVE_INFINITY) || (this == Double.NEGATIVE_INFINITY)

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public actual fun Float.isInfinite(): Boolean = (this == Float.POSITIVE_INFINITY) || (this == Float.NEGATIVE_INFINITY)

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
public actual fun Double.toBits(): Long = if (isNaN()) Double.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toRawBits(): Long = wasm_i64_reinterpret_f64(this)

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Double.Companion.fromBits(bits: Long): Double = wasm_f64_reinterpret_i64(bits)

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toBits(): Int = if (isNaN()) Float.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toRawBits(): Int = wasm_i32_reinterpret_f32(this)

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Float.Companion.fromBits(bits: Int): Float = wasm_f32_reinterpret_i32(bits)