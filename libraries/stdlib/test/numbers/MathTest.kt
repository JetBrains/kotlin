/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.math.*
import kotlin.test.*

fun assertAlmostEquals(expected: Double, actual: Double, tolerance: Double? = null) {
    val tolerance_ = tolerance?.let { abs(it) } ?: 0.000000000001
    if (abs(expected - actual) > tolerance_) {
        assertEquals(expected, actual)
    }
}

fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Double? = null) {
    val tolerance_ = tolerance?.let { abs(it) } ?: 0.0000001
    if (abs(expected - actual) > tolerance_) {
        assertEquals(expected, actual)
    }
}

// For Kotlin JS tests
private val Float.ulpCommon: Float
    get() = when {
        isNaN() -> Float.NaN
        isInfinite() -> Float.POSITIVE_INFINITY
        this == Float.MAX_VALUE || this == -Float.MAX_VALUE -> 2.0f.pow(104)
        else -> {
            val d = absoluteValue
            // Ensure we never have -0.0f
            val valueOrPositiveZero = (this + 0.0f).toBits();
            Float.fromBits(valueOrPositiveZero + (if (valueOrPositiveZero >= 0) 1 else -1)) - d
        }
    }

class DoubleMathTest {

    @Test fun trigonometric() {
        assertEquals(0.0, sin(0.0))
        assertAlmostEquals(0.0, sin(PI))

        assertEquals(0.0, asin(0.0))
        assertAlmostEquals(PI / 2, asin(1.0))

        assertEquals(1.0, cos(0.0))
        assertAlmostEquals(-1.0, cos(PI))

        assertEquals(0.0, acos(1.0))
        assertAlmostEquals(PI / 2, acos(0.0))

        assertEquals(0.0, tan(0.0))
        assertAlmostEquals(1.0, tan(PI / 4))

        assertAlmostEquals(0.0, atan(0.0))
        assertAlmostEquals(PI / 4, atan(1.0))

        assertAlmostEquals(PI / 4, atan2(10.0, 10.0))
        assertAlmostEquals(-PI / 4, atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
        assertAlmostEquals(0.0, atan2(0.0, 0.0))
        assertAlmostEquals(0.0, atan2(0.0, 10.0))
        assertAlmostEquals(PI / 2, atan2(2.0, 0.0))

        for (angle in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertTrue(sin(angle).isNaN(), "sin($angle)")
            assertTrue(cos(angle).isNaN(), "cos($angle)")
            assertTrue(tan(angle).isNaN(), "tan($angle)")
        }

        for (value in listOf(Double.NaN, 1.2, -1.1)) {
            assertTrue(asin(value).isNaN())
            assertTrue(acos(value).isNaN())
        }
        assertTrue(atan(Double.NaN).isNaN())
        assertTrue(atan2(Double.NaN, 0.0).isNaN())
        assertTrue(atan2(0.0, Double.NaN).isNaN())
    }

    @Test fun hyperbolic() {
        assertEquals(Double.POSITIVE_INFINITY, sinh(Double.POSITIVE_INFINITY))
        assertEquals(Double.NEGATIVE_INFINITY, sinh(Double.NEGATIVE_INFINITY))
        assertTrue(sinh(Double.MIN_VALUE) != 0.0)
        assertTrue(sinh(710.0).isFinite())
        assertTrue(sinh(-710.0).isFinite())
        assertTrue(sinh(Double.NaN).isNaN())

        assertEquals(Double.POSITIVE_INFINITY, cosh(Double.POSITIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, cosh(Double.NEGATIVE_INFINITY))
        assertTrue(cosh(710.0).isFinite())
        assertTrue(cosh(-710.0).isFinite())
        assertTrue(cosh(Double.NaN).isNaN())

        assertAlmostEquals(1.0, tanh(Double.POSITIVE_INFINITY))
        assertAlmostEquals(-1.0, tanh(Double.NEGATIVE_INFINITY))
        assertTrue(tanh(Double.MIN_VALUE) != 0.0)
        assertTrue(tanh(Double.NaN).isNaN())
    }

    @Test fun inverseHyperbolicSin() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, Double.MIN_VALUE, -Double.MIN_VALUE, 0.00001)) {
            assertEquals(exact, asinh(sinh(exact)))
        }
        for (approx in listOf(Double.MIN_VALUE, 0.1, 1.0, 100.0, 710.0)) {
            assertAlmostEquals(approx, asinh(sinh(approx)))
            assertAlmostEquals(-approx, asinh(sinh(-approx)))
        }
        assertTrue(asinh(Double.NaN).isNaN())
    }

    @Test fun inverseHyperbolicCos() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0)) {
            assertEquals(abs(exact), acosh(cosh(exact)))
        }
        for (approx in listOf(Double.MIN_VALUE, 0.00001, 1.0, 100.0, 710.0)) {
            assertAlmostEquals(approx, acosh(cosh(approx)))
            assertAlmostEquals(approx, acosh(cosh(-approx)))
        }
        for (invalid in listOf(-1.0, 0.0, 0.99999, Double.NaN)) {
            assertTrue(acosh(invalid).isNaN())
        }
    }

    @Test fun inverseHyperbolicTan() {
        for (exact in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, Double.MIN_VALUE, -Double.MIN_VALUE)) {
            assertEquals(exact, atanh(tanh(exact)))
        }
        for (approx in listOf(0.00001)) {
            assertAlmostEquals(approx, atanh(tanh(approx)))
        }

        for (invalid in listOf(-1.00001, 1.00001, Double.NaN, Double.MAX_VALUE, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertTrue(atanh(invalid).isNaN())
        }
    }

    @Test fun cubeRoots() {
        val testingPairs = mapOf(
            Double.NaN to Double.NaN,
            Double.POSITIVE_INFINITY to Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY to Double.NEGATIVE_INFINITY,
            Double.fromBits(0x0010000000000000) to 2.812644285236262E-103, // smallest normal value
            Double.fromBits(0x1L) to 1.7031839360032603E-108, // smallest value (denormal)
            Double.MAX_VALUE to 5.643803094122362E102,
            0.0 to 0.0,
            0.9 to 0.9654893846056297,
            9.9 to 2.1472291690189413,
            27.0 to 3.0,
            399.1289 to 7.362710510208026,
            8123.452 to 20.102351976782558,
            21717.639 to 27.9,
            392890.22 to 73.24147345684439
        )

        for ((x, result) in testingPairs) {
            assertEquals(result, cbrt(x), if (result.isFinite()) 2.0 * result.ulp else 0.0)
            assertEquals(cbrt(-x), -cbrt(x))
        }
    }

    @Test fun powers() {
        assertEquals(5.0, hypot(3.0, 4.0))
        assertEquals(Double.POSITIVE_INFINITY, hypot(Double.NEGATIVE_INFINITY, Double.NaN))
        assertEquals(Double.POSITIVE_INFINITY, hypot(Double.NaN, Double.POSITIVE_INFINITY))
        assertTrue(hypot(Double.NaN, 0.0).isNaN())

        assertEquals(1.0, Double.NaN.pow(0.0))
        assertEquals(1.0, Double.POSITIVE_INFINITY.pow(0))
        assertEquals(49.0, 7.0.pow(2))
        assertEquals(0.25, 2.0.pow(-2))
        assertTrue(0.0.pow(Double.NaN).isNaN())
        assertTrue(Double.NaN.pow(-1).isNaN())
        assertTrue((-7.0).pow(1 / 3.0).isNaN())
        assertTrue(1.0.pow(Double.POSITIVE_INFINITY).isNaN())
        assertTrue((-1.0).pow(Double.NEGATIVE_INFINITY).isNaN())

        assertEquals(5.0, sqrt(9.0 + 16.0))
        assertTrue(sqrt(-1.0).isNaN())
        assertTrue(sqrt(Double.NaN).isNaN())

        assertTrue(exp(Double.NaN).isNaN())
        assertAlmostEquals(E, exp(1.0))
        assertEquals(1.0, exp(0.0))
        assertEquals(0.0, exp(Double.NEGATIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, exp(Double.POSITIVE_INFINITY))

        assertEquals(0.0, expm1(0.0))
        assertEquals(Double.MIN_VALUE, expm1(Double.MIN_VALUE))
        assertEquals(0.00010000500016667084, expm1(1e-4))
        assertEquals(-1.0, expm1(Double.NEGATIVE_INFINITY))
        assertEquals(Double.POSITIVE_INFINITY, expm1(Double.POSITIVE_INFINITY))
    }

    @Test fun logarithms() {
        assertTrue(log(1.0, Double.NaN).isNaN())
        assertTrue(log(Double.NaN, 1.0).isNaN())
        assertTrue(log(-1.0, 2.0).isNaN())
        assertTrue(log(2.0, -1.0).isNaN())
        assertTrue(log(2.0, 0.0).isNaN())
        assertTrue(log(2.0, 1.0).isNaN())
        assertTrue(log(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY).isNaN())
        assertEquals(-2.0, log(0.25, 2.0))
        assertEquals(-0.5, log(2.0, 0.25))
        assertEquals(Double.NEGATIVE_INFINITY, log(Double.POSITIVE_INFINITY, 0.25))
        assertEquals(Double.POSITIVE_INFINITY, log(Double.POSITIVE_INFINITY, 2.0))
        assertEquals(Double.NEGATIVE_INFINITY, log(0.0, 2.0))
        assertEquals(Double.POSITIVE_INFINITY, log(0.0, 0.25))

        assertTrue(ln(Double.NaN).isNaN())
        assertTrue(ln(-1.0).isNaN())
        assertEquals(1.0, ln(E))
        assertEquals(Double.NEGATIVE_INFINITY, ln(0.0))
        assertEquals(Double.POSITIVE_INFINITY, ln(Double.POSITIVE_INFINITY))

        assertEquals(1.0, log10(10.0))
        assertAlmostEquals(-1.0, log10(0.1))

        assertAlmostEquals(3.0, log2(8.0))
        assertEquals(-1.0, log2(0.5))

        assertTrue(ln1p(Double.NaN).isNaN())
        assertTrue(ln1p(-1.1).isNaN())
        assertEquals(0.0, ln1p(0.0))
        assertEquals(9.999995000003334e-7, ln1p(1e-6))
        assertEquals(Double.MIN_VALUE, ln1p(Double.MIN_VALUE))
        assertEquals(Double.NEGATIVE_INFINITY, ln1p(-1.0))
    }

    @Test fun rounding() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, 1.0, -10.0)) {
            assertEquals(value, ceil(value))
            assertEquals(value, floor(value))
            assertEquals(value, truncate(value))
            assertEquals(value, round(value))
        }
        val data = arrayOf( //   v floor trunc round  ceil
                doubleArrayOf( 1.3,  1.0,  1.0,  1.0,  2.0),
                doubleArrayOf(-1.3, -2.0, -1.0, -1.0, -1.0),
                doubleArrayOf( 1.5,  1.0,  1.0,  2.0,  2.0),
                doubleArrayOf(-1.5, -2.0, -1.0, -2.0, -1.0),
                doubleArrayOf( 1.8,  1.0,  1.0,  2.0,  2.0),
                doubleArrayOf(-1.8, -2.0, -1.0, -2.0, -1.0),

                doubleArrayOf( 2.3,  2.0,  2.0,  2.0,  3.0),
                doubleArrayOf(-2.3, -3.0, -2.0, -2.0, -2.0),
                doubleArrayOf( 2.5,  2.0,  2.0,  2.0,  3.0),
                doubleArrayOf(-2.5, -3.0, -2.0, -2.0, -2.0),
                doubleArrayOf( 2.8,  2.0,  2.0,  3.0,  3.0),
                doubleArrayOf(-2.8, -3.0, -2.0, -3.0, -2.0)
        )
        for ((v, f, t, r, c) in data) {
            assertEquals(f, floor(v), "floor($v)")
            assertEquals(t, truncate(v), "truncate($v)")
            assertEquals(r, round(v), "round($v)")
            assertEquals(c, ceil(v), "ceil($v)")
        }
    }

    @Test fun roundingConversion() {
        assertEquals(1L, 1.0.roundToLong())
        assertEquals(1L, 1.1.roundToLong())
        assertEquals(2L, 1.5.roundToLong())
        assertEquals(3L, 2.5.roundToLong())
        assertEquals(-2L, (-2.5).roundToLong())
        assertEquals(-3L, (-2.6).roundToLong())
        assertEquals(9223372036854774784, (9223372036854774800.0).roundToLong())
        assertEquals(Long.MAX_VALUE, Double.MAX_VALUE.roundToLong())
        assertEquals(Long.MIN_VALUE, (-Double.MAX_VALUE).roundToLong())
        assertEquals(Long.MAX_VALUE, Double.POSITIVE_INFINITY.roundToLong())
        assertEquals(Long.MIN_VALUE, Double.NEGATIVE_INFINITY.roundToLong())

        assertFails { Double.NaN.roundToLong() }

        assertEquals(1, 1.0.roundToInt())
        assertEquals(1, 1.1.roundToInt())
        assertEquals(2, 1.5.roundToInt())
        assertEquals(3, 2.5.roundToInt())
        assertEquals(-2, (-2.5).roundToInt())
        assertEquals(-3, (-2.6).roundToInt())
        assertEquals(2123456789, (2123456789.0).roundToInt())
        assertEquals(Int.MAX_VALUE, Double.MAX_VALUE.roundToInt())
        assertEquals(Int.MIN_VALUE, (-Double.MAX_VALUE).roundToInt())
        assertEquals(Int.MAX_VALUE, Double.POSITIVE_INFINITY.roundToInt())
        assertEquals(Int.MIN_VALUE, Double.NEGATIVE_INFINITY.roundToInt())

        assertFails { Double.NaN.roundToInt() }
    }

    @Test fun absoluteValue() {
        assertTrue(abs(Double.NaN).isNaN())
        assertTrue(Double.NaN.absoluteValue.isNaN())

        for (value in listOf(0.0, Double.MIN_VALUE, 0.1, 1.0, 1000.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY)) {
            assertEquals(value, value.absoluteValue)
            assertEquals(value, (-value).absoluteValue)
            assertEquals(value, abs(value))
            assertEquals(value, abs(-value))
        }
    }

    @Test fun signs() {
        assertTrue(sign(Double.NaN).isNaN())
        assertTrue(Double.NaN.sign.isNaN())

        val negatives = listOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -Double.MIN_VALUE)
        for (value in negatives) {
            assertEquals(-1.0, sign(value))
            assertEquals(-1.0, value.sign)
        }

        val zeroes = listOf(0.0, -0.0)
        for (value in zeroes) {
            assertEquals(value, sign(value))
            assertEquals(value, value.sign)
        }


        val positives = listOf(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1.0, Double.MIN_VALUE)
        for (value in positives) {
            assertEquals(1.0, sign(value))
            assertEquals(1.0, value.sign)
        }

        val allValues = negatives + positives
        for (a in allValues) {
            for (b in allValues) {
                val r = a.withSign(b)
                assertEquals(a.absoluteValue, r.absoluteValue)
                assertEquals(b.sign, r.sign, "expected $a with sign bit of $b to have sign ${b.sign}")
            }

            val rp0 = a.withSign(0.0)
            assertEquals(1.0, rp0.sign)
            assertEquals(a.absoluteValue, rp0.absoluteValue)

            val rm0 = a.withSign(-0.0)
            assertEquals(-1.0, rm0.sign)
            assertEquals(a.absoluteValue, rm0.absoluteValue)

            val ri = a.withSign(-1)
            assertEquals(-1.0, ri.sign)
            assertEquals(a.absoluteValue, ri.absoluteValue)

            val rn = a.withSign(Double.NaN)
            assertEquals(a.absoluteValue, rn.absoluteValue)
        }
    }


    @Test fun nextAndPrev() {
        for (value in listOf(0.0, -0.0, Double.MIN_VALUE, -1.0, 2.0.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            } else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0)
            if (toZero != 0.0) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Double.POSITIVE_INFINITY, Double.MAX_VALUE.nextUp())
            assertEquals(Double.MAX_VALUE, Double.POSITIVE_INFINITY.nextDown())

            assertEquals(Double.NEGATIVE_INFINITY, (-Double.MAX_VALUE).nextDown())
            assertEquals((-Double.MAX_VALUE), Double.NEGATIVE_INFINITY.nextUp())

            assertTrue(Double.NaN.ulp.isNaN())
            assertTrue(Double.NaN.nextDown().isNaN())
            assertTrue(Double.NaN.nextUp().isNaN())
            assertTrue(Double.NaN.nextTowards(0.0).isNaN())

            assertEquals(Double.MIN_VALUE, (0.0).ulp)
            assertEquals(Double.MIN_VALUE, (-0.0).ulp)
            assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY.ulp)
            assertEquals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY.ulp)

            val maxUlp = 2.0.pow(971)
            assertEquals(maxUlp, Double.MAX_VALUE.ulp)
            assertEquals(maxUlp, (-Double.MAX_VALUE).ulp)
        }
    }
}

class FloatMathTest {

    companion object {
        const val PI = kotlin.math.PI.toFloat()
        const val E = kotlin.math.E.toFloat()
    }

    // TODO: ensure it passes in kotlin-stdlib-js-ir after implementing KT-24975
    @Ignore
    @Test
    fun floatRangeConversion() {
        // expected run-time conversion to produce the same result as compile-time one
        assertEquals(kotlin.math.E.toFloat(), E)
        assertEquals(kotlin.math.PI.toFloat(), PI)
    }

    @Test fun trigonometric() {
        assertEquals(0.0F, sin(0.0F))
        assertAlmostEquals(0.0F, sin(PI))

        assertEquals(0.0F, asin(0.0F))
        assertAlmostEquals(PI / 2, asin(1.0F), 0.0000002)

        assertEquals(1.0F, cos(0.0F))
        assertAlmostEquals(-1.0F, cos(PI))

        assertEquals(0.0F, acos(1.0F))
        assertAlmostEquals(PI / 2, acos(0.0F))

        assertEquals(0.0F, tan(0.0F))
        assertAlmostEquals(1.0F, tan(PI / 4))

        assertAlmostEquals(0.0F, atan(0.0F))
        assertAlmostEquals(PI / 4, atan(1.0F))

        assertAlmostEquals(PI / 4, atan2(10.0F, 10.0F))
        assertAlmostEquals(-PI / 4, atan2(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY))
        assertAlmostEquals(0.0F, atan2(0.0F, 0.0F))
        assertAlmostEquals(0.0F, atan2(0.0F, 10.0F))
        assertAlmostEquals(PI / 2, atan2(2.0F, 0.0F))

        for (angle in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertTrue(sin(angle).isNaN(), "sin($angle)")
            assertTrue(cos(angle).isNaN(), "cos($angle)")
            assertTrue(tan(angle).isNaN(), "tan($angle)")
        }

        for (value in listOf(Float.NaN, 1.2F, -1.1F)) {
            assertTrue(asin(value).isNaN())
            assertTrue(acos(value).isNaN())
        }
        assertTrue(atan(Float.NaN).isNaN())
        assertTrue(atan2(Float.NaN, 0.0F).isNaN())
        assertTrue(atan2(0.0F, Float.NaN).isNaN())
    }

    @Test fun hyperbolic() {
        assertEquals(Float.POSITIVE_INFINITY, sinh(Float.POSITIVE_INFINITY))
        assertEquals(Float.NEGATIVE_INFINITY, sinh(Float.NEGATIVE_INFINITY))
        assertTrue(sinh(Float.MIN_VALUE) != 0.0F)
        assertTrue(sinh(89.0F).isFinite())
        assertTrue(sinh(-89.0F).isFinite())
        assertTrue(sinh(Float.NaN).isNaN())

        assertEquals(Float.POSITIVE_INFINITY, cosh(Float.POSITIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, cosh(Float.NEGATIVE_INFINITY))
        assertTrue(cosh(89.0F).isFinite())
        assertTrue(cosh(-89.0F).isFinite())
        assertTrue(cosh(Float.NaN).isNaN())

        assertAlmostEquals(1.0F, tanh(Float.POSITIVE_INFINITY))
        assertAlmostEquals(-1.0F, tanh(Float.NEGATIVE_INFINITY))
        assertTrue(tanh(Float.MIN_VALUE) != 0.0F)
        assertTrue(tanh(Float.NaN).isNaN())
    }

    @Test fun inverseHyperbolicSin() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, Float.MIN_VALUE, -Float.MIN_VALUE, 0.00001F)) {
            assertEquals(exact, asinh(sinh(exact)))
        }
        for (approx in listOf(Float.MIN_VALUE, 0.1F, 1.0F, 89.0F)) {
            assertAlmostEquals(approx, asinh(sinh(approx)))
            assertAlmostEquals(-approx, asinh(sinh(-approx)))
        }
        assertTrue(asinh(Float.NaN).isNaN())
    }

    @Test fun inverseHyperbolicCos() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F)) {
            assertEquals(abs(exact), acosh(cosh(exact)))
        }
        for (approx in listOf(Float.MIN_VALUE, 0.1F, 1.0F, 89.0F)) {
            assertAlmostEquals(approx, acosh(cosh(approx)))
            assertAlmostEquals(approx, acosh(cosh(-approx)))
        }
        for (invalid in listOf(-1.0F, 0.0F, 0.99999F, Float.NaN)) {
            assertTrue(acosh(invalid).isNaN())
        }
    }

    @Test fun inverseHyperbolicTan() {
        for (exact in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, Float.MIN_VALUE, -Float.MIN_VALUE)) {
            assertEquals(exact, atanh(tanh(exact)))
        }

        for (approx in listOf(0.00001F)) {
            assertAlmostEquals(approx, atanh(tanh(approx)))
        }

        for (invalid in listOf(-1.00001F, 1.00001F, Float.NaN, Float.MAX_VALUE, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)) {
            assertTrue(atanh(invalid).isNaN())
        }
    }

    @Test fun cubeRoots() {
        val testingPairs = mapOf(
            Float.NaN to Float.NaN,
            Float.POSITIVE_INFINITY to Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY to Float.NEGATIVE_INFINITY,
            Float.fromBits(0x00800000) to 2.2737368E-13f, // smallest normal value
            Float.fromBits(0x1) to 1.1190347E-15f, // smallest value (denormal)
            Float.MAX_VALUE to 6.9814636E12f,
            0.0f to 0.0f,
            0.9f to 0.9654894f,
            9.9f to 2.1472292f,
            27.0f to 3.0f,
            399.1289f to 7.3627105f,
            8123.452f to 20.102352f,
            21717.639f to 27.9f,
            392890.22f to 73.24147f
        )

        for ((x, result) in testingPairs) {
            assertEquals(result, cbrt(x), if (result.isFinite()) 2.0f * result.ulpCommon else 0.0f)
            assertEquals(cbrt(-x), -cbrt(x))
        }
    }

    @Test fun powers() {
        assertEquals(5.0F, hypot(3.0F, 4.0F))
        assertEquals(Float.POSITIVE_INFINITY, hypot(Float.NEGATIVE_INFINITY, Float.NaN))
        assertEquals(Float.POSITIVE_INFINITY, hypot(Float.NaN, Float.POSITIVE_INFINITY))
        assertTrue(hypot(Float.NaN, 0.0F).isNaN())

        assertEquals(1.0F, Float.NaN.pow(0.0F))
        assertEquals(1.0F, Float.POSITIVE_INFINITY.pow(0))
        assertEquals(49.0F, 7.0F.pow(2))
        assertEquals(0.25F, 2.0F.pow(-2))
        assertTrue(0.0F.pow(Float.NaN).isNaN())
        assertTrue(Float.NaN.pow(-1).isNaN())
        assertTrue((-7.0F).pow(1 / 3.0F).isNaN())
        assertTrue(1.0F.pow(Float.POSITIVE_INFINITY).isNaN())
        assertTrue((-1.0F).pow(Float.NEGATIVE_INFINITY).isNaN())

        assertEquals(5.0F, sqrt(9.0F + 16.0F))
        assertTrue(sqrt(-1.0F).isNaN())
        assertTrue(sqrt(Float.NaN).isNaN())

        assertTrue(exp(Float.NaN).isNaN())
        assertAlmostEquals(kotlin.math.E.toFloat(), exp(1.0F))
        assertEquals(1.0F, exp(0.0F))
        assertEquals(0.0F, exp(Float.NEGATIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, exp(Float.POSITIVE_INFINITY))

        assertEquals(0.0F, expm1(0.0F))
        assertEquals(-1.0F, expm1(Float.NEGATIVE_INFINITY))
        assertEquals(Float.POSITIVE_INFINITY, expm1(Float.POSITIVE_INFINITY))
    }

    @Test fun logarithms() {
        assertTrue(log(1.0F, Float.NaN).isNaN())
        assertTrue(log(Float.NaN, 1.0F).isNaN())
        assertTrue(log(-1.0F, 2.0F).isNaN())
        assertTrue(log(2.0F, -1.0F).isNaN())
        assertTrue(log(2.0F, 0.0F).isNaN())
        assertTrue(log(2.0F, 1.0F).isNaN())
        assertTrue(log(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY).isNaN())
        assertEquals(-2.0F, log(0.25F, 2.0F))
        assertEquals(-0.5F, log(2.0F, 0.25F))
        assertEquals(Float.NEGATIVE_INFINITY, log(Float.POSITIVE_INFINITY, 0.25F))
        assertEquals(Float.POSITIVE_INFINITY, log(Float.POSITIVE_INFINITY, 2.0F))
        assertEquals(Float.NEGATIVE_INFINITY, log(0.0F, 2.0F))
        assertEquals(Float.POSITIVE_INFINITY, log(0.0F, 0.25F))

        assertTrue(ln(Float.NaN).isNaN())
        assertTrue(ln(-1.0F).isNaN())
        assertAlmostEquals(1.0F, ln(E))
        assertEquals(Float.NEGATIVE_INFINITY, ln(0.0F))
        assertEquals(Float.POSITIVE_INFINITY, ln(Float.POSITIVE_INFINITY))

        assertEquals(1.0F, log10(10.0F))
        assertAlmostEquals(-1.0F, log10(0.1F))

        assertAlmostEquals(3.0F, log2(8.0F))
        assertEquals(-1.0F, log2(0.5F))

        assertTrue(ln1p(Float.NaN).isNaN())
        assertTrue(ln1p(-1.1F).isNaN())
        assertEquals(0.0F, ln1p(0.0F))
        assertEquals(Float.NEGATIVE_INFINITY, ln1p(-1.0F))
    }

    @Test fun rounding() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, 1.0F, -10.0F)) {
            assertEquals(value, ceil(value))
            assertEquals(value, floor(value))
            assertEquals(value, truncate(value))
            assertEquals(value, round(value))
        }
        val data = arrayOf( //   v floor trunc round  ceil
                floatArrayOf( 1.3F,  1.0F,  1.0F,  1.0F,  2.0F),
                floatArrayOf(-1.3F, -2.0F, -1.0F, -1.0F, -1.0F),
                floatArrayOf( 1.5F,  1.0F,  1.0F,  2.0F,  2.0F),
                floatArrayOf(-1.5F, -2.0F, -1.0F, -2.0F, -1.0F),
                floatArrayOf( 1.8F,  1.0F,  1.0F,  2.0F,  2.0F),
                floatArrayOf(-1.8F, -2.0F, -1.0F, -2.0F, -1.0F),

                floatArrayOf( 2.3F,  2.0F,  2.0F,  2.0F,  3.0F),
                floatArrayOf(-2.3F, -3.0F, -2.0F, -2.0F, -2.0F),
                floatArrayOf( 2.5F,  2.0F,  2.0F,  2.0F,  3.0F),
                floatArrayOf(-2.5F, -3.0F, -2.0F, -2.0F, -2.0F),
                floatArrayOf( 2.8F,  2.0F,  2.0F,  3.0F,  3.0F),
                floatArrayOf(-2.8F, -3.0F, -2.0F, -3.0F, -2.0F)
        )
        for ((v, f, t, r, c) in data) {
            assertEquals(f, floor(v), "floor($v)")
            assertEquals(t, truncate(v), "truncate($v)")
            assertEquals(r, round(v), "round($v)")
            assertEquals(c, ceil(v), "ceil($v)")
        }
    }

    @Test fun roundingConversion() {
        assertEquals(1L, 1.0F.roundToLong())
        assertEquals(1L, 1.1F.roundToLong())
        assertEquals(2L, 1.5F.roundToLong())
        assertEquals(3L, 2.5F.roundToLong())
        assertEquals(-2L, (-2.5F).roundToLong())
        assertEquals(-3L, (-2.6F).roundToLong())
        // assertEquals(9223372036854774784, (9223372036854774800.0F).roundToLong()) // platform-specific
        assertEquals(Long.MAX_VALUE, Float.MAX_VALUE.roundToLong())
        assertEquals(Long.MIN_VALUE, (-Float.MAX_VALUE).roundToLong())
        assertEquals(Long.MAX_VALUE, Float.POSITIVE_INFINITY.roundToLong())
        assertEquals(Long.MIN_VALUE, Float.NEGATIVE_INFINITY.roundToLong())

        assertFails { Float.NaN.roundToLong() }

        assertEquals(1, 1.0F.roundToInt())
        assertEquals(1, 1.1F.roundToInt())
        assertEquals(2, 1.5F.roundToInt())
        assertEquals(3, 2.5F.roundToInt())
        assertEquals(-2, (-2.5F).roundToInt())
        assertEquals(-3, (-2.6F).roundToInt())
        assertEquals(16777218, (16777218F).roundToInt())
        assertEquals(Int.MAX_VALUE, Float.MAX_VALUE.roundToInt())
        assertEquals(Int.MIN_VALUE, (-Float.MAX_VALUE).roundToInt())
        assertEquals(Int.MAX_VALUE, Float.POSITIVE_INFINITY.roundToInt())
        assertEquals(Int.MIN_VALUE, Float.NEGATIVE_INFINITY.roundToInt())

        assertFails { Float.NaN.roundToInt() }
    }

    @Test fun absoluteValue() {
        assertTrue(abs(Float.NaN).isNaN())
        assertTrue(Float.NaN.absoluteValue.isNaN())

        for (value in listOf(0.0F, Float.MIN_VALUE, 0.1F, 1.0F, 1000.0F, Float.MAX_VALUE, Float.POSITIVE_INFINITY)) {
            assertEquals(value, value.absoluteValue)
            assertEquals(value, (-value).absoluteValue)
            assertEquals(value, abs(value))
            assertEquals(value, abs(-value))
        }
    }

    @Test fun signs() {
        assertTrue(sign(Float.NaN).isNaN())
        assertTrue(Float.NaN.sign.isNaN())

        val negatives = listOf(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -1.0F, -Float.MIN_VALUE)
        for (value in negatives) {
            assertEquals(-1.0F, sign(value))
            assertEquals(-1.0F, value.sign)
        }

        val zeroes = listOf(0.0F, -0.0F)
        for (value in zeroes) {
            assertEquals(value, sign(value))
            assertEquals(value, value.sign)
        }


        val positives = listOf(Float.POSITIVE_INFINITY, Float.MAX_VALUE, 1.0F, Float.MIN_VALUE)
        for (value in positives) {
            assertEquals(1.0F, sign(value))
            assertEquals(1.0F, value.sign)
        }

        val allValues = negatives + positives
        for (a in allValues) {
            for (b in allValues) {
                val r = a.withSign(b)
                assertEquals(a.absoluteValue, r.absoluteValue)
                assertEquals(b.sign, r.sign)
            }

            val rp0 = a.withSign(0.0F)
            assertEquals(1.0F, rp0.sign)
            assertEquals(a.absoluteValue, rp0.absoluteValue)

            val rm0 = a.withSign(-0.0F)
            assertEquals(-1.0F, rm0.sign)
            assertEquals(a.absoluteValue, rm0.absoluteValue)

            val ri = a.withSign(-1)
            assertEquals(-1.0F, ri.sign)
            assertEquals(a.absoluteValue, ri.absoluteValue)
        }
    }

}

class IntegerMathTest {

    @Test fun intSigns() {
        val negatives = listOf(Int.MIN_VALUE, -65536, -1)
        val positives = listOf(1, 100, 256, Int.MAX_VALUE)
        negatives.forEach { assertEquals(-1, it.sign) }
        positives.forEach { assertEquals(1, it.sign) }
        assertEquals(0, 0.sign)

        (negatives - Int.MIN_VALUE).forEach { assertEquals(-it, it.absoluteValue) }
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.absoluteValue)

        positives.forEach { assertEquals(it, it.absoluteValue) }
    }


    @Test fun longSigns() {
        val negatives = listOf(Long.MIN_VALUE, -65536L, -1L)
        val positives = listOf(1L, 100L, 256L, Long.MAX_VALUE)
        negatives.forEach { assertEquals(-1, it.sign) }
        positives.forEach { assertEquals(1, it.sign) }
        assertEquals(0, 0L.sign)

        (negatives - Long.MIN_VALUE).forEach { assertEquals(-it, it.absoluteValue) }
        assertEquals(Long.MIN_VALUE, Long.MIN_VALUE.absoluteValue)

        positives.forEach { assertEquals(it, it.absoluteValue) }
    }
}