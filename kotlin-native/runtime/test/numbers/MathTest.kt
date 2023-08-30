/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.numbers

import kotlin.math.*
import kotlin.test.*

//  Native-specific part for libraries/stdlib/test/numbers/MathTest.kt

class DoubleMathNativeTest {
    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                doubleArrayOf(-2.0,   0.5),
                doubleArrayOf(-1.25, -1.25),
                doubleArrayOf( 0.0,   0.0),
                doubleArrayOf( 1.0,   1.0),
                doubleArrayOf( 1.25,  1.25),
                doubleArrayOf( 1.5,  -1.0),
                doubleArrayOf( 2.0,  -0.5),
                doubleArrayOf( 2.5,   0.0),
                doubleArrayOf( 3.5,   1.0),
                doubleArrayOf( 3.75, -1.25),
                doubleArrayOf( 4.0,  -1.0)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5), "($a).IEEErem(2.5)")
        }

        assertTrue(Double.NaN.IEEErem(2.5).isNaN())
        assertTrue(2.0.IEEErem(Double.NaN).isNaN())
        assertTrue(Double.POSITIVE_INFINITY.IEEErem(2.0).isNaN())
        assertTrue(2.0.IEEErem(0.0).isNaN())
        assertEquals(PI, PI.IEEErem(Double.NEGATIVE_INFINITY))
    }

    /*
     * Special cases:
     *   - `atan2(0.0, 0.0)` is `0.0`
     *   - `atan2(0.0, x)` is  `0.0` for `x > 0` and `PI` for `x < 0`
     *   - `atan2(-0.0, x)` is `-0.0` for 'x > 0` and `-PI` for `x < 0`
     *   - `atan2(y, +Inf)` is `0.0` for `0 < y < +Inf` and `-0.0` for '-Inf < y < 0`
     *   - `atan2(y, -Inf)` is `PI` for `0 < y < +Inf` and `-PI` for `-Inf < y < 0`
     *   - `atan2(y, 0.0)` is `PI/2` for `y > 0` and `-PI/2` for `y < 0`
     *   - `atan2(+Inf, x)` is `PI/2` for finite `x`y
     *   - `atan2(-Inf, x)` is `-PI/2` for finite `x`
     *   - `atan2(NaN, x)` and `atan2(y, NaN)` is `NaN`
     */
    @Test fun atan2SpecialCases() {

        assertEquals(atan2(0.0, 0.0), 0.0)
        assertEquals(atan2(0.0, 1.0), 0.0)
        assertEquals(atan2(0.0, -1.0), PI)
        assertEquals(atan2(-0.0, 1.0), -0.0)
        assertEquals(atan2(-0.0, -1.0), -PI)
        assertEquals(atan2(1.0, Double.POSITIVE_INFINITY), 0.0)
        assertEquals(atan2(-1.0, Double.POSITIVE_INFINITY), -0.0)
        assertEquals(atan2(1.0, Double.NEGATIVE_INFINITY), PI)
        assertEquals(atan2(-1.0, Double.NEGATIVE_INFINITY), -PI)
        assertEquals(atan2(1.0, 0.0), PI/2)
        assertEquals(atan2(-1.0, 0.0), -PI/2)
        assertEquals(atan2(Double.POSITIVE_INFINITY, 1.0), PI/2)
        assertEquals(atan2(Double.NEGATIVE_INFINITY, 1.0), -PI/2)

        assertTrue(atan2(Double.NaN, 1.0).isNaN())
        assertTrue(atan2(1.0, Double.NaN).isNaN())
    }
}

class FloatMathNativeTest {
    @Test fun nextAndPrev() {
        for (value in listOf(0.0f, -0.0f, Float.MIN_VALUE, -1.0f, 2.0f.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            }
            else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0f)
            if (toZero != 0.0f) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Float.POSITIVE_INFINITY, Float.MAX_VALUE.nextUp())
            assertEquals(Float.MAX_VALUE, Float.POSITIVE_INFINITY.nextDown())

            assertEquals(Float.NEGATIVE_INFINITY, (-Float.MAX_VALUE).nextDown())
            assertEquals((-Float.MAX_VALUE), Float.NEGATIVE_INFINITY.nextUp())

            assertTrue(Float.NaN.ulp.isNaN())
            assertTrue(Float.NaN.nextDown().isNaN())
            assertTrue(Float.NaN.nextUp().isNaN())
            assertTrue(Float.NaN.nextTowards(0.0f).isNaN())

            assertEquals(Float.MIN_VALUE, (0.0f).ulp)
            assertEquals(Float.MIN_VALUE, (-0.0f).ulp)
            assertEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY.ulp)
            assertEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY.ulp)

            val maxUlp = 2.0f.pow(104)
            assertEquals(maxUlp, Float.MAX_VALUE.ulp)
            assertEquals(maxUlp, (-Float.MAX_VALUE).ulp)
        }
    }

    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                floatArrayOf(-2.0f,   0.5f),
                floatArrayOf(-1.25f, -1.25f),
                floatArrayOf( 0.0f,   0.0f),
                floatArrayOf( 1.0f,   1.0f),
                floatArrayOf( 1.25f,  1.25f),
                floatArrayOf( 1.5f,  -1.0f),
                floatArrayOf( 2.0f,  -0.5f),
                floatArrayOf( 2.5f,   0.0f),
                floatArrayOf( 3.5f,   1.0f),
                floatArrayOf( 3.75f, -1.25f),
                floatArrayOf( 4.0f,  -1.0f)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5f), "($a).IEEErem(2.5f)")
        }

        assertTrue(Float.NaN.IEEErem(2.5f).isNaN())
        assertTrue(2.0f.IEEErem(Float.NaN).isNaN())
        assertTrue(Float.POSITIVE_INFINITY.IEEErem(2.0f).isNaN())
        assertTrue(2.0f.IEEErem(0.0f).isNaN())
        assertEquals(PI.toFloat(), PI.toFloat().IEEErem(Float.NEGATIVE_INFINITY))
    }
}