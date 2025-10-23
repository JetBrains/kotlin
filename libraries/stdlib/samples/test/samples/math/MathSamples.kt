/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.math

import samples.*
import kotlin.math.*
import kotlin.test.*

@RunWith(Enclosed::class)
class MathSamples {
    class Doubles {
        @Sample
        fun floor() {
            assertPrints(floor(3.14159), "3.0")
            assertPrints(floor(-1.1), "-2.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(floor(10.0), "10.0")
            // Special cases
            assertPrints(floor(Double.NaN), "NaN")
            assertPrints(floor(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(floor(Double.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun ceil() {
            assertPrints(ceil(3.14159), "4.0")
            assertPrints(ceil(-1.1), "-1.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(ceil(10.0), "10.0")
            // Special cases
            assertPrints(ceil(Double.NaN), "NaN")
            assertPrints(ceil(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(ceil(Double.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun truncate() {
            assertPrints(truncate(3.14159), "3.0")
            assertPrints(truncate(-1.1), "-1.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(truncate(10.0), "10.0")
            // Special cases
            assertPrints(truncate(Double.NaN), "NaN")
            assertPrints(truncate(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(truncate(Double.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun round() {
            assertPrints(round(3.49), "3.0")
            assertPrints(round(3.51), "4.0")
            // 3.5 is between 3.0 and 4.0, ties are rounded away from zero to 4.0
            assertPrints(round(3.5), "4.0")
            // 2.5 is between 2.0 and 3.0, ties are rounded away from zero to 3.0
            assertPrints(round(2.5), "3.0")
            // -10.0 is already an "integer", so no rounding will take place
            assertPrints(truncate(-10.0), "-10.0")
            // Special cases
            assertPrints(round(Double.NaN), "NaN")
            assertPrints(round(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(round(Double.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun roundingModes() {
            assertPrints(floor(3.5), "3.0")
            assertPrints(ceil(3.5), "4.0")
            assertPrints(truncate(3.5), "3.0")
            assertPrints(round(3.5), "4.0")
            assertPrints(round(3.49), "3.0")

            assertPrints(floor(-3.5), "-4.0")
            assertPrints(ceil(-3.5), "-3.0")
            assertPrints(truncate(-3.5), "-3.0")
            assertPrints(round(-3.5), "-4.0")
            assertPrints(round(-3.49), "-3.0")
        }

        @Sample
        fun roundToInt() {
            assertPrints(3.14159.roundToInt(), "3")
            assertPrints((-10.0).roundToInt(), "-10")
            // Values greater than Int.MAX_VALUE are rounded to Int.MAX_VALUE
            assertTrue((Int.MAX_VALUE.toDouble() + 1.0).roundToInt() == Int.MAX_VALUE)
            assertTrue(Double.POSITIVE_INFINITY.roundToInt() == Int.MAX_VALUE)
            // Values smaller than Int.MIN_VALUE are rounded to Int.MIN_VALUE
            assertTrue((Int.MIN_VALUE.toDouble() - 1.0).roundToInt() == Int.MIN_VALUE)
            assertTrue(Double.NEGATIVE_INFINITY.roundToInt() == Int.MIN_VALUE)
            // NaN could not be rounded to Int
            assertFailsWith<IllegalArgumentException> { Double.NaN.roundToInt() }
        }

        @Sample
        fun roundToLong() {
            assertPrints(3.14159.roundToLong(), "3")
            assertPrints((-10.0).roundToLong(), "-10")
            // Values greater than Long.MAX_VALUE are rounded to Long.MAX_VALUE
            assertTrue((Long.MAX_VALUE.toDouble() * 2.0).roundToLong() == Long.MAX_VALUE)
            assertTrue(Double.POSITIVE_INFINITY.roundToLong() == Long.MAX_VALUE)
            // Values smaller than Long.MIN_VALUE are rounded to Long.MIN_VALUE
            assertTrue((Long.MIN_VALUE.toDouble() * 2.0).roundToLong() == Long.MIN_VALUE)
            assertTrue(Double.NEGATIVE_INFINITY.roundToLong() == Long.MIN_VALUE)
            // NaN could not be rounded to Long
            assertFailsWith<IllegalArgumentException> { Double.NaN.roundToLong() }
        }

        @Sample
        fun sign() {
            assertPrints(3.14.sign, "1.0")
            assertPrints((-3.14).sign, "-1.0")
            assertPrints(0.0.sign, "0.0")
            assertPrints(Double.POSITIVE_INFINITY.sign, "1.0")
            assertPrints(Double.NEGATIVE_INFINITY.sign, "-1.0")

            // Special cases
            assertPrints(Double.NaN.sign, "NaN")
            assertPrints((-0.0).sign, "-0.0")
        }

        @Sample
        fun signFun() {
            assertPrints(sign(3.14), "1.0")
            assertPrints(sign(-3.14), "-1.0")
            assertPrints(sign(0.0), "0.0")
            assertPrints(sign(Double.POSITIVE_INFINITY), "1.0")
            assertPrints(sign(Double.NEGATIVE_INFINITY), "-1.0")

            // Special cases
            assertPrints(sign(Double.NaN), "NaN")
            assertPrints(sign(-0.0), "-0.0")
        }

        @Test
        fun naturalLogarithm() {
            assertPrints(ln(E), "1.0")
            assertPrints(ln(1.0), "0.0")
            assertPrints(ln(E * E), "2.0")

            // Special cases
            assertPrints(ln(Double.NaN), "NaN")
            assertPrints(ln(-1.0), "NaN")
            assertPrints(ln(0.0), "-Infinity")
            assertPrints(ln(Double.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logBase2() {
            assertPrints(log2(2.0), "1.0")
            assertPrints(log2(1.0), "0.0")
            assertPrints(log2(8.0), "3.0")

            // Special cases
            assertPrints(log2(Double.NaN), "NaN")
            assertPrints(log2(-1.0), "NaN")
            assertPrints(log2(0.0), "-Infinity")
            assertPrints(log2(Double.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logBase10() {
            assertPrints(log10(10.0), "1.0")
            assertPrints(log10(1.0), "0.0")
            assertPrints(log10(1000.0), "3.0")

            // Special cases
            assertPrints(log10(Double.NaN), "NaN")
            assertPrints(log10(-1.0), "NaN")
            assertPrints(log10(0.0), "-Infinity")
            assertPrints(log10(Double.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logarithm() {
            assertPrints(log(64.0, 4.0), "3.0")
            assertPrints(log(100.0, 10.0), "2.0")
            // √4 = 2 -> log₄2 = ½
            assertPrints(log(2.0, 4.0), "0.5")

            // Special cases
            assertPrints(log(Double.NaN, 2.0), "NaN")
            assertPrints(log(-1.0, 2.0), "NaN")
            assertPrints(log(1.0, 1.0), "NaN")
            assertPrints(log(Double.POSITIVE_INFINITY, 0.5), "-Infinity")
            assertPrints(log(0.0, 0.5), "Infinity")
        }

        @Test
        fun naturalLogarithmPlusOne() {
            // ln1p has better precision for arguments close to zero
            assertTrue(ln1p(1e-20) > 0.0)
            println(ln1p(1e-20))
            // compared to adding 1.0 to a value and passing it to ln
            assertPrints(ln(1e-20 + 1.0), "0.0")

            // Special cases
            assertPrints(ln1p(Double.NaN), "NaN")
            assertPrints(ln1p(-2.0), "NaN")
            assertPrints(ln1p(-1.0), "-Infinity")
            assertPrints(ln1p(Double.POSITIVE_INFINITY), "Infinity")
        }
    }

    class Floats {
        @Sample
        fun floor() {
            assertPrints(floor(3.14159f), "3.0")
            assertPrints(floor(-1.1f), "-2.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(floor(10.0f), "10.0")
            // Special cases
            assertPrints(floor(Float.NaN), "NaN")
            assertPrints(floor(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(floor(Float.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun ceil() {
            assertPrints(ceil(3.14159f), "4.0")
            assertPrints(ceil(-1.1f), "-1.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(ceil(10.0f), "10.0")
            // Special cases
            assertPrints(ceil(Float.NaN), "NaN")
            assertPrints(ceil(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(ceil(Float.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun truncate() {
            assertPrints(truncate(3.14159f), "3.0")
            assertPrints(truncate(-1.1f), "-1.0")
            // 10.0 is already an "integer", so no rounding will take place
            assertPrints(truncate(10.0f), "10.0")
            // Special cases
            assertPrints(truncate(Float.NaN), "NaN")
            assertPrints(truncate(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(truncate(Float.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun round() {
            assertPrints(round(3.49f), "3.0")
            assertPrints(round(3.51f), "4.0")
            // 3.5 is between 3.0 and 4.0, ties are rounded away from zero to 4.0
            assertPrints(round(3.5f), "4.0")
            // 2.5 is between 2.0 and 3.0, ties are rounded away from zero to 3.0
            assertPrints(round(2.5f), "3.0")
            // -10.0 is already an "integer", so no rounding will take place
            assertPrints(truncate(-10.0f), "-10.0")
            // Special cases
            assertPrints(round(Float.NaN), "NaN")
            assertPrints(round(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(round(Float.NEGATIVE_INFINITY), "-Infinity")
        }

        @Sample
        fun roundingModes() {
            assertPrints(floor(3.5f), "3.0")
            assertPrints(ceil(3.5f), "4.0")
            assertPrints(truncate(3.5f), "3.0")
            assertPrints(round(3.5f), "4.0")
            assertPrints(round(3.49f), "3.0")

            assertPrints(floor(-3.5f), "-4.0")
            assertPrints(ceil(-3.5f), "-3.0")
            assertPrints(truncate(-3.5f), "-3.0")
            assertPrints(round(-3.5f), "-4.0")
            assertPrints(round(-3.49f), "-3.0")
        }

        @Sample
        fun roundToInt() {
            assertPrints(3.14159f.roundToInt(), "3")
            assertPrints((-10.0f).roundToInt(), "-10")
            // Values greater than Int.MAX_VALUE are rounded to Int.MAX_VALUE
            assertTrue((Int.MAX_VALUE.toFloat() * 2.0f).roundToInt() == Int.MAX_VALUE)
            assertTrue(Float.POSITIVE_INFINITY.roundToInt() == Int.MAX_VALUE)
            // Values smaller than Int.MIN_VALUE are rounded to Int.MIN_VALUE
            assertTrue((Int.MIN_VALUE.toFloat() * 2.0f).roundToInt() == Int.MIN_VALUE)
            assertTrue(Float.NEGATIVE_INFINITY.roundToInt() == Int.MIN_VALUE)
            // NaN could not be rounded to Int
            assertFailsWith<IllegalArgumentException> { Float.NaN.roundToInt() }
        }

        @Sample
        fun roundToLong() {
            assertPrints(3.14159f.roundToLong(), "3")
            assertPrints((-10.0f).roundToLong(), "-10")
            // Values greater than Long.MAX_VALUE are rounded to Long.MAX_VALUE
            assertTrue((Long.MAX_VALUE.toFloat() * 2.0f).roundToLong() == Long.MAX_VALUE)
            assertTrue(Float.POSITIVE_INFINITY.roundToLong() == Long.MAX_VALUE)
            // Values smaller than Long.MIN_VALUE are rounded to Long.MIN_VALUE
            assertTrue((Long.MIN_VALUE.toFloat() * 2.0f).roundToLong() == Long.MIN_VALUE)
            assertTrue(Float.NEGATIVE_INFINITY.roundToLong() == Long.MIN_VALUE)
            // NaN could not be rounded to Long
            assertFailsWith<IllegalArgumentException> { Float.NaN.roundToLong() }
        }

        @Sample
        fun sign() {
            assertPrints(3.14f.sign, "1.0")
            assertPrints((-3.14f).sign, "-1.0")
            assertPrints(0.0f.sign, "0.0")
            assertPrints(Float.POSITIVE_INFINITY.sign, "1.0")
            assertPrints(Float.NEGATIVE_INFINITY.sign, "-1.0")

            // Special cases
            assertPrints(Float.NaN.sign, "NaN")
            assertPrints((-0.0f).sign, "-0.0")
        }

        @Sample
        fun signFun() {
            assertPrints(sign(3.14f), "1.0")
            assertPrints(sign(-3.14f), "-1.0")
            assertPrints(sign(0.0f), "0.0")
            assertPrints(sign(Float.POSITIVE_INFINITY), "1.0")
            assertPrints(sign(Float.NEGATIVE_INFINITY), "-1.0")

            // Special cases
            assertPrints(sign(Float.NaN), "NaN")
            assertPrints(sign(-0.0f), "-0.0")
        }

        @Test
        fun naturalLogarithm() {
            assertTrue(abs(ln(E.toFloat()) - 1.0f) < 1e7f)
            assertPrints(ln(1.0f), "0.0")
            assertPrints(ln(E.toFloat() * E.toFloat()), "2.0")

            // Special cases
            assertPrints(ln(Float.NaN), "NaN")
            assertPrints(ln(-1.0f), "NaN")
            assertPrints(ln(0.0f), "-Infinity")
            assertPrints(ln(Float.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logBase2() {
            assertPrints(log2(2.0f), "1.0")
            assertPrints(log2(1.0f), "0.0")
            assertPrints(log2(8.0f), "3.0")

            // Special cases
            assertPrints(log2(Float.NaN), "NaN")
            assertPrints(log2(-1.0f), "NaN")
            assertPrints(log2(0.0f), "-Infinity")
            assertPrints(log2(Float.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logBase10() {
            assertPrints(log10(10.0f), "1.0")
            assertPrints(log10(1.0f), "0.0")
            assertPrints(log10(1000.0f), "3.0")

            // Special cases
            assertPrints(log10(Float.NaN), "NaN")
            assertPrints(log10(-1.0f), "NaN")
            assertPrints(log10(0.0f), "-Infinity")
            assertPrints(log10(Float.POSITIVE_INFINITY), "Infinity")
        }

        @Test
        fun logarithm() {
            assertPrints(log(9.0f, 3.0f), "2.0")
            assertPrints(log(1000.0f, 10.0f), "3.0")
            // √4 = 2 -> log₄2 = ½
            assertPrints(log(2.0f, 4.0f), "0.5")

            // Special cases
            assertPrints(log(Float.NaN, 2.0f), "NaN")
            assertPrints(log(-1.0f, 2.0f), "NaN")
            assertPrints(log(1.0f, 1.0f), "NaN")
            assertPrints(log(Float.POSITIVE_INFINITY, 0.5f), "-Infinity")
            assertPrints(log(0.0f, 0.5f), "Infinity")
        }

        @Test
        fun naturalLogarithmPlusOne() {
            // ln1p has better precision for arguments close to zero
            assertTrue(ln1p(1e-20f) > 0.0f)
            println(ln1p(1e-20f))
            // compared to adding 1.0 to a value and passing it to ln
            assertPrints(ln(1e-20f + 1.0f), "0.0")

            // Special cases
            assertPrints(ln1p(Float.NaN), "NaN")
            assertPrints(ln1p(-2.0f), "NaN")
            assertPrints(ln1p(-1.0f), "-Infinity")
            assertPrints(ln1p(Float.POSITIVE_INFINITY), "Infinity")
        }
    }
}
