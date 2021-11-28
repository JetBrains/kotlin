/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.unsigned

import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

class UIntTest {

    private fun identity(u: UInt): UInt =
        (u.toInt() + 0).toUInt()

    val zero = 0u
    val one = 1u
    val max = UInt.MAX_VALUE

    @Test
    fun equality() {

        fun testEqual(uv1: UInt, uv2: UInt) {
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

        fun testNotEqual(uv1: UInt, uv2: UInt) {
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
        fun testToString(expected: String, u: UInt) {
            assertEquals(expected, u.toString())
            assertEquals(expected, (u as Any).toString(), "Boxed toString")
            assertEquals(expected, "$u", "String template")
        }

        repeat(100) {
            val v = Random.nextBits(UInt.SIZE_BITS - 1)
            testToString(v.toString(), v.toUInt())
        }

        repeat(100) {
            val v = Random.nextInt(294967295 + 1)
            testToString("4${v.toString().padStart(9, '0')}", (2_000_000_000.toUInt() * 2.toUInt() + v.toUInt()))
        }

        testToString("4294967295", UInt.MAX_VALUE)
    }

    @Test
    fun operations() {
        assertEquals(2_147_483_648u, Int.MAX_VALUE.toUInt() + identity(1u))
        assertEquals(11u, UInt.MAX_VALUE + identity(12u))
        assertEquals(UInt.MAX_VALUE - 99u, 45u - identity(145u))

        assertEquals(UInt.MAX_VALUE - 1u, Int.MAX_VALUE.toUInt() * identity(2u))
        assertEquals(2_147_483_645u, Int.MAX_VALUE.toUInt() * identity(3u))

        testMulDivRem(125u, 3u, 41u, 2u)
        testMulDivRem(210u, 5u, 42u, 0u)
        testMulDivRem(UInt.MAX_VALUE, 256u, 16777215u, 255u)
        testMulDivRem(UInt.MAX_VALUE - 1u, UInt.MAX_VALUE, 0u, UInt.MAX_VALUE - 1u)
        testMulDivRem(UInt.MAX_VALUE, UInt.MAX_VALUE - 1u, 1u, 1u)
        testMulDivRem(UInt.MAX_VALUE, Int.MAX_VALUE.toUInt(), 2u, 1u)
    }


    private fun testMulDivRem(number: UInt, divisor: UInt, div: UInt, rem: UInt) {
        assertEquals(div, number / divisor)
        assertEquals(rem, number % divisor)
        assertEquals(div, number.floorDiv(divisor))
        assertEquals(rem, number.mod(divisor))

        assertEquals(number, div * divisor + rem)
        assertTrue(rem < divisor)
        assertTrue(div < number)
    }

    @Test
    fun divRem() = repeat(1000) {
        val number = Random.nextUInt()
        val divisor = Random.nextUInt(until = UInt.MAX_VALUE) + 1u
        testMulDivRem(number, divisor, number / divisor, number % divisor)
    }

    @Test
    fun comparisons() {
        fun <T> compare(op1: Comparable<T>, op2: T) = op1.compareTo(op2)

        fun testComparison(uv1: UInt, uv2: UInt, expected: Int) {
            val desc = "${uv1.toString()}, ${uv2.toString()}"
            assertEquals(expected, uv1.compareTo(uv2).sign, "compareTo: $desc")
            assertEquals(expected, (uv1 as Comparable<UInt>).compareTo(uv2).sign, "Comparable.compareTo: $desc")
            assertEquals(expected, compare(uv1, uv2).sign, "Generic compareTo: $desc")

            assertEquals(expected < 0, uv1 < uv2)
            assertEquals(expected <= 0, uv1 <= uv2)
            assertEquals(expected > 0, uv1 > uv2)
            assertEquals(expected >= 0, uv1 >= uv2)
        }

        fun testEquals(uv1: UInt, uv2: UInt) = testComparison(uv1, uv2, 0)
        fun testCompare(uv1: UInt, uv2: UInt, expected12: Int) {
            testComparison(uv1, uv2, expected12)
            testComparison(uv2, uv1, -expected12)
        }

        testEquals(one, identity(one))
        testEquals(max, identity(max))

        testCompare(zero, one, -1)
        testCompare(Int.MAX_VALUE.toUInt(), zero, 1)

        testCompare(zero, UInt.MAX_VALUE, -1)
        testCompare((Int.MAX_VALUE).toUInt() + one, UInt.MAX_VALUE, -1)
    }


    @Test
    fun convertToFloat() {
        fun testEquals(v1: Float, v2: UInt) = assertEquals(v1, v2.toFloat())

        testEquals(0.0f, zero)
        testEquals(1.0f, one)
        testEquals(0xFFFF_FFFF.toFloat(), max)

        repeat(100) {
            val long = Random.nextLong(0, 0xFFFF_FFFF)
            testEquals(long.toFloat(), long.toUInt())
        }
    }

    @Test
    fun convertToDouble() {
        fun testEquals(v1: Double, v2: UInt) = assertEquals(v1, v2.toDouble())

        testEquals(0.0, zero)
        testEquals(1.0, one)
        testEquals(max.toLong().toDouble(), max)

        repeat(100) {
            val long = Random.nextLong(0, max.toLong())
            testEquals(long.toDouble(), long.toUInt())
        }

        fun testRounding(from: UInt, count: UInt) {
            for (x in from..(from + count)) {
                val double = x.toDouble()
                val v = double.toUInt()
                val down = double.nextDown().toUInt()
                val up = double.nextUp().toUInt()

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
        testRounding(Int.MAX_VALUE.toUInt() - 10u, 100u)
        testRounding(UInt.MAX_VALUE - 100u, 100u)
    }

    @Test
    fun convertDoubleToUInt() {
        fun testEquals(v1: Double, v2: UInt) = assertEquals(v1.toUInt(), v2)

        testEquals(0.0, zero)
        testEquals(-1.0, zero)

        testEquals(-2_000_000_000_000.0, zero)
        testEquals(-(2.0.pow(UInt.SIZE_BITS + 12)), zero)
        testEquals(Double.MIN_VALUE, zero)
        testEquals(Double.NEGATIVE_INFINITY, zero)
        testEquals(Double.NaN, zero)

        testEquals(1.0, one)

        testEquals(2_000_000_000_000.0, max)
        testEquals(max.toDouble(), max)
        testEquals(2.0.pow(UInt.SIZE_BITS), max)
        testEquals(2.0.pow(UInt.SIZE_BITS + 12), max)
        testEquals(Double.MAX_VALUE, max)
        testEquals(Double.POSITIVE_INFINITY, max)

        repeat(100) {
            val v = -Random.nextDouble(until = 2.0.pow(UInt.SIZE_BITS + 8))
            testEquals(v, zero)
        }

        repeat(100) {
            val v = Random.nextDouble(from = max.toDouble(), until = 2.0.pow(UInt.SIZE_BITS + 8))
            testEquals(v, max)
        }

        repeat(100) {
            val v = Random.nextDouble(until = max.toDouble())
            testEquals(v, v.toLong().toUInt())
        }
    }
}