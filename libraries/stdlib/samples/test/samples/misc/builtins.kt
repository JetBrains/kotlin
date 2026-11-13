/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import kotlin.test.assertFailsWith

class Builtins {

    @Sample
    fun inc() {
        val a = 3
        val b = a.inc()
        assertPrints(a, "3")
        assertPrints(b, "4")

        var x = 3
        val y = x++
        assertPrints(x, "4")
        assertPrints(y, "3")

        val z = ++x
        assertPrints(x, "5")
        assertPrints(z, "5")
    }

    @Sample
    fun dec() {
        val a = 3
        val b = a.dec()
        assertPrints(a, "3")
        assertPrints(b, "2")

        var x = 3
        val y = x--
        assertPrints(x, "2")
        assertPrints(y, "3")

        val z = --x
        assertPrints(x, "1")
        assertPrints(z, "1")
    }

    @Sample
    fun floorDiv() {
        // Regular integer division (/) truncates toward zero
        assertPrints((-7) / 3, "-2")
        // floorDiv rounds toward negative infinity
        assertPrints((-7).floorDiv(3), "-3")
        assertPrints(7.floorDiv(3), "2")
        assertPrints(7.floorDiv(-3), "-3")

        // Dividing by zero throws, just like the / operator
        @Suppress("DIVISION_BY_ZERO")
        assertFailsWith<ArithmeticException> { 1.floorDiv(0) }
    }

    @Sample
    fun mod() {
        // Regular remainder (%) takes the sign of the dividend
        assertPrints((-7) % 3, "-1")
        // mod takes the sign of the divisor
        assertPrints((-7).mod(3), "2")
        assertPrints(7.mod(3), "1")
        assertPrints(7.mod(-3), "-2")

        // A zero divisor throws, just like the % operator
        @Suppress("DIVISION_BY_ZERO")
        assertFailsWith<ArithmeticException> { 1.mod(0) }
    }

    @Sample
    fun modFloat() {
        // For finite arguments, the result has the same sign as the divisor
        assertPrints((-7.5).mod(3.0), "1.5")
        assertPrints(7.5.mod(-3.0), "-1.5")

        // The divisor is not required to be an integer
        assertPrints(5.0.mod(1.1), "0.5999999999999996")

        // A negative-zero dividend keeps its sign
        assertPrints((-0.0).mod(3.0), "-0.0")

        // Unlike integer mod, a zero divisor produces NaN instead of throwing
        @Suppress("DIVISION_BY_ZERO")
        assertPrints(5.0.mod(0.0), "NaN")

        // If either argument is NaN, or the dividend is infinite, the result is NaN
        assertPrints(Double.NaN.mod(3.0), "NaN")
        assertPrints(Double.POSITIVE_INFINITY.mod(3.0), "NaN")

        // If only the divisor is infinite, the result is the dividend when the
        // signs agree, and the infinite divisor otherwise
        assertPrints(3.0.mod(Double.POSITIVE_INFINITY), "3.0")
        assertPrints((-3.0).mod(Double.POSITIVE_INFINITY), "Infinity")
    }

    @Sample
    fun floorDivUnsigned() {
        // Unsigned types have no negative values, so floorDiv produces the
        // same result as the / operator
        assertPrints(7u.floorDiv(3u), "2")
        assertPrints(7u / 3u, "2")

        // Dividing by zero throws
        @Suppress("DIVISION_BY_ZERO")
        assertFailsWith<ArithmeticException> { 1u.floorDiv(0u) }
    }

    @Sample
    fun modUnsigned() {
        // Unsigned types have no negative values, so mod produces the
        // same result as the % operator
        assertPrints(7u.mod(3u), "1")
        assertPrints(7u % 3u, "1")

        // A zero divisor throws
        @Suppress("DIVISION_BY_ZERO")
        assertFailsWith<ArithmeticException> { 1u.mod(0u) }
    }

    @Sample
    fun shl() {
        // 0001 -> 1000, which is 8 in binary
        assertPrints(1 shl 3, "8")
        // In most cases, shl 1 is equivalent to multiplying by two
        assertPrints(13 shl 1, "26")

        // This also works for negative numbers
        assertPrints(-1 shl 1, "-2")
        assertPrints(-256 shl 2, "-1024")

        // However, if first bit of the number becomes 1, number overflows into negatives
        assertPrints(1 shl 31, "-2147483648")

        // only the five lowest-order bits of the bitCount are used, so `shl 32` won't work
        assertPrints(1 shl 32, "1")

        // This also means `shl -1` is equivalent to `shl 31`. not `shr 1`
        assertPrints(2 shl -1, "0")
        assertPrints(1 shl -31, "2")
    }

    @Sample
    fun shr() {
        // 1000 -> 0001
        assertPrints(8 shr 3, "1")
        // In most cases, shl 1 is equivalent to dividing by two (rounding down)
        assertPrints(13 shr 1, "6")

        // This also works for negative numbers.
        assertPrints(-2 shr 1, "-1")
        assertPrints(-256 shr 2, "-64")

        // Note that regular division rounds negative numbers up
        assertPrints(-13 shr 1, "-7")
        assertPrints(-13 / 2, "-6")

        // only the five lowest-order bits of the bitCount are used, so `shr 32` won't work
        assertPrints(1 shr 32, "1")
        // This also means `shr -1` is equivalent to `shr 31`. not `shl 1`
        assertPrints(2 shr -31, "1")
    }

    @Sample
    fun ushr() {
        // 1000 -> 0001
        assertPrints(8 ushr 3, "1")
        // In most cases, shl 1 is equivalent to dividing by two (rounding down)
        assertPrints(13 ushr 1, "6")

        // ushr replaces leftmost bits with zeros, so negative numbers become positive.
        assertPrints(-1 ushr 1, "2147483647")

        // only the five lowest-order bits of the bitCount are used, so `shr 32` won't work
        assertPrints(1 ushr 32, "1")
        // This also means `shr -1` is equivalent to `shr 31`. not `shl 1`
        assertPrints(2 ushr -31, "1")
    }

    @Sample
    fun and() {
        assertPrints(0 and 6, "0")
        // 5 = 0101, 6 = 0110
        assertPrints(5 and 6, "4")
        // -1 = 11..1
        assertPrints(1 and -1, "1")
    }

    @Sample
    fun or() {
        assertPrints(0 or 6, "6")
        // 5 = 0101, 6 = 0110
        assertPrints(5 or 6, "7")
        // -1 = 11..1
        assertPrints(1 or -1, "-1")
    }

    @Sample
    fun xor() {
        assertPrints(0 xor 6, "6")
        // 5 = 0101, 6 = 0110
        assertPrints(5 xor 6, "3")
        // -1 = 11..1
        assertPrints(1 xor -1, "-2")
    }

    @Sample
    fun inv() {
        // -1 = 11..1
        assertPrints(0.inv(), "-1")
        // x.inv() = -x - 1
        assertPrints(100.inv(), "-101")
        assertPrints((-100).inv(), "99")
    }
}

