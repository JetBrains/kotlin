/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.math

import samples.*
import kotlin.math.*

class Maths {

    @Sample
    fun doubleSinCosAndTan() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000
        val ninetyDegreesInRadians: Double = PI / 2
        val sixtyDegreesInRadians: Double = PI / 3
        val fortyFiveDegreesInRadians: Double = PI / 4
        val thirtyDegreesInRadians: Double = PI / 6
        val sqrt2 = sqrt(2.0)
        val sqrt3 = sqrt(3.0)

        assertPrints(rnd(sin(ninetyDegreesInRadians)), "1.0")
        assertPrints(rnd(sin(sixtyDegreesInRadians)), "${rnd(sqrt3 / 2)}")
        assertPrints(rnd(sin(fortyFiveDegreesInRadians)), "${rnd(1 / sqrt2)}")

        assertPrints(rnd(cos(ninetyDegreesInRadians)), "0.0")
        assertPrints(rnd(cos(sixtyDegreesInRadians)), "0.5")
        assertPrints(rnd(cos(fortyFiveDegreesInRadians)), "${rnd(1 / sqrt2)}")

        assertPrints(rnd(tan(sixtyDegreesInRadians)), "${rnd(sqrt3)}")
        assertPrints(rnd(tan(fortyFiveDegreesInRadians)), "1.0")
        assertPrints(rnd(tan(thirtyDegreesInRadians)), "${rnd(1 / sqrt3)}")
    }

    @Sample
    fun doubleAsinAcosAndAtan() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000
        val ninetyDegreesInRadians: Double = PI / 2
        val sixtyDegreesInRadians: Double = PI / 3
        val fortyFiveDegreesInRadians: Double = PI / 4
        val thirtyDegreesInRadians: Double = PI / 6
        val sqrt2 = sqrt(2.0)
        val sqrt3 = sqrt(3.0)

        assertPrints(rnd(asin(1.0)), "${rnd(ninetyDegreesInRadians)}")
        assertPrints(rnd(asin(sqrt3 / 2)), "${rnd(sixtyDegreesInRadians)}")
        assertPrints(rnd(asin(1 / sqrt2)), "${rnd(fortyFiveDegreesInRadians)}")

        assertPrints(rnd(acos(0.0)), "${rnd(ninetyDegreesInRadians)}")
        assertPrints(rnd(acos(0.5)), "${rnd(sixtyDegreesInRadians)}")
        assertPrints(rnd(acos(1 / sqrt2)), "${rnd(fortyFiveDegreesInRadians)}")

        assertPrints(rnd(atan(sqrt3)), "${rnd(sixtyDegreesInRadians)}")
        assertPrints(rnd(atan(1.0)), "${rnd(fortyFiveDegreesInRadians)}")
        assertPrints(rnd(atan(1 / sqrt3)), "${rnd(thirtyDegreesInRadians)}")
    }
}