/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.math

import samples.*
import kotlin.math.*

class Doubles {

    @Sample
    fun sinCosAndTan() {
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
    fun asinAcosAndAtan() {
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

    @Sample
    fun atan2() {
        val ninetyDegreesInRadians: Double = PI / 2
        val sixtyDegreesInRadians: Double = PI / 3
        val sqrt3 = sqrt(3.0)

        assertPrints(atan2(1.0, 0.0), "$ninetyDegreesInRadians")
        assertPrints(atan2(sqrt3 / 2, 0.5), "$sixtyDegreesInRadians")
        assertPrints(atan2(0.0, 1.0), "0.0")
    }

    @Sample
    fun sinhCoshAndTanh() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000
        val goldenRatio = 2 * cos(PI / 5)
        val sqrt5 = sqrt(5.0)

        assertPrints(sinh(0.0), "0.0")
        assertPrints(sinh(ln(goldenRatio)), "0.5")

        assertPrints(cosh(0.0), "1.0")
        assertPrints(rnd(cosh(ln(goldenRatio))), "${rnd(0.5 * sqrt5)}")

        assertPrints(tanh(0.0), "0.0")
        assertPrints(rnd(tanh(ln(goldenRatio))), "${rnd(1.0 / 5 * sqrt5)}")
    }

    @Sample
    fun asinhAcoshAndAtanh() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000
        val goldenRatio = 2 * cos(PI / 5)
        val sqrt5 = sqrt(5.0)

        assertPrints(asinh(0.0), "0.0")
        assertPrints(rnd(asinh(0.5)), "${rnd(ln(goldenRatio))}")

        assertPrints(acosh(1.0), "0.0")
        assertPrints(rnd(acosh(0.5 * sqrt5)), "${rnd(ln(goldenRatio))}")

        assertPrints(atanh(0.0), "0.0")
        assertPrints(rnd(atanh(1.0 / 5 * sqrt5)), "${rnd(ln(goldenRatio))}")
    }

    @Sample
    fun hypot() {
        assertPrints(hypot(3.0, 4.0), "5.0")
        assertPrints(hypot(5.0, 12.0), "13.0")
        assertPrints(hypot(8.0, 15.0), "17.0")
    }

    @Sample
    fun sqrt() {
        assertPrints(sqrt(9.0), "3.0")
        assertPrints(sqrt(25.0), "5.0")
        assertPrints(sqrt(100.0), "10.0")
    }

    @Sample
    fun expAndExpm1() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000

        assertPrints(rnd(exp(2.0)), "${rnd(E.pow(2))}")
        assertPrints(rnd(exp(5.0)), "${rnd(E.pow(5))}")
        assertPrints(rnd(exp(10.0)), "${rnd(E.pow(10))}")

        assertPrints(expm1(1e-10), "1.00000000005E-10")
        assertPrints(expm1(1e-11), "1.000000000005E-11")
        assertPrints(expm1(1e-12), "1.0000000000005E-12")
    }

    @Sample
    fun logLnLog10Log2AndLn1p() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000

        assertPrints(rnd(log(100.0, 10.0)), "2.0")
        assertPrints(rnd(log(125.0, 5.0)), "3.0")
        assertPrints(rnd(log(823543.0, 7.0)), "7.0")

        assertPrints(ln(E), "1.0")
        assertPrints(rnd(ln(7.0)), "1.946")
        assertPrints(rnd(ln(0.004)), "-5.521")

        assertPrints(log10(1000.0), "3.0")
        assertPrints(log10(1e7), "7.0")
        assertPrints(rnd(log10(42.0)), "1.623")

        assertPrints(log2(4.0), "2.0")
        assertPrints(log2(16.0), "4.0")
        assertPrints(log2(128.0), "7.0")

        assertPrints(ln1p(1e-10), "9.999999999500001E-11")
        assertPrints(ln1p(1e-11), "9.999999999949999E-12")
        assertPrints(ln1p(1e-12), "9.999999999995E-13")
    }

    @Sample
    fun ceilFloorTruncateAndRound() {
        assertPrints(ceil(10.12), "11.0")
        assertPrints(ceil(99.99), "100.0")
        assertPrints(ceil(-13.5), "-13.0")

        assertPrints(floor(10.12), "10.0")
        assertPrints(floor(99.99), "99.0")
        assertPrints(floor(-13.5), "-14.0")

        assertPrints(truncate(10.12), "10.0")
        assertPrints(truncate(99.99), "99.0")
        assertPrints(truncate(-13.5), "-13.0")

        assertPrints(round(10.12), "10.0")
        assertPrints(round(99.99), "100.0")
        assertPrints(round(-13.5), "-14.0")
    }

    @Sample
    fun abs() {
        assertPrints(abs(10.12), "10.12")
        assertPrints(abs(-42.0), "42.0")
        assertPrints(abs(-E), "$E")

        assertPrints(10.12.absoluteValue, "10.12")
        assertPrints((-42.0).absoluteValue, "42.0")
        assertPrints((-E).absoluteValue, "$E")
    }

    @Sample
    fun sign() {
        assertPrints(sign(10.12), "1.0")
        assertPrints(sign(-42.0), "-1.0")
        assertPrints(sign(0.0), "0.0")

        assertPrints(10.12.sign, "1.0")
        assertPrints((-42.0).sign, "-1.0")
        assertPrints(0.0.sign, "0.0")
    }

    @Sample
    fun minAndMax() {
        assertPrints(min(10.12, 10.13), "10.12")
        assertPrints(min(-42.0, -43.0), "-43.0")
        assertPrints(min(1e-12, 1e-13), "1.0E-13")

        assertPrints(max(10.12, 10.13), "10.13")
        assertPrints(max(-42.0, -43.0), "-42.0")
        assertPrints(max(1e-12, 1e-13), "1.0E-12")
    }

    @Sample
    fun pow() {
        fun rnd(d: Double): Double = round(d * 1000) / 1000

        assertPrints(rnd(10.3.pow(2.5)), "340.481")
        assertPrints(rnd(2.1.pow(10.3)), "2083.813")
        assertPrints(rnd(5.0.pow(4.5)), "1397.542")

        assertPrints(rnd(10.3.pow(2)), "106.09")
        assertPrints(rnd(2.1.pow(10)), "1667.988")
        assertPrints(rnd(5.0.pow(4)), "625.0")
    }

    @Sample
    fun IEEErem() {
        assertPrints(10.0.IEEErem(4.0), "2.0")
        assertPrints(20.0.IEEErem(8.0), "4.0")
        assertPrints(3.0.IEEErem(2.0), "-1.0")
    }

    @Sample
    fun withSign() {
        assertPrints(10.0.withSign(1.0), "10.0")
        assertPrints(10.0.withSign(-9.0), "-10.0")
        assertPrints(10.0.withSign(0.0), "10.0")

        assertPrints(5.5.withSign(1), "5.5")
        assertPrints(5.5.withSign(-9), "-5.5")
        assertPrints(5.5.withSign(0), "5.5")
    }

    @Sample
    fun ulpNextUpNextDownAndNextTowards() {
        assertPrints(0.0.ulp, "${Double.MIN_VALUE}")
        assertPrints(1.0.ulp, "2.220446049250313E-16")
        assertPrints(10.0.ulp, "1.7763568394002505E-15")

        assertPrints(0.0.nextUp(), "${Double.MIN_VALUE}")
        assertPrints(1.0.nextUp(), "1.0000000000000002")
        assertPrints(10.0.nextUp(), "10.000000000000002")

        assertPrints(0.0.nextDown(), "${-Double.MIN_VALUE}")
        assertPrints(1.0.nextDown(), "0.9999999999999999")
        assertPrints(10.0.nextDown(), "9.999999999999998")

        assertPrints(0.0.nextTowards(1.0), "${Double.MIN_VALUE}")
        assertPrints(0.0.nextTowards(-1.0), "${-Double.MIN_VALUE}")
        assertPrints(10.0.nextTowards(Double.MIN_VALUE), "9.999999999999998")
    }

    @Sample
    fun roundToIntAndRoundToLong() {
        assertPrints(10.5.roundToInt(), "11")
        assertPrints(PI.roundToInt(), "3")
        assertPrints((Int.MAX_VALUE + 1.0).roundToInt(), "${Int.MAX_VALUE}")

        assertPrints(10.5.roundToLong(), "11")
        assertPrints(PI.roundToLong(), "3")
        assertPrints((Long.MAX_VALUE + 1.0).roundToLong(), "${Long.MAX_VALUE}")
    }
}