package test.text

import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.Test as test

class ParsePrimitivesJVMTest {

    @test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
    }

    @test fun toByte() {
        assertEquals(77.toByte(), "77".toByte())
        assertFails { "255".toByte() }
    }

    @test fun toShort() {
        assertEquals(77.toShort(), "77".toShort())
    }

    @test fun toInt() {
        assertEquals(77, "77".toInt())
        assertFails { "2147483648".toInt() }
        assertFails { "-2147483649".toInt() }
    }

    @test fun toLong() {
        assertEquals(77.toLong(), "77".toLong())
        assertFails { "9223372036854775808".toLong() }
        assertFails { "-9223372036854775809".toLong() }
    }

    @test fun toFloat() {
        assertEquals(77.0f, "77.0".toFloat())
        assertEquals(Float.NEGATIVE_INFINITY, "-1e39".toFloat())
        assertEquals(Float.POSITIVE_INFINITY, "1000000000000000000000000000000000000000".toFloat())
        assertFails { "dark side".toFloat() }
    }

    @test fun toDouble() {
        assertEquals(-77.0, "-77".toDouble())
        assertEquals(77.0, "77.".toDouble())
        assertEquals(77.0, "77.0".toDouble())

        assertEquals(-1.77, "-1.77".toDouble())
        assertEquals(0.77, "+.77".toDouble())

        assertEquals(-77.0, "\t-77 \n".toDouble())
        assertEquals(77.0, "7.7e1".toDouble())
        assertEquals(77.0, "+770e-1".toDouble())

        assertEquals(-Double.NaN, "-NaN".toDouble())
        assertEquals(Double.POSITIVE_INFINITY, "+Infinity".toDouble())

        assertEquals((0x77 shl 1).toDouble(), "0x77p1".toDouble())
        assertEquals(0x77.toDouble(), "0x.77P8".toDouble())

        assertFails { "7..7".toDouble() }
        assertFails { "0x77e1".toDouble() }
        assertFails { "007 not a number".toDouble() }
    }
}
