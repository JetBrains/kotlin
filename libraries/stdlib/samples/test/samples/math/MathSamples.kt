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
        fun abs() {
            assertPrints(abs(3.14), "3.14")
            assertPrints(abs(-3.14), "3.14")
            assertPrints(abs(0.0), "0.0")
            
            // Special cases
            assertPrints(abs(Double.NaN), "NaN")
            assertPrints(abs(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(abs(Double.NEGATIVE_INFINITY), "Infinity")
        }
        
        @Sample
        fun min() {
            assertPrints(min(1.0, 2.0), "1.0")
            assertPrints(min(-1.0, -2.0), "-2.0")
            assertPrints(min(-1.0, 2.0), "-1.0")
            assertPrints(min(0.0, 0.0), "0.0")
            
            // Special cases
            assertPrints(min(Double.NaN, 1.0), "NaN")
            assertPrints(min(1.0, Double.NaN), "NaN")
            assertPrints(min(Double.POSITIVE_INFINITY, 1.0), "1.0")
            assertPrints(min(Double.NEGATIVE_INFINITY, 1.0), "-Infinity")
        }
        
        @Sample
        fun max() {
            assertPrints(max(1.0, 2.0), "2.0")
            assertPrints(max(-1.0, -2.0), "-1.0")
            assertPrints(max(-1.0, 2.0), "2.0")
            assertPrints(max(0.0, 0.0), "0.0")
            
            // Special cases
            assertPrints(max(Double.NaN, 1.0), "NaN")
            assertPrints(max(1.0, Double.NaN), "NaN")
            assertPrints(max(Double.POSITIVE_INFINITY, 1.0), "Infinity")
            assertPrints(max(Double.NEGATIVE_INFINITY, 1.0), "1.0")
        }
        
        @Sample
        fun sin() {
            // Values close to common angles
            assertPrints(sin(0.0), "0.0")
            assertEquals(1.0, sin(PI / 2), 1e-15)
            assertEquals(0.0, sin(PI), 1e-15)
            assertEquals(-1.0, sin(3 * PI / 2), 1e-15)
            assertEquals(0.0, sin(2 * PI), 1e-15)
            
            // Special cases
            assertPrints(sin(Double.NaN), "NaN")
            assertPrints(sin(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(sin(Double.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun cos() {
            // Values close to common angles
            assertEquals(1.0, cos(0.0), 1e-15)
            assertEquals(0.0, cos(PI / 2), 1e-15)
            assertEquals(-1.0, cos(PI), 1e-15)
            assertEquals(0.0, cos(3 * PI / 2), 1e-15)
            assertEquals(1.0, cos(2 * PI), 1e-15)
            
            // Special cases
            assertPrints(cos(Double.NaN), "NaN")
            assertPrints(cos(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(cos(Double.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun tan() {
            // Values close to common angles
            assertEquals(0.0, tan(0.0), 1e-15)
            assertEquals(0.0, tan(PI), 1e-15)
            assertEquals(0.0, tan(2 * PI), 1e-15)
            
            // Special cases
            assertPrints(tan(Double.NaN), "NaN")
            assertPrints(tan(Double.POSITIVE_INFINITY), "NaN")
            assertPrints(tan(Double.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun sqrt() {
            assertPrints(sqrt(0.0), "0.0")
            assertPrints(sqrt(1.0), "1.0")
            assertPrints(sqrt(4.0), "2.0")
            assertEquals(1.4142135623730951, sqrt(2.0), 1e-15)
            
            // Special cases
            assertPrints(sqrt(Double.NaN), "NaN")
            assertPrints(sqrt(-1.0), "NaN")
            assertPrints(sqrt(Double.POSITIVE_INFINITY), "Infinity")
        }
        
        @Sample
        fun pow() {
            assertPrints(2.0.pow(0.0), "1.0")
            assertPrints(2.0.pow(1.0), "2.0")
            assertPrints(2.0.pow(2.0), "4.0")
            assertEquals(1.4142135623730951, 2.0.pow(0.5), 1e-15)
            assertPrints((-2.0).pow(2.0), "4.0")
            assertPrints((-2.0).pow(3.0), "-8.0")
            
            // Special cases
            assertPrints(Double.NaN.pow(1.0), "NaN")
            assertPrints(1.0.pow(Double.NaN), "NaN")
            assertPrints(1.0.pow(Double.POSITIVE_INFINITY), "NaN")
            assertPrints((-1.0).pow(0.5), "NaN") // Negative base with non-integer exponent
        }
        
        @Sample
        fun exp() {
            assertPrints(exp(0.0), "1.0")
            assertEquals(E, exp(1.0), 1e-15)
            assertEquals(E * E, exp(2.0), 1e-15)
            
            // Special cases
            assertPrints(exp(Double.NaN), "NaN")
            assertPrints(exp(Double.POSITIVE_INFINITY), "Infinity")
            assertPrints(exp(Double.NEGATIVE_INFINITY), "0.0")
        }
        
        @Sample
        fun ln() {
            assertPrints(ln(1.0), "0.0")
            assertEquals(1.0, ln(E), 1e-15)
            assertEquals(2.0, ln(E * E), 1e-15)
            
            // Special cases
            assertPrints(ln(Double.NaN), "NaN")
            assertPrints(ln(-1.0), "NaN")
            assertPrints(ln(0.0), "-Infinity")
            assertPrints(ln(Double.POSITIVE_INFINITY), "Infinity")
        }
        
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
    }

    class Floats {
        @Sample
        fun abs() {
            assertPrints(abs(3.14f), "3.14")
            assertPrints(abs(-3.14f), "3.14")
            assertPrints(abs(0.0f), "0.0")
            
            // Special cases
            assertPrints(abs(Float.NaN), "NaN")
            assertPrints(abs(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(abs(Float.NEGATIVE_INFINITY), "Infinity")
        }
        
        @Sample
        fun min() {
            assertPrints(min(1.0f, 2.0f), "1.0")
            assertPrints(min(-1.0f, -2.0f), "-2.0")
            assertPrints(min(-1.0f, 2.0f), "-1.0")
            assertPrints(min(0.0f, 0.0f), "0.0")
            
            // Special cases
            assertPrints(min(Float.NaN, 1.0f), "NaN")
            assertPrints(min(1.0f, Float.NaN), "NaN")
            assertPrints(min(Float.POSITIVE_INFINITY, 1.0f), "1.0")
            assertPrints(min(Float.NEGATIVE_INFINITY, 1.0f), "-Infinity")
        }
        
        @Sample
        fun max() {
            assertPrints(max(1.0f, 2.0f), "2.0")
            assertPrints(max(-1.0f, -2.0f), "-1.0")
            assertPrints(max(-1.0f, 2.0f), "2.0")
            assertPrints(max(0.0f, 0.0f), "0.0")
            
            // Special cases
            assertPrints(max(Float.NaN, 1.0f), "NaN")
            assertPrints(max(1.0f, Float.NaN), "NaN")
            assertPrints(max(Float.POSITIVE_INFINITY, 1.0f), "Infinity")
            assertPrints(max(Float.NEGATIVE_INFINITY, 1.0f), "1.0")
        }
        
        @Sample
        fun sin() {
            // Values close to common angles
            assertPrints(sin(0.0f), "0.0")
            assertEquals(1.0f, sin(PI.toFloat() / 2), 1e-6f)
            assertEquals(0.0f, sin(PI.toFloat()), 1e-6f)
            assertEquals(-1.0f, sin(3 * PI.toFloat() / 2), 1e-6f)
            assertEquals(0.0f, sin(2 * PI.toFloat()), 1e-6f)
            
            // Special cases
            assertPrints(sin(Float.NaN), "NaN")
            assertPrints(sin(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(sin(Float.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun cos() {
            // Values close to common angles
            assertEquals(1.0f, cos(0.0f), 1e-6f)
            assertEquals(0.0f, cos(PI.toFloat() / 2), 1e-6f)
            assertEquals(-1.0f, cos(PI.toFloat()), 1e-6f)
            assertEquals(0.0f, cos(3 * PI.toFloat() / 2), 1e-6f)
            assertEquals(1.0f, cos(2 * PI.toFloat()), 1e-6f)
            
            // Special cases
            assertPrints(cos(Float.NaN), "NaN")
            assertPrints(cos(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(cos(Float.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun tan() {
            // Values close to common angles
            assertEquals(0.0f, tan(0.0f), 1e-6f)
            assertEquals(0.0f, tan(PI.toFloat()), 1e-6f)
            assertEquals(0.0f, tan(2 * PI.toFloat()), 1e-6f)
            
            // Special cases
            assertPrints(tan(Float.NaN), "NaN")
            assertPrints(tan(Float.POSITIVE_INFINITY), "NaN")
            assertPrints(tan(Float.NEGATIVE_INFINITY), "NaN")
        }
        
        @Sample
        fun sqrt() {
            assertPrints(sqrt(0.0f), "0.0")
            assertPrints(sqrt(1.0f), "1.0")
            assertPrints(sqrt(4.0f), "2.0")
            assertEquals(1.4142135f, sqrt(2.0f), 1e-6f)
            
            // Special cases
            assertPrints(sqrt(Float.NaN), "NaN")
            assertPrints(sqrt(-1.0f), "NaN")
            assertPrints(sqrt(Float.POSITIVE_INFINITY), "Infinity")
        }
        
        @Sample
        fun pow() {
            assertPrints(2.0f.pow(0.0f), "1.0")
            assertPrints(2.0f.pow(1.0f), "2.0")
            assertPrints(2.0f.pow(2.0f), "4.0")
            assertEquals(1.4142135f, 2.0f.pow(0.5f), 1e-6f)
            assertPrints((-2.0f).pow(2.0f), "4.0")
            assertPrints((-2.0f).pow(3.0f), "-8.0")
            
            // Special cases
            assertPrints(Float.NaN.pow(1.0f), "NaN")
            assertPrints(1.0f.pow(Float.NaN), "NaN")
            assertPrints(1.0f.pow(Float.POSITIVE_INFINITY), "NaN")
            assertPrints((-1.0f).pow(0.5f), "NaN") // Negative base with non-integer exponent
        }
        
        @Sample
        fun exp() {
            assertPrints(exp(0.0f), "1.0")
            assertEquals(E.toFloat(), exp(1.0f), 1e-6f)
            assertEquals((E * E).toFloat(), exp(2.0f), 1e-6f)
            
            // Special cases
            assertPrints(exp(Float.NaN), "NaN")
            assertPrints(exp(Float.POSITIVE_INFINITY), "Infinity")
            assertPrints(exp(Float.NEGATIVE_INFINITY), "0.0")
        }
        
        @Sample
        fun ln() {
            assertPrints(ln(1.0f), "0.0")
            assertEquals(1.0f, ln(E.toFloat()), 1e-6f)
            assertEquals(2.0f, ln((E * E).toFloat()), 1e-6f)
            
            // Special cases
            assertPrints(ln(Float.NaN), "NaN")
            assertPrints(ln(-1.0f), "NaN")
            assertPrints(ln(0.0f), "-Infinity")
            assertPrints(ln(Float.POSITIVE_INFINITY), "Infinity")
        }
        
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
    }
}
