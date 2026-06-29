/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly

// CHANGES IN THIS FILE SHOULD BE SYNCED WITH THE SAME CHANGES IN: UnsignedJVM.kt and kotlin-native/Unsigned.kt

/* Several algorithms come from "the JS Long paper":
 *   S. Doeraene and T. Schlatter,
 *   "64-bit Integer Division for the JavaScript Platform,"
 *   33rd IEEE Symposium on Computer Arithmetic (ARITH), Fulda, Germany, 2026.
 *   https://arith2026.org/papers/64-bit%20Integer%20Division%20for%20the%20JavaScript%20Platform.pdf
 */


private const val TWO_PWR_32_DBL_ = 4294967296.0

private const val TWO_PWR_M32_DBL_ = 1.0 / TWO_PWR_32_DBL_

@InlineOnly
private inline fun jsToInt32(x: Double): Int = js("x | 0")

@InlineOnly
private inline fun jsToString(x: Double, base: Int): String = js("x.toString(base)")

@PublishedApi
internal actual fun uintRemainder(v1: UInt, v2: UInt): UInt {
    val d1 = uintToDouble(v1.toInt()).unsafeCast<Int>()
    val d2 = uintToDouble(v2.toInt()).unsafeCast<Int>()
    return (d1 % d2).toUInt()
}

@PublishedApi
internal actual fun uintDivide(v1: UInt, v2: UInt): UInt {
    val d1 = uintToDouble(v1.toInt()).unsafeCast<Int>()
    val d2 = uintToDouble(v2.toInt()).unsafeCast<Int>()
    return (d1 / d2).toUInt()
}

@PublishedApi
internal actual fun ulongDivide(v1: ULong, v2: ULong): ULong {
    // JS Long paper, Section VI, Algorithm 6, specialized to get the quotient

    val a = v1.toLong()
    val b = v2.toLong()
    val alo = a.toInt()
    val ahi = (a ushr 32).toInt()
    val blo = b.toInt()
    val bhi = (b ushr 32).toInt()

    /* Conveniently, when b = 0, we enter the first case, where the first thing
     * we do is an int division by blo. If that throws an ArithmeticException,
     * we will throw it as well. If it ignores it and returns 0, we'll also
     * compute a 0 quotient, so we're in sync with the behavior of Int division.
     */

    if (bothZero(bhi, blo and 0xffe00000.toInt())) {
        // b < 2^21, Algorithm 4
        val quotHi = (ahi.toUInt() / blo.toUInt()).toInt()
        val k = ahi - blo * quotHi
        val quotLo = jsToInt32(toUnsignedNumber(alo, k) / blo.toDouble())
        return makeULong(quotLo, quotHi)
    } else if (bhi >= 0) {
        // 2^21 <= b < 2^63, Algorithm 5
        val aHat = toUnsignedNumber(alo, ahi)
        val bHat = toUnsignedNumber(blo, bhi)
        val qHat = ulongFromUnsignedSafeDouble((aHat / bHat) + 0.00390625).toLong() // 2^(-8)
        val rHat = a - b * qHat
        return if (rHat < 0L) (qHat - 1L).toULong() else qHat.toULong()
    } else {
        if (v1 >= v2) {
            return 1UL
        } else {
            return 0UL
        }
    }
}

@PublishedApi
internal actual fun ulongRemainder(v1: ULong, v2: ULong): ULong {
    // JS Long paper, Section VI, Algorithm 6, specialized to get the remainder

    val a = v1.toLong()
    val b = v2.toLong()
    val alo = a.toInt()
    val ahi = (a ushr 32).toInt()
    val blo = b.toInt()
    val bhi = (b ushr 32).toInt()

    // See ulongDivide about division by 0

    if (bothZero(bhi, blo and 0xffe00000.toInt())) {
        // b < 2^21, Algorithm 4
        val k = (ahi.toUInt() % blo.toUInt()).toInt()
        val quotLo = jsToInt32(toUnsignedNumber(alo, k) / blo.toDouble())
        val remLo = alo - blo * quotLo
        return makeULong(remLo, 0)
    } else if (bhi >= 0) {
        // 2^21 <= b < 2^63, Algorithm 5
        val aHat = toUnsignedNumber(alo, ahi)
        val bHat = toUnsignedNumber(blo, bhi)
        val qHat = ulongFromUnsignedSafeDouble((aHat / bHat) + 0.00390625).toLong() // 2^(-8)
        val rHat = a - b * qHat
        return if (rHat < 0L) (rHat + b).toULong() else rHat.toULong()
    } else {
        if (v1 >= v2) {
            return v1 - v2
        } else {
            return v1
        }
    }
}

@InlineOnly
private inline fun bothZero(a: Int, b: Int): Boolean = (a or b) == 0

@InlineOnly
private inline fun makeULong(low: Int, high: Int): ULong =
    low.toUInt().toULong() or (high.toUInt().toULong() shl 32)

@PublishedApi
internal actual fun uintCompare(v1: Int, v2: Int): Int = when {
    v1 == v2 -> 0
    uintToDouble(v1) < uintToDouble(v2) -> -1
    else -> 1
}

@PublishedApi
internal actual fun ulongCompare(v1: Long, v2: Long): Int = (v1 xor Long.MIN_VALUE).compareTo(v2 xor Long.MIN_VALUE)

@PublishedApi
@InlineOnly
internal actual inline fun uintToULong(value: Int): ULong = ULong(uintToLong(value))

@PublishedApi
@InlineOnly
internal actual inline fun uintToLong(value: Int): Long = value.toLong() and 0xFFFF_FFFF

@PublishedApi
@InlineOnly
internal actual inline fun uintToFloat(value: Int): Float = uintToDouble(value).toFloat()

@PublishedApi
@InlineOnly
internal actual inline fun floatToUInt(value: Float): UInt = doubleToUInt(value.toDouble())

// For JS engines, the operation `value >>> 0` is a no-op at run-time.
// It only changes the internal type information of the JIT to an unsigned 32-bit integer.
// That makes this operation, and subsequent operations on its result, very efficient.
@InlineOnly
@PublishedApi
internal actual inline fun uintToDouble(value: Int): Double = js("value >>> 0")

@PublishedApi
internal actual fun doubleToUInt(value: Double): UInt = when {
    value <= UInt.MIN_VALUE.toDouble() -> UInt.MIN_VALUE
    value >= UInt.MAX_VALUE.toDouble() -> UInt.MAX_VALUE
    else -> jsToInt32(value).toUInt()
}

@PublishedApi
@InlineOnly
internal actual inline fun ulongToFloat(value: Long): Float = ulongToDouble(value).toFloat()

@PublishedApi
@InlineOnly
internal actual inline fun floatToULong(value: Float): ULong = doubleToULong(value.toDouble())

@PublishedApi
internal actual fun ulongToDouble(value: Long): Double =
    toUnsignedNumber(value.toInt(), (value ushr 32).toInt())

@InlineOnly
private inline fun toUnsignedNumber(low: Int, high: Int): Double {
    // See the JS Long paper, Section V.A.
    return uintToDouble(high) * TWO_PWR_32_DBL_ + uintToDouble(low)
}

@PublishedApi
internal actual fun doubleToULong(value: Double): ULong = when {
    value <= ULong.MIN_VALUE.toDouble() -> ULong.MIN_VALUE
    value >= ULong.MAX_VALUE.toDouble() -> ULong.MAX_VALUE
    else -> ulongFromUnsignedSafeDouble(value)
}

@InlineOnly
private inline fun ulongFromUnsignedSafeDouble(value: Double): ULong {
    // JS Long paper, Section V.B.
    return makeULong(jsToInt32(value), jsToInt32(value * TWO_PWR_M32_DBL_))
}

@InlineOnly
internal actual inline fun uintToString(value: Int): String = uintToDouble(value).toString()

@InlineOnly
internal actual inline fun uintToString(value: Int, base: Int): String =
  jsToString(uintToDouble(value), base)

/* Conversion from ulong to string.
 * See the JS Long paper, Section VIII.
 */

private final class ToStringTableEntry(
    val w: Int,
    val d: Int,
    val mHat: Double,
    val paddingZeros: String
)

private fun makeToStringTable(): Array<ToStringTableEntry?> {
    val r: Array<ToStringTableEntry?> = arrayOfNulls<ToStringTableEntry>(37)

    for (base in 2..36) {
        /* - d must be the biggest exact power of base that is <= 2^30.
         * - w is then log_radix(d).
         * - paddingZeros is a string with exactly w '0's.
         * - mHat = (1.0 / d.toDouble()) + 2^(-75).
         */
        val barrier = (1 shl 30) / base
        var d = base
        var w = 1
        var paddingZeros = "0"
        while (d <= barrier) {
            d *= base
            w += 1
            paddingZeros += "0"
        }
        val mHat = (1.0 / d.toDouble()) + 2.6469779601696886e-23 // 2^(-75)
        r[base] = ToStringTableEntry(w, d, mHat, paddingZeros)
    }

    return r
}

@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
private val ToStringTable: Array<ToStringTableEntry?> = makeToStringTable()

@InlineOnly
internal actual inline fun ulongToString(value: Long): String = ulongToString(value, 10)

internal actual fun ulongToString(value: Long, base: Int): String {
    // JS Long paper, Algorithm 10

    val low = value.toInt()
    val high = (value ushr 32).toInt()

    if (high == 0) {
        // value < 2^32
        return jsToString(uintToDouble(low), base)
    } else if ((high and 0xffe00000.toInt()) == 0) {
        // value < 2^53
        return jsToString(ulongToDouble(value), base)
    } else {
        // value >= 2^53
        return ulongToStringLarge(value, base)
    }
}

/**
 * Converts an unsigned long >= 2^53 to string.
 */
internal fun ulongToStringLarge(value: Long, base: Int): String {
    // JS Long paper, Algorithm 12

    val entry = ToStringTable[base]!!
    val d = entry.d

    // initial approximation of the quotient and remainder
    val aHat = ulongToDouble(value)
    var qHat = kotlin.math.floor(aHat * entry.mHat)
    var rHat = value.toInt() - d * jsToInt32(qHat)

    // correct the approximations
    if (rHat < 0) {
        qHat -= 1.0
        rHat += d
    }

    // build the result string
    val qStr = jsToString(qHat, base)
    val rStr = jsToString(rHat.toDouble(), base)
    return qStr + entry.paddingZeros.substring(rStr.length) + rStr
}
