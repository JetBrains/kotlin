/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

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
    }

    @Sample
    fun mod() {
        // Regular remainder (%) takes the sign of the dividend
        assertPrints((-7) % 3, "-1")
        // mod takes the sign of the divisor
        assertPrints((-7).mod(3), "2")
        assertPrints(7.mod(3), "1")
        assertPrints(7.mod(-3), "-2")
    }

    @Sample
    fun floorDivLong() {
        assertPrints((-7L).floorDiv(3L), "-3")
    }

    @Sample
    fun modFloat() {
        // Floating-point mod also takes the sign of the divisor
        assertPrints((-1.5).mod(1.0), "0.5")
    }
}

