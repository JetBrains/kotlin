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
        assertFailsWith<ArithmeticException> { 1u.floorDiv(0u) }
    }

    @Sample
    fun modUnsigned() {
        // Unsigned types have no negative values, so mod produces the
        // same result as the % operator
        assertPrints(7u.mod(3u), "1")
        assertPrints(7u % 3u, "1")

        // A zero divisor throws
        assertFailsWith<ArithmeticException> { 1u.mod(0u) }
    }
}

