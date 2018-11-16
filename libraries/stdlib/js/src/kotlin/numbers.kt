/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
@library("doubleToBits")
public actual fun Double.toBits(): Long = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@library("doubleToRawBits")
public actual fun Double.toRawBits(): Long = definedExternally

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.Companion.fromBits(bits: Long): Double = js("Kotlin").doubleFromBits(bits).unsafeCast<Double>()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToBits")
public actual fun Float.toBits(): Int = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToRawBits")
public actual fun Float.toRawBits(): Int = definedExternally

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.Companion.fromBits(bits: Int): Float = js("Kotlin").floatFromBits(bits).unsafeCast<Float>()

/**
 * Returns the number of one-bits in the two's complement binary representation of the specified [Int] value.
 */
public actual fun Int.bits(): Int {
    var i = this
    i -= (i.ushr(1) and 0x55555555)
    i = (i and 0x33333333) + (i.ushr(2) and 0x33333333)
    i = (i + i.ushr(4)) and 0x0f0f0f0f
    i += i.ushr(8)
    i += i.ushr(16)
    return i and 0x3f
}

/**
 * Returns the number of one-bits in the two's complement binary representation of the specified [Long] value.
 *
 * TODO: kotlin stdlib-js should use `getHighBits` and `getLowBits` by internal method, not dynamic.
 */
public actual fun Long.bits(): Int = (this.asDynamic().getHighBits() as Int).bits() + (this.asDynamic().getLowBits() as Int).bits()

/**
 * @return The number of zero bits following the lowest-order ("rightmost")
 * one-bit in the two's complement binary representation of the specified [Int] value.
 * Returns 32 if the specified value has no one-bits in its two's complement representation,
 * in other words if it is equal to zero.
 */
public actual fun Int.trailingZeros(): Int {
    if (this == 0) {
        return 32
    }

    var i = this
    var n = 31
    var j = 16
    var y: Int

    while (j > 1) {
        y = i shl j
        if (y != 0) {
            n -= j
            i = y
        }
        j = j ushr 1
    }

    return n - (i shl 1).ushr(31)
}

/**
 * TODO: kotlin stdlib-js should use `getHighBits` and `getLowBits` by internal method, not dynamic.
 *
 * @return The number of zero bits following the lowest-order ("rightmost")
 * one-bit in the two's complement binary representation of the specified [Long] value.
 * Returns 64 if the specified value has no one-bits in its two's complement representation,
 * in other words if it is equal to zero.
 */
public actual fun Long.trailingZeros(): Int {
    val low = (this.asDynamic().getLowBits() as Int).trailingZeros()

    return if (low < 32) {
        low
    } else { // is all zero-bits
        (this.asDynamic().getHighBits() as Int).trailingZeros() + low
    }
}
