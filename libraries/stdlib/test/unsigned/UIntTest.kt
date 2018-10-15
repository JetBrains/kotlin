/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.unsigned

import kotlin.math.sign
import kotlin.random.Random
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




}