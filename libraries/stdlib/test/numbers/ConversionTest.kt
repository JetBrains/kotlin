/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class ConversionTest {

    @Test
    fun doubleToLong() {
        fun testEquals(expected: Long, v: Double) = assertEquals(expected, v.toLong())

        testEquals(0L, 0.0)
        testEquals(0L, Double.NaN)
        testEquals(0L, Double.MIN_VALUE)

        testEquals(1L, 1.0)
        testEquals(-1L, -1.0)

        testEquals(Long.MIN_VALUE, -2_000_000_000_000_000_000_000.0)
        testEquals(Long.MIN_VALUE, -(2.0.pow(Long.SIZE_BITS - 1)))
        testEquals(Long.MIN_VALUE, -(2.0.pow(Long.SIZE_BITS + 5)))
        testEquals(Long.MIN_VALUE, -Double.MAX_VALUE)
        testEquals(Long.MIN_VALUE, Double.NEGATIVE_INFINITY)

        testEquals(Long.MAX_VALUE, 2_000_000_000_000_000_000_000.0)
        testEquals(Long.MAX_VALUE, 2.0.pow(Long.SIZE_BITS - 1))
        testEquals(Long.MAX_VALUE, 2.0.pow(Long.SIZE_BITS + 5))
        testEquals(Long.MAX_VALUE, Double.MAX_VALUE)
        testEquals(Long.MAX_VALUE, Double.POSITIVE_INFINITY)

        repeat(100) {
            val v = Random.nextDouble(from = 2.0.pow(Long.SIZE_BITS - 1), until = 2.0.pow(Long.SIZE_BITS + 8))
            testEquals(Long.MIN_VALUE, -v)
            testEquals(Long.MAX_VALUE, v)
        }

        repeat(100) {
            val v = Random.nextLong(1L shl 53)
            testEquals(v, v.toDouble())
            testEquals(-v, -v.toDouble())
        }

        fun testTrailingBits(v: Double, count: Int) {
            val mask = (1L shl count) - 1L
            assertEquals(0L, v.toLong() and mask)
        }

        var withTrailingZeros = 2.0.pow(63)
        repeat(10) {
            withTrailingZeros = withTrailingZeros.nextDown()
            testTrailingBits(withTrailingZeros, 10)
        }

        withTrailingZeros = -(2.0.pow(63))
        repeat(10) {
            testTrailingBits(withTrailingZeros, 10)
            withTrailingZeros = withTrailingZeros.nextUp()
        }

        repeat(100) {
            val msb = Random.nextInt(53, 63)
            val v = 2.0.pow(msb) * (1.0 + Random.nextDouble())
            testTrailingBits(v, msb - 52)
        }
    }

    @Test
    fun doubleToInt() {
        fun testEquals(expected: Int, v: Double) = assertEquals(expected, v.toInt())

        testEquals(0, 0.0)
        testEquals(0, Double.NaN)
        testEquals(0, Double.MIN_VALUE)

        testEquals(1, 1.0)
        testEquals(-1, -1.0)

        testEquals(Int.MIN_VALUE, -2_000_000_000_000.0)
        testEquals(Int.MIN_VALUE, Int.MIN_VALUE.toDouble())
        testEquals(Int.MIN_VALUE, -(2.0.pow(Int.SIZE_BITS - 1)))
        testEquals(Int.MIN_VALUE, -(2.0.pow(Int.SIZE_BITS + 12)))
        testEquals(Int.MIN_VALUE, -Double.MAX_VALUE)
        testEquals(Int.MIN_VALUE, Double.NEGATIVE_INFINITY)

        testEquals(Int.MAX_VALUE, 2_000_000_000_000.0)
        testEquals(Int.MAX_VALUE, Int.MAX_VALUE.toDouble())
        testEquals(Int.MAX_VALUE, 2.0.pow(Int.SIZE_BITS - 1))
        testEquals(Int.MAX_VALUE, 2.0.pow(Int.SIZE_BITS + 12))
        testEquals(Int.MAX_VALUE, Double.MAX_VALUE)
        testEquals(Int.MAX_VALUE, Double.POSITIVE_INFINITY)

        repeat(100) {
            val v = Random.nextDouble(from = 2.0.pow(Int.SIZE_BITS - 1), until = 2.0.pow(Int.SIZE_BITS + 8))
            testEquals(Int.MIN_VALUE, -v)
            testEquals(Int.MAX_VALUE, v)
        }

        repeat(100) {
            val v = Random.nextDouble(from = Int.MIN_VALUE.toDouble(), until = Int.MAX_VALUE.toDouble())
            testEquals(v.toLong().toInt(), v)
        }
    }

    @Test
    fun floatToLong() {
        fun testEquals(expected: Long, v: Float) = assertEquals(expected, v.toLong())

        testEquals(0L, 0.0f)
        testEquals(0L, Float.NaN)
        testEquals(0L, Float.MIN_VALUE)

        testEquals(1L, 1.0f)
        testEquals(-1L, -1.0f)

        testEquals(Long.MIN_VALUE, -2_000_000_000_000_000_000_000.0f)
        testEquals(Long.MIN_VALUE, -(2.0f.pow(Long.SIZE_BITS - 1)))
        testEquals(Long.MIN_VALUE, -(2.0f.pow(Long.SIZE_BITS + 5)))
        testEquals(Long.MIN_VALUE, -Float.MAX_VALUE)
        testEquals(Long.MIN_VALUE, Float.NEGATIVE_INFINITY)

        testEquals(Long.MAX_VALUE, 2_000_000_000_000_000_000_000.0f)
        testEquals(Long.MAX_VALUE, 2.0f.pow(Long.SIZE_BITS - 1))
        testEquals(Long.MAX_VALUE, 2.0f.pow(Long.SIZE_BITS + 5))
        testEquals(Long.MAX_VALUE, Float.MAX_VALUE)
        testEquals(Long.MAX_VALUE, Float.POSITIVE_INFINITY)

        repeat(100) {
            val v = Random.nextDouble(from = 2.0.pow(Long.SIZE_BITS - 1), until = 2.0.pow(Long.SIZE_BITS + 8)).toFloat()
            testEquals(Long.MIN_VALUE, -v)
            testEquals(Long.MAX_VALUE, v)
        }

        repeat(100) {
            val v = Random.nextLong(1L shl 23)
            testEquals(v, v.toFloat())
            testEquals(-v, -v.toFloat())
        }
    }

    @Test
    fun floatToInt() {
        fun testEquals(expected: Int, v: Float) = assertEquals(expected, v.toInt())

        testEquals(0, 0.0f)
        testEquals(0, Float.NaN)
        testEquals(0, Float.MIN_VALUE)

        testEquals(1, 1.0f)
        testEquals(-1, -1.0f)

        testEquals(Int.MIN_VALUE, -2_000_000_000_000.0f)
        testEquals(Int.MIN_VALUE, -(2.0f.pow(Int.SIZE_BITS - 1)))
        testEquals(Int.MIN_VALUE, -(2.0f.pow(Int.SIZE_BITS + 12)))
        testEquals(Int.MIN_VALUE, -Float.MAX_VALUE)
        testEquals(Int.MIN_VALUE, Float.NEGATIVE_INFINITY)

        testEquals(Int.MAX_VALUE, 2_000_000_000_000.0f)
        testEquals(Int.MAX_VALUE, 2.0f.pow(Int.SIZE_BITS - 1))
        testEquals(Int.MAX_VALUE, 2.0f.pow(Int.SIZE_BITS + 12))
        testEquals(Int.MAX_VALUE, Float.MAX_VALUE)
        testEquals(Int.MAX_VALUE, Float.POSITIVE_INFINITY)

        repeat(100) {
            val v = Random.nextDouble(from = 2.0.pow(Int.SIZE_BITS - 1), until = 2.0.pow(Int.SIZE_BITS + 8)).toFloat()
            testEquals(Int.MIN_VALUE, -v)
            testEquals(Int.MAX_VALUE, v)
        }

        repeat(100) {
            val v = Random.nextInt(1 shl 23)
            testEquals(v, v.toFloat())
            testEquals(-v, -v.toFloat())
        }
    }
}