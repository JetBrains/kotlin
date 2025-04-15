/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.math.pow
import kotlin.math.sign
import kotlin.random.Random
import kotlin.test.*

class FloorDivModTest {

    @Test
    fun intDivMod() {
        fun check(a: Int, b: Int, expectedFd: Int? = null, expectedMod: Int? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.sign != b.sign && rem != 0) 1 else 0, fd)
                assertEquals(a - b * fd, mod)
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(10, -3, -4, -2)
        check(10, 3, 3, 1)
        check(-10, 3, -4, 2)
        check(-10, -3, 3, -1)
        check(-2, 2, -1, -0)
        val values = listOf(1, -1, 2, -2, 3, -3, Int.MIN_VALUE, Int.MAX_VALUE)
        for (a in values + 0) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextInt()
            val b = Random.nextInt().let { if (it == 0) 1 else it }
            check(a, b)
        }
    }

    @Test
    fun longDivMod() {
        fun check(a: Long, b: Long, expectedFd: Long? = null, expectedMod: Long? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.sign != b.sign && rem != 0L) 1 else 0, fd)
                assertEquals(a - b * fd, mod)
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(10, -3, -4, -2)
        check(10, 3, 3, 1)
        check(-10, 3, -4, 2)
        check(-10, -3, 3, -1)
        check(-2, 2, -1, -0)
        val values = listOf(1, -1, 2, -2, 3, -3, Long.MIN_VALUE, Long.MAX_VALUE)
        for (a in values + 0) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextLong()
            val b = Random.nextLong().let { if (it == 0L) 1 else it }
            check(a, b)
        }
    }

    @Test
    fun byteDivMod() {
        fun check(a: Byte, b: Byte, expectedFd: Int? = null, expectedMod: Byte? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.toInt().sign != b.toInt().sign && rem != 0) 1 else 0, fd)
                assertEquals(a - b * fd, mod.toInt())
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(10, -3, -4, -2)
        check(10, 3, 3, 1)
        check(-10, 3, -4, 2)
        check(-10, -3, 3, -1)
        check(-2, 2, -1, -0)
        val values = listOf(1, -1, 2, -2, 3, -3, Byte.MIN_VALUE, Byte.MAX_VALUE)
        for (a in values + 0) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextInt().toByte()
            val b = Random.nextInt().toByte().let { if (it == 0.toByte()) 1 else it }
            check(a, b)
        }
    }
    
    @Test
    fun shortDivMod() {
        fun check(a: Short, b: Short, expectedFd: Int? = null, expectedMod: Short? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.toInt().sign != b.toInt().sign && rem != 0) 1 else 0, fd)
                assertEquals(a - b * fd, mod.toInt())
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(10, -3, -4, -2)
        check(10, 3, 3, 1)
        check(-10, 3, -4, 2)
        check(-10, -3, 3, -1)
        check(-2, 2, -1, -0)
        val values = listOf(1, -1, 2, -2, 3, -3, Short.MIN_VALUE, Short.MAX_VALUE)
        for (a in values + 0) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextInt().toShort()
            val b = Random.nextInt().toShort().let { if (it == 0.toShort()) 1 else it }
            check(a, b)
        }
    }

    @Test
    fun longIntMod() {
        fun check(a: Long, b: Int, expectedFd: Long? = null, expectedMod: Int? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.sign != b.sign && rem != 0L) 1 else 0, fd)
                assertEquals(a - b * fd, mod.toLong())
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(Long.MAX_VALUE, 2, Long.MAX_VALUE / 2, 1)
        @Suppress("INTEGER_OPERATOR_RESOLVE_WILL_CHANGE") // KT-47729
        check(Long.MAX_VALUE, 1.shl(30), expectedMod = 1.shl(30) - 1)
        @Suppress("INTEGER_OPERATOR_RESOLVE_WILL_CHANGE") // KT-47729
        check(-1L, 1.shl(30), expectedMod = 1.shl(30) - 1)
        check(Long.MAX_VALUE, Int.MAX_VALUE, expectedMod = 1)
        check(Long.MAX_VALUE, Int.MIN_VALUE, expectedMod = -1)
    }

    @Test
    fun shortIntMod() {
        fun check(a: Short, b: Int, expectedFd: Int? = null, expectedMod: Int? = null) {
            val div = a / b
            val rem = a % b
            val fd = a.floorDiv(b)
            val mod = a.mod(b)

            try {
                expectedFd?.let { assertEquals(it, fd) }
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(div - if (a.toInt().sign != b.sign && rem != 0) 1 else 0, fd)
                assertEquals(a.toInt() - b * fd, mod) // a.toInt() to workaround Int-out-of-range in JS legacy
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, div: $div, rem: $rem, floorDiv: $fd, mod: $mod", e)
            }
        }

        check(Short.MAX_VALUE, Int.MAX_VALUE, 0, Short.MAX_VALUE.toInt())
        check(Short.MAX_VALUE, Int.MIN_VALUE, -1, Int.MIN_VALUE + Short.MAX_VALUE)
        @Suppress("INTEGER_OPERATOR_RESOLVE_WILL_CHANGE") // KT-47729
        check((-1).toShort(), 1.shl(30), -1, 1.shl(30) - 1)
    }

    @Test
    fun doubleMod() {
        fun check(a: Double, b: Double, expectedMod: Double? = null) {
            val rem = a % b
            val mod = a.mod(b)

            try {
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(if (rem.sign == mod.sign) rem else rem + b, mod)
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, rem: $rem, mod: $mod", e)
            }
        }

        check(10.125, -0.5, -0.375)
        check(10.125, 0.5, 0.125)
        check(-10.125, 0.5, 0.375)
        check(-10.125, -0.5, -0.125)
        check(-2.0, 2.0, -0.0)
        val large = 2.0.pow(53)
        check(0.025, large, 0.025)
        check(-0.025, large, expectedMod = large)
        check(0.025, -large, expectedMod = -large)
        check(1.0, Double.NaN, Double.NaN)
        check(Double.NaN, 1.0, Double.NaN)
        check(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN)
        check(Long.MAX_VALUE.toDouble(), 3.0, 2.0)
        check(Long.MAX_VALUE.toDouble(), -3.0, -1.0)
        check(Long.MAX_VALUE.toDouble(), 3.0, 2.0)
        check(Long.MAX_VALUE.toDouble(), -3.0, -1.0)
        val values = listOf(1.0, -1.0, 3.0, -3.0, large, -large, Double.MIN_VALUE, Double.MAX_VALUE,
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN)
        for (a in values + 0.0) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextDouble()
            val b = Random.nextDouble().let { if (it == 0.0) 1.0 else it }
            check(a, b)
        }
    }
    @Test
    fun floatMod() {
        fun check(a: Float, b: Float, expectedMod: Float? = null) {
            val rem = a % b
            val mod = a.mod(b)

            try {
                expectedMod?.let { assertEquals(it, mod) }
                assertEquals(if (rem.sign == mod.sign) rem else rem + b, mod)
            } catch (e: AssertionError) {
                fail("a: $a, b: $b, rem: $rem, mod: $mod", e)
            }
        }

        check(10.125f, -0.5f, -0.375f)
        check(10.125f, 0.5f, 0.125f)
        check(-10.125f, 0.5f, 0.375f)
        check(-10.125f, -0.5f, -0.125f)
        check(-2.0f, 2.0f, -0.0f)
        val large = 2.0f.pow(53)
        check(0.025f, large, 0.025f)
        check(-0.025f, large, expectedMod = large)
        check(0.025f, -large, expectedMod = -large)
        check(1.0f, Float.NaN, Float.NaN)
        check(Float.NaN, 1.0f, Float.NaN)
        check(Long.MAX_VALUE.toFloat(), 3.0f, 2.0f)
        check(Long.MAX_VALUE.toFloat(), -3.0f, -1.0f)
        check(Long.MAX_VALUE.toFloat(), 3.0f, 2.0f)
        check(Long.MAX_VALUE.toFloat(), -3.0f, -1.0f)
        check(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN)
        val values = listOf(1.0f, -1.0f, 3.0f, -3.0f, large, -large, Float.MIN_VALUE, Float.MAX_VALUE,
                            Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN)
        for (a in values + 0.0f) {
            for (b in values) {
                check(a, b)
            }
        }
        repeat(1000) {
            val a = Random.nextFloat()
            val b = Random.nextFloat().let { if (it == 0.0f) 1.0f else it }
            check(a, b)
        }
    }

}

