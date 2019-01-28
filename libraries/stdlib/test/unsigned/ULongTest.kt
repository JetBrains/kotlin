/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.unsigned

import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class ULongTest {

    private fun identity(u: ULong): ULong =
        (u.toLong() + 0).toULong()

    val zero = 0uL
    val one = 1uL
    val max = ULong.MAX_VALUE

    @Test
    fun equality() {

        fun testEqual(uv1: ULong, uv2: ULong) {
            assertEquals(uv1, uv2, "Boxed values should be equal")
            assertTrue(uv1.equals(uv2), "Boxed values should be equal: $uv1, $uv2")
            assertTrue(uv1 == uv2, "Values should be equal: $uv1, $uv2")
            assertEquals(uv1.hashCode(), uv2.hashCode())
            assertEquals((uv1 as Any).hashCode(), (uv2 as Any).hashCode())
            assertEquals(uv1.toString(), uv2.toString())
            assertEquals((uv1 as Any).toString(), (uv2 as Any).toString())
        }

        testEqual(one, identity(one))
        testEqual(max, identity(max))

        fun testNotEqual(uv1: ULong, uv2: ULong) {
            assertNotEquals(uv1, uv2, "Boxed values should be equal")
            assertTrue(uv1 != uv2, "Values should be not equal: $uv1, $uv2")
            assertNotEquals(uv1.toString(), uv2.toString())
            assertNotEquals((uv1 as Any).toString(), (uv2 as Any).toString())
        }

        testNotEqual(one, zero)
        testNotEqual(max, zero)
    }

    @Test
    fun convertToString() {
        fun testToString(expected: String, u: ULong) {
            assertEquals(expected, u.toString())
            assertEquals(expected, (u as Any).toString(), "Boxed toString")
            assertEquals(expected, "$u", "String template")
        }

        repeat(100) {
            val v = Random.nextLong() ushr 1
            testToString(v.toString(), v.toULong())
        }

        repeat(100) {
            val v = Random.nextLong(8446744073709551615L + 1)
            testToString("1${v.toString().padStart(19, '0')}", (5000000000000000000.toULong() * 2.toULong() + v.toULong()))
        }

        testToString("18446744073709551615", ULong.MAX_VALUE)
    }

    @Test
    fun comparisons() {
        fun <T> compare(op1: Comparable<T>, op2: T) = op1.compareTo(op2)

        fun testComparison(uv1: ULong, uv2: ULong, expected: Int) {
            val desc = "${uv1.toString()}, ${uv2.toString()}"
            assertEquals(expected, uv1.compareTo(uv2).sign, "compareTo: $desc")
            assertEquals(expected, (uv1 as Comparable<ULong>).compareTo(uv2).sign, "Comparable.compareTo: $desc")
            assertEquals(expected, compare(uv1, uv2).sign, "Generic compareTo: $desc")

            assertEquals(expected < 0, uv1 < uv2)
            assertEquals(expected <= 0, uv1 <= uv2)
            assertEquals(expected > 0, uv1 > uv2)
            assertEquals(expected >= 0, uv1 >= uv2)
        }

        fun testEquals(uv1: ULong, uv2: ULong) = testComparison(uv1, uv2, 0)
        fun testCompare(uv1: ULong, uv2: ULong, expected12: Int) {
            testComparison(uv1, uv2, expected12)
            testComparison(uv2, uv1, -expected12)
        }

        testEquals(one, identity(one))
        testEquals(max, identity(max))

        testCompare(zero, one, -1)
        testCompare(Long.MAX_VALUE.toULong(), zero, 1)

        testCompare(zero, ULong.MAX_VALUE, -1)
        testCompare((Long.MAX_VALUE).toULong() + one, ULong.MAX_VALUE, -1)
    }


    @Test
    fun convertToFloat() {
        fun testEquals(v1: Float, v2: ULong) = assertEquals(v1, v2.toFloat())

        testEquals(0.0f, zero)
        testEquals(1.0f, one)

        testEquals(2.0f.pow(ULong.SIZE_BITS) - 1, max)
        testEquals(2.0f * Long.MAX_VALUE + 1, max)

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            testEquals(long.toFloat(), long.toULong())
        }

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            val float = Long.MAX_VALUE.toFloat() + long.toFloat()    // We lose accuracy here, hence `eps` is used.
            val ulong = Long.MAX_VALUE.toULong() + long.toULong()

            val eps = 1e+13
            assertTrue(abs(float - ulong.toFloat()) < eps)
        }
    }

    @Test
    fun convertToDouble() {
        fun testEquals(v1: Double, v2: ULong) = assertEquals(v1, v2.toDouble())

        testEquals(0.0, zero)
        testEquals(1.0, one)

        testEquals(2.0.pow(ULong.SIZE_BITS) - 1, max)
        testEquals(2.0 * Long.MAX_VALUE + 1, max)

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            testEquals(long.toDouble(), long.toULong())
        }

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            val double = Long.MAX_VALUE.toDouble() + long.toDouble()    // We lose accuracy here, hence `eps` is used.
            val ulong = Long.MAX_VALUE.toULong() + long.toULong()

            val eps = 1e+4
            assertTrue(abs(double - ulong.toDouble()) < eps)
        }

        fun testRounding(from: ULong, count: UInt) {
            for (x in from..(from + count)) {
                val double = x.toDouble()
                val v = double.toULong()
                val down = double.nextDown().toULong()
                val up = double.nextUp().toULong()

                assertTrue(down <= x && down <= v)
                assertTrue(up >= x && up >= v)

                if (v > x) {
                    assertTrue(v - x <= x - down, "Expected $x being closer to $v than to $down")
                } else {
                    assertTrue(x - v <= up - x, "Expected $x being closer to $v than to $up")
                }
            }
        }

        testRounding(0u, 100u)
        testRounding(Long.MAX_VALUE.toULong() - 520u, 100u)
        testRounding(ULong.MAX_VALUE - 10000u, 10000u)
    }

    @Test
    fun convertDoubleToULong() {
        fun testEquals(v1: Double, v2: ULong) = assertEquals(v1.toULong(), v2)

        testEquals(0.0, zero)
        testEquals(-1.0, zero)

        testEquals(-2_000_000_000_000.0, zero)
        testEquals(-(2.0.pow(ULong.SIZE_BITS + 5)), zero)
        testEquals(Double.MIN_VALUE, zero)
        testEquals(Double.NEGATIVE_INFINITY, zero)
        testEquals(Double.NaN, zero)

        testEquals(1.0, one)

        testEquals(2_000_000_000_000_000_000_000.0, max)
        testEquals(2.0.pow(ULong.SIZE_BITS), max)
        testEquals(2.0.pow(ULong.SIZE_BITS + 5), max)
        testEquals(Double.MAX_VALUE, max)
        testEquals(Double.POSITIVE_INFINITY, max)

        repeat(100) {
            val v = -Random.nextDouble(until = 2.0.pow(ULong.SIZE_BITS + 8))
            testEquals(v, zero)
        }

        repeat(100) {
            val v = Random.nextDouble(from = max.toDouble(), until = 2.0.pow(ULong.SIZE_BITS + 8))
            testEquals(v, max)
        }

        repeat(100) {
            val v = Random.nextDouble() * Long.MAX_VALUE
            testEquals(v, v.toLong().toULong())
        }

        repeat(100) {
            val diff = Random.nextDouble() * Long.MAX_VALUE
            val d = Long.MAX_VALUE.toDouble() + diff
            val ul = Long.MAX_VALUE.toULong() + diff.toLong().toULong()
            val eps = 1e-6

            assertTrue(d.toULong() / ul <= 1u)
            assertTrue(d / ul.toDouble() < 1 + eps)
            assertTrue(d / ul.toDouble() > 1 - eps)
        }

        fun testTrailingBits(v: Double, count: Int) {
            val mask = (1uL shl count) - 1uL
            assertEquals(0uL, v.toULong() and mask)
        }

        var withTrailingZeros = 2.0.pow(64)
        repeat(10) {
            withTrailingZeros = withTrailingZeros.nextDown()
            testTrailingBits(withTrailingZeros, 11)
        }

        withTrailingZeros = 2.0.pow(63)
        repeat(10) {
            testTrailingBits(withTrailingZeros, 11)
            withTrailingZeros = withTrailingZeros.nextUp()
        }

        repeat(100) {
            val msb = Random.nextInt(53, 64)
            val v = 2.0.pow(msb) * (1.0 + Random.nextDouble())
            testTrailingBits(v, msb - 52)
        }
    }
}