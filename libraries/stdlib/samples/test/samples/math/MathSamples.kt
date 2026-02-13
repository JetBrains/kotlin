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
            // 3.5 is between 3.0 and 4.0, so it is rounded towards an even number 4.0
            assertPrints(round(3.5), "4.0")
            // 2.5 is between 2.0 and 3.0, so it is rounded towards an even number 2.0
            assertPrints(round(2.5), "2.0")
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
        fun abs() {
            assertPrints(abs(3.14), "3.14")
            assertPrints(abs(-3.14), "3.14")
            assertPrints(abs(-0.0), "0.0")
            assertPrints(abs(Double.NEGATIVE_INFINITY), "Infinity")
            assertPrints(abs(Double.NaN), "NaN")
        }

        @Sample
        fun absoluteValue() {
            assertPrints(3.14.absoluteValue, "3.14")
            assertPrints((-3.14).absoluteValue, "3.14")
            assertPrints((-0.0).absoluteValue, "0.0")
            assertPrints(Double.NEGATIVE_INFINITY.absoluteValue, "Infinity")
            assertPrints(Double.NaN.absoluteValue, "NaN")
        }

        @Sample
        fun withSignDouble() {
            assertPrints(3.14.withSign(3.14), "3.14")
            assertPrints(3.14.withSign(-3.14), "-3.14")
            assertPrints((-3.14).withSign(-3.14), "-3.14")
            assertPrints((-3.14).withSign(0.0), "3.14")
            assertPrints(Double.NaN.withSign(-1.0), "NaN")
        }

        @Sample
        fun withSignInt() {
            assertPrints(3.14.withSign(100), "3.14")
            assertPrints(3.14.withSign(-100), "-3.14")
            assertPrints((-3.14).withSign(-100), "-3.14")
            assertPrints((-3.14).withSign(0), "3.14")
            assertPrints(Double.NaN.withSign(-1), "NaN")
        }

        @Sample
        fun cbrt() {
            assertPrints(cbrt(8.0), "2.0")
            assertPrints(cbrt(-8.0), "-2.0")

            // special cases
            assertPrints(cbrt(Double.NaN), "NaN")
            assertPrints(cbrt(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(cbrt(Double.NEGATIVE_INFINITY), "-Infinity")
            assertPrints(cbrt(-0.0), "-0.0")
        }

        @Sample
        fun sqrt() {
            assertPrints(sqrt(9.0), "3.0")
            assertPrints(sqrt(5.76), "2.4")
            // special cases
            assertPrints(sqrt(-0.0), "-0.0")
            assertPrints(sqrt(-4.0), "NaN")
            assertPrints(sqrt(Double.NaN), "NaN")
            assertPrints(sqrt(Double.POSITIVE_INFINITY), "Infinity")
        }

        @Sample
        fun hypot() {
            // sqrt(6 * 6 + 8 * 8) = sqrt(36 + 64) = sqrt(100) = 10
            assertPrints(hypot(6.0, 8.0), "10.0")

            // special cases
            assertPrints(hypot(Double.NaN, 2.0), "NaN")
            assertPrints(hypot(1.0, Double.NaN), "NaN")
            assertPrints(hypot(Double.POSITIVE_INFINITY, 2.0), "Infinity")
            assertPrints(hypot(1.0, Double.NEGATIVE_INFINITY), "Infinity")
        }

        @Sample
        fun powDouble() {
            assertPrints(2.0.pow(2.0), "4.0")
            assertPrints(3.0.pow(1.0), "3.0")
            assertPrints(4.0.pow(0.0), "1.0")
            assertPrints(2.0.pow(-2.0), "0.25")
            assertPrints(4.0.pow(0.5), "2.0")
            assertPrints((-2.0).pow(2.0), "4.0")

            // special cases
            assertPrints(Double.NaN.pow(42.0), "NaN")
            assertPrints(Double.NaN.pow(0.0), "1.0")
            assertPrints(3.0.pow(Double.NaN), "NaN")
            assertPrints((-2.0).pow(0.75), "NaN")
            assertPrints(1.0.pow(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(1.0.pow(Double.NEGATIVE_INFINITY), "NaN")
            assertPrints(2.0.pow(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(2.0.pow(Double.NEGATIVE_INFINITY), "0.0")
        }

        @Sample
        fun powInt() {
            assertPrints(2.0.pow(2), "4.0")
            assertPrints(3.0.pow(1), "3.0")
            assertPrints(4.0.pow(0), "1.0")
            assertPrints(2.0.pow(-2), "0.25")
            assertPrints((-2.0).pow(2), "4.0")

            // special cases
            assertPrints(Double.NaN.pow(42), "NaN")
            assertPrints(Double.NaN.pow(0), "1.0")
        }

        @Sample
        fun exp() {
            fun Double.firstFiveDigits(): String = toString().substring(0, 7)

            assertPrints(exp(1.0).firstFiveDigits(), "2.71828")
            assertPrints(exp(2.0).firstFiveDigits(), "7.38905")
            assertPrints(exp(-1.0).firstFiveDigits(), "0.36787")

            // special cases
            assertPrints(exp(Double.NaN), "NaN")
            assertPrints(exp(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(exp(Double.NEGATIVE_INFINITY), "0.0")
        }

        @Sample
        fun expm1() {
            fun Double.firstFiveDigits(): String = toString().substring(0, 7)

            assertPrints(expm1(1.0).firstFiveDigits(), "1.71828")
            // While it does not really matter for relatively large x values (like 1.0),
            // the difference is getting more pronounced when x is getting closer to 0.0
            assertPrints(exp(1e-17) - 1.0, "0.0")
            assertPrints(expm1(1e-17), 1e-17.toString())

            // special cases
            assertPrints(expm1(Double.NaN), "NaN")
            assertPrints(expm1(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(expm1(Double.NEGATIVE_INFINITY), "-1.0")
        }

        @Sample
        fun sin() {
            val epsilon = 1e-10

            assertPrints(sin(0.0), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // sin(π/2) = 1.0
            assertTrue((sin(PI / 2) - 1.0).absoluteValue < epsilon)
            // sin(-π/2) = -1.0
            assertTrue((sin(-PI / 2) - -1.0).absoluteValue < epsilon)

            // special cases
            assertPrints(sin(Double.NaN), "NaN")
            assertPrints(sin(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(sin(Double.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun cos() {
            val epsilon = 1e-10

            assertPrints(cos(0.0), "1.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // cos(π/2) = 0
            assertTrue(cos(PI / 2).absoluteValue < epsilon)
            assertPrints(cos(PI), "-1.0")

            // special cases
            assertPrints(cos(Double.NaN), "NaN")
            assertPrints(cos(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(cos(Double.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun tan() {
            val epsilon = 1e-10

            assertPrints(tan(0.0), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // tan(π/4) = 1.0
            assertTrue((tan(PI / 4) - 1.0).absoluteValue < epsilon)
            // tan(-π/4) = -1.0
            assertTrue((tan(-PI / 4) - -1.0).absoluteValue < epsilon)

            // special cases
            assertPrints(tan(Double.NaN), "NaN")
            assertPrints(tan(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(tan(Double.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun asin() {
            val epsilon = 1e-10

            assertPrints(asin(0.0), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // asin(1.0) = π/2
            assertTrue((asin(1.0) - PI / 2).absoluteValue < epsilon)
            // asin(-1.0) = -π/2
            assertTrue((asin(-1.0) - (-PI / 2)).absoluteValue < epsilon)
            // asin(sin(x)) = x
            assertTrue((asin(sin(0.123)) - 0.123).absoluteValue < epsilon)

            // special cases
            assertPrints(asin(Double.NaN), "NaN")
            assertPrints(asin(1.1), "NaN")
            assertPrints(asin(-2.0), "NaN")
        }

        @Sample
        fun acos() {
            val epsilon = 1e-10

            assertPrints(acos(1.0), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // acos(0.0) = π/2
            assertTrue((acos(0.0) - PI / 2).absoluteValue < epsilon)
            // acos(-1.0) = π
            assertTrue((acos(-1.0) - PI).absoluteValue < epsilon)
            // acos(cos(x)) = x
            assertTrue((acos(cos(0.123)) - 0.123).absoluteValue < epsilon)

            // special cases
            assertPrints(acos(Double.NaN), "NaN")
            assertPrints(acos(1.1), "NaN")
            assertPrints(acos(-2.0), "NaN")
        }

        @Sample
        fun atan() {
            val epsilon = 1e-10

            assertPrints(atan(0.0), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // atan(1.0) = π/4
            assertTrue((atan(1.0) - PI / 4).absoluteValue < epsilon)
            // atan(-1.0) = -π/4
            assertTrue((atan(-1.0) - (-PI / 4)).absoluteValue < epsilon)
            // atan(tan(x)) = x
            assertTrue((atan(tan(0.123)) - 0.123).absoluteValue < epsilon)

            // special cases
            assertPrints(atan(Double.NaN), "NaN")
        }

        @Sample
        fun atan2() {
            val epsilon = 1e-10
            fun Double.toDegrees(): Double = this * 180.0 / PI

            assertPrints(atan2(y = 0.0, x = 0.0), "0.0")

            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            assertTrue((atan2(y = 1.0, x = 0.0) - PI / 2).absoluteValue < epsilon)
            assertPrints(atan2(y = 1.0, x = 0.0).toDegrees(), "90.0")

            assertTrue((atan2(y = 0.0, x = -1.0) - PI).absoluteValue < epsilon)
            assertPrints(atan2(y = 0.0, x = -1.0).toDegrees(), "180.0")

            assertTrue((atan2(y = -1.0, x = 0.0) - (-PI / 2)).absoluteValue < epsilon)
            assertPrints(atan2(y = -1.0, x = 0.0).toDegrees(), "-90.0")

            // special cases (some of them)
            assertPrints(atan2(y = Double.NaN, x = 0.5), "NaN")
            assertPrints(atan2(y = 0.5, x = Double.NaN), "NaN")
            assertPrints(atan2(y = -0.0, x = 100500.0), "-0.0")
            assertPrints(atan2(y = 0.0, x = Double.POSITIVE_INFINITY), "0.0")
            assertPrints(atan2(y = Double.POSITIVE_INFINITY, x = 100500.0).toDegrees(), "90.0")
        }

        @Sample
        fun min() {
            assertPrints(min(-1.0, 1.0), "-1.0")
            assertPrints(min(1.0, -1.0), "-1.0")
            assertPrints(min(-3.0, -1.0), "-3.0")
            assertPrints(min(-0.0, 0.0), "-0.0")

            // special cases
            assertPrints(min(Double.NaN, 0.0), "NaN")
            assertPrints(min(-9000.0, Double.NaN), "NaN")
            // Note that MIN_VALUE has a different meaning compared to Long.MIN_VALUE or Int.MIN_VALUE
            assertPrints(min(Double.MIN_VALUE, 0.0), "0.0")
            assertTrue(min(Double.POSITIVE_INFINITY, Double.MAX_VALUE) == Double.MAX_VALUE)
        }

        @Sample
        fun max() {
            assertPrints(max(-1.0, 1.0), "1.0")
            assertPrints(max(1.0, -1.0), "1.0")
            assertPrints(max(-3.0, -1.0), "-1.0")
            assertPrints(max(-0.0, 0.0), "0.0")

            // special cases
            assertPrints(max(Double.NaN, 0.0), "NaN")
            assertPrints(max(-9000.0, Double.NaN), "NaN")
            // Note that MIN_VALUE has a different meaning compared to Long.MIN_VALUE or Int.MIN_VALUE
            assertTrue(max(Double.MIN_VALUE, 0.0) == Double.MIN_VALUE)
            assertPrints(max(Double.POSITIVE_INFINITY, Double.MAX_VALUE), "Infinity")
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
            // 3.5 is between 3.0 and 4.0, so it is rounded towards an even number 4.0
            assertPrints(round(3.5f), "4.0")
            // 2.5 is between 2.0 and 3.0, so it is rounded towards an even number 2.0
            assertPrints(round(2.5f), "2.0")
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
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

        @Sample
        fun abs() {
            assertPrints(abs(3.14f), "3.14")
            assertPrints(abs(-3.14f), "3.14")
            assertPrints(abs(-0.0f), "0.0")
            assertPrints(abs(Float.NEGATIVE_INFINITY), "Infinity")
            assertPrints(abs(Float.NaN), "NaN")
        }

        @Sample
        fun absoluteValue() {
            assertPrints(3.14f.absoluteValue, "3.14")
            assertPrints((-3.14f).absoluteValue, "3.14")
            assertPrints((-0.0f).absoluteValue, "0.0")
            assertPrints(Float.NEGATIVE_INFINITY.absoluteValue, "Infinity")
            assertPrints(Float.NaN.absoluteValue, "NaN")
        }

        @Sample
        fun withSignFloat() {
            assertPrints(3.14f.withSign(3.14f), "3.14")
            assertPrints(3.14f.withSign(-3.14f), "-3.14")
            assertPrints((-3.14f).withSign(-3.14f), "-3.14")
            assertPrints((-3.14f).withSign(0.0f), "3.14")
            assertPrints(Float.NaN.withSign(-1.0f), "NaN")
        }

        @Sample
        fun withSignInt() {
            assertPrints(3.14f.withSign(100), "3.14")
            assertPrints(3.14f.withSign(-100), "-3.14")
            assertPrints((-3.14f).withSign(-100), "-3.14")
            assertPrints((-3.14f).withSign(0), "3.14")
            assertPrints(Float.NaN.withSign(-1), "NaN")
        }

        @Sample
        fun cbrt() {
            assertPrints(cbrt(8.0f), "2.0")
            assertPrints(cbrt(-8.0f), "-2.0")

            // special cases
            assertPrints(cbrt(Float.NaN), "NaN")
            assertPrints(cbrt(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(cbrt(Float.NEGATIVE_INFINITY), "-Infinity")
            assertPrints(cbrt(-0.0f), "-0.0")
        }

        @Sample
        fun sqrt() {
            assertPrints(sqrt(9.0f), "3.0")
            assertPrints(sqrt(5.76f), "2.4")

            // special cases
            assertPrints(sqrt(-0.0f), "-0.0")
            assertPrints(sqrt(-4.0f), "NaN")
            assertPrints(sqrt(Float.NaN), "NaN")
            assertPrints(sqrt(Float.POSITIVE_INFINITY), "Infinity")
        }

        @Sample
        fun hypot() {
            // sqrt(6 * 6 + 8 * 8) = sqrt(36 + 64) = sqrt(100) = 10
            assertPrints(hypot(6.0f, 8.0f), "10.0")

            // special cases
            assertPrints(hypot(Float.NaN, 2.0f), "NaN")
            assertPrints(hypot(1.0f, Float.NaN), "NaN")
            assertPrints(hypot(Float.POSITIVE_INFINITY, 2.0f), "Infinity")
            assertPrints(hypot(1.0f, Float.NEGATIVE_INFINITY), "Infinity")
        }

        @Sample
        fun powFloat() {
            assertPrints(2.0f.pow(2.0f), "4.0")
            assertPrints(3.0f.pow(1.0f), "3.0")
            assertPrints(4.0f.pow(0.0f), "1.0")
            assertPrints(2.0f.pow(-2.0f), "0.25")
            assertPrints(4.0f.pow(0.5f), "2.0")
            assertPrints((-2.0f).pow(2.0f), "4.0")

            // special cases
            assertPrints(Float.NaN.pow(42.0f), "NaN")
            assertPrints(Float.NaN.pow(0.0f), "1.0")
            assertPrints(3.0f.pow(Float.NaN), "NaN")
            assertPrints((-2.0f).pow(0.75f), "NaN")
            assertPrints(1.0f.pow(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(1.0f.pow(Float.NEGATIVE_INFINITY), "NaN")
            assertPrints(2.0f.pow(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(2.0f.pow(Float.NEGATIVE_INFINITY), "0.0")
        }

        @Sample
        fun powInt() {
            assertPrints(2.0f.pow(2), "4.0")
            assertPrints(3.0f.pow(1), "3.0")
            assertPrints(4.0f.pow(0), "1.0")
            assertPrints(2.0f.pow(-2), "0.25")
            assertPrints((-2.0f).pow(2), "4.0")

            // special cases
            assertPrints(Float.NaN.pow(42), "NaN")
            assertPrints(Float.NaN.pow(0), "1.0")
        }

        @Sample
        fun exp() {
            fun Float.firstFiveDigits(): String = toString().substring(0, 7)

            assertPrints(exp(1.0f).firstFiveDigits(), "2.71828")
            assertPrints(exp(2.0f).firstFiveDigits(), "7.38905")
            assertPrints(exp(-1.0f).firstFiveDigits(), "0.36787")

            // special cases
            assertPrints(exp(Float.NaN), "NaN")
            assertPrints(exp(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(exp(Float.NEGATIVE_INFINITY), "0.0")
        }

        @Sample
        fun expm1() {
            fun Float.firstFiveDigits(): String = toString().substring(0, 7)

            assertPrints(expm1(1.0f).firstFiveDigits(), "1.71828")
            // While it does not really matter for relatively large x values (like 1.0),
            // the difference is getting more pronounced when x is getting closer to 0.0
            assertPrints(exp(1e-17f) - 1.0f, "0.0")
            assertPrints(expm1(1e-17f), 1e-17f.toString())

            // special cases
            assertPrints(expm1(Float.NaN), "NaN")
            assertPrints(expm1(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(expm1(Float.NEGATIVE_INFINITY), "-1.0")
        }

        @Sample
        fun sin() {
            val epsilon = 1e-6f

            assertPrints(sin(0.0f), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // sin(π/2) = 1.0
            assertTrue((sin(PI.toFloat() / 2) - 1.0f).absoluteValue < epsilon)
            // sin(-π/2) = -1.0
            assertTrue((sin(-PI.toFloat() / 2) - -1.0f).absoluteValue < epsilon)

            // special cases
            assertPrints(sin(Float.NaN), "NaN")
            assertPrints(sin(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(sin(Float.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun cos() {
            val epsilon = 1e-6f

            assertPrints(cos(0.0f), "1.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // cos(π/2) = 0
            assertTrue(cos(PI.toFloat() / 2).absoluteValue < epsilon)
            assertPrints(cos(PI.toFloat()), "-1.0")

            // special cases
            assertPrints(cos(Float.NaN), "NaN")
            assertPrints(cos(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(cos(Float.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun tan() {
            val epsilon = 1e-6f

            assertPrints(tan(0.0f), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // tan(π/4) = 1.0
            assertTrue((tan(PI.toFloat() / 4) - 1.0f).absoluteValue < epsilon)
            // tan(-π/4) = -1.0
            assertTrue((tan(-PI.toFloat() / 4) - -1.0f).absoluteValue < epsilon)

            // special cases
            assertPrints(tan(Float.NaN), "NaN")
            assertPrints(tan(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(tan(Float.NEGATIVE_INFINITY), "NaN")
        }

        @Sample
        fun asin() {
            val epsilon = 1e-6f

            assertPrints(asin(0.0f), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // asin(1.0) = π/2
            assertTrue((asin(1.0f) - PI.toFloat() / 2).absoluteValue < epsilon)
            // asin(-1.0) = -π/2
            assertTrue((asin(-1.0f) - (-PI.toFloat() / 2)).absoluteValue < epsilon)
            // asin(sin(x)) = x
            assertTrue((asin(sin(0.25f)) - 0.25f).absoluteValue < epsilon)

            // special cases
            assertPrints(asin(Float.NaN), "NaN")
            assertPrints(asin(1.1f), "NaN")
            assertPrints(asin(-2.0f), "NaN")
        }

        @Sample
        fun acos() {
            val epsilon = 1e-6f

            assertPrints(acos(1.0f), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // acos(0.0) = π/2
            assertTrue((acos(0.0f) - PI.toFloat() / 2).absoluteValue < epsilon)
            // acos(-1.0) = π
            assertTrue((acos(-1.0f) - PI.toFloat()).absoluteValue < epsilon)
            // acos(cos(x)) = x
            assertTrue((acos(cos(0.25f)) - 0.25f).absoluteValue < epsilon)

            // special cases
            assertPrints(acos(Float.NaN), "NaN")
            assertPrints(acos(1.1f), "NaN")
            assertPrints(acos(-2.0f), "NaN")
        }

        @Sample
        fun atan() {
            val epsilon = 1e-6f

            assertPrints(atan(0.0f), "0.0")
            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            // atan(1.0) = π/4
            assertTrue((atan(1.0f) - PI.toFloat() / 4).absoluteValue < epsilon)
            // atan(-1.0) = -π/4
            assertTrue((atan(-1.0f) - (-PI.toFloat() / 4)).absoluteValue < epsilon)
            // atan(tan(x)) = x
            assertTrue((atan(tan(0.25f)) - 0.25f).absoluteValue < epsilon)

            // special cases
            assertPrints(atan(Float.NaN), "NaN")
        }

        @Sample
        fun atan2() {
            val epsilon = 1e-6f
            fun Float.toDegrees(): Float = this * 180.0f / PI.toFloat()

            assertPrints(atan2(y = 0.0f, x = 0.0f), "0.0")

            // Results may not be exact, so we're only checking that they are within epsilon from the expected value
            assertTrue((atan2(y = 1.0f, x = 0.0f) - PI.toFloat() / 2).absoluteValue < epsilon)
            assertPrints(atan2(y = 1.0f, x = 0.0f).toDegrees(), "90.0")

            assertTrue((atan2(y = 0.0f, x = -1.0f) - PI.toFloat()).absoluteValue < epsilon)
            assertPrints(atan2(y = 0.0f, x = -1.0f).toDegrees(), "180.0")

            assertTrue((atan2(y = -1.0f, x = 0.0f) - (-PI.toFloat() / 2)).absoluteValue < epsilon)
            assertPrints(atan2(y = -1.0f, x = 0.0f).toDegrees(), "-90.0")

            // special cases (some of them)
            assertPrints(atan2(y = Float.NaN, x = 0.5f), "NaN")
            assertPrints(atan2(y = 0.5f, x = Float.NaN), "NaN")
            assertPrints(atan2(y = -0.0f, x = 100500.0f), "-0.0")
            assertPrints(atan2(y = 0.0f, x = Float.POSITIVE_INFINITY), "0.0")
            assertPrints(atan2(y = Float.POSITIVE_INFINITY, x = 100500.0f).toDegrees(), "90.0")
        }

        @Sample
        fun min() {
            assertPrints(min(-1.0f, 1.0f), "-1.0")
            assertPrints(min(1.0f, -1.0f), "-1.0")
            assertPrints(min(-3.0f, -1.0f), "-3.0")
            assertPrints(min(-0.0f, 0.0f), "-0.0")

            // special cases
            assertPrints(min(Float.NaN, 0.0f), "NaN")
            assertPrints(min(-9000.0f, Float.NaN), "NaN")
            // Note that MIN_VALUE has a different meaning compared to Long.MIN_VALUE or Int.MIN_VALUE
            assertPrints(min(Float.MIN_VALUE, 0.0f), "0.0")
            assertTrue(min(Float.POSITIVE_INFINITY, Float.MAX_VALUE) == Float.MAX_VALUE)
        }

        @Sample
        fun max() {
            assertPrints(max(-1.0f, 1.0f), "1.0")
            assertPrints(max(1.0f, -1.0f), "1.0")
            assertPrints(max(-3.0f, -1.0f), "-1.0")
            assertPrints(max(-0.0f, 0.0f), "0.0")

            // special cases
            assertPrints(max(Float.NaN, 0.0f), "NaN")
            assertPrints(max(-9000.0f, Float.NaN), "NaN")
            // Note that MIN_VALUE has a different meaning compared to Long.MIN_VALUE or Int.MIN_VALUE
            assertTrue(max(Float.MIN_VALUE, 0.0f) == Float.MIN_VALUE)
            assertPrints(max(Float.POSITIVE_INFINITY, Float.MAX_VALUE), "Infinity")
        }
    }

    class Longs {
        @Sample
        fun abs() {
            assertPrints(abs(42L), "42")
            assertPrints(abs(-42L), "42")
            // Special case: can't get the absolute value due to an overflow
            assertTrue(abs(Long.MIN_VALUE) == Long.MIN_VALUE)
        }

        @Sample
        fun absoluteValue() {
            assertPrints(42L.absoluteValue, "42")
            assertPrints((-42L).absoluteValue, "42")
            // Special case: can't get the absolute value due to an overflow
            assertTrue(Long.MIN_VALUE.absoluteValue == Long.MIN_VALUE)
        }

        @Sample
        fun sign() {
            assertPrints(0L.sign, "0")
            assertPrints(9000L.sign, "1")
            assertPrints((-9000L).sign, "-1")
            assertPrints(Long.MAX_VALUE.sign, "1")
            assertPrints(Long.MIN_VALUE.sign, "-1")
        }

        @Sample
        fun min() {
            assertPrints(min(-1L, 1L), "-1")
            assertPrints(min(1L, -1L), "-1")
            assertPrints(min(-3L, -1L), "-3")
        }

        @Sample
        fun max() {
            assertPrints(max(-1L, 1L), "1")
            assertPrints(max(1L, -1L), "1")
            assertPrints(max(-3L, -1L), "-1")
        }
    }

    class Ints {
        @Sample
        fun abs() {
            assertPrints(abs(42), "42")
            assertPrints(abs(-42), "42")
            // Special case: can't get the absolute value due to an overflow
            assertTrue(abs(Int.MIN_VALUE) == Int.MIN_VALUE)
        }

        @Sample
        fun absoluteValue() {
            assertPrints(42.absoluteValue, "42")
            assertPrints((-42).absoluteValue, "42")
            // Special case: can't get the absolute value due to an overflow
            assertTrue(Int.MIN_VALUE.absoluteValue == Int.MIN_VALUE)
        }

        @Sample
        fun sign() {
            assertPrints(0.sign, "0")
            assertPrints(9000.sign, "1")
            assertPrints((-9000).sign, "-1")
            assertPrints(Int.MAX_VALUE.sign, "1")
            assertPrints(Int.MIN_VALUE.sign, "-1")
        }

        @Sample
        fun min() {
            assertPrints(min(-1, 1), "-1")
            assertPrints(min(1, -1), "-1")
            assertPrints(min(-3, -1), "-3")
        }

        @Sample
        fun max() {
            assertPrints(max(-1, 1), "1")
            assertPrints(max(1, -1), "1")
            assertPrints(max(-3, -1), "-1")
        }
    }
}
