package test.text

import kotlin.test.*
import org.junit.Test as test

class ParsePrimitivesJVMTest {

    @test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
    }

    @test fun toByte() {
        CompareBehaviorContext(String::toByte, String::toByteOrNull).apply {

            assertProduce("+77", 77.toByte())
            assertProduce("-128", Byte.MIN_VALUE)
            assertFailsOrNull("128")
        }
    }

    @test fun toShort() {
        CompareBehaviorContext(String::toShort, String::toShortOrNull).apply {

            assertProduce("+77", 77.toShort())
            assertProduce("32767", Short.MAX_VALUE)
            assertProduce("-32768", Short.MIN_VALUE)
            assertFailsOrNull("+32768")
        }
    }

    @test fun toInt() {
        CompareBehaviorContext(String::toInt, String::toIntOrNull).apply {

            assertProduce("+77", 77)
            assertProduce("2147483647", Int.MAX_VALUE)
            assertProduce("-2147483648", Int.MIN_VALUE)

            assertFailsOrNull("2147483648")
            assertFailsOrNull("-2147483649")
            assertFailsOrNull("239239kotlin")
        }
    }

    @test fun toLong() {
        CompareBehaviorContext(String::toLong, String::toLongOrNull).apply {

            assertProduce("77", 77.toLong())
            assertProduce("+9223372036854775807", Long.MAX_VALUE)
            assertProduce("-9223372036854775808", Long.MIN_VALUE)

            assertFailsOrNull("9223372036854775808")
            assertFailsOrNull("-9223372036854775809")
            assertFailsOrNull("922337 75809")
            assertFailsOrNull("92233,75809")
            assertFailsOrNull("92233`75809")
            assertFailsOrNull("-922337KOTLIN775809")
        }
    }

    @test fun toFloat() {
        CompareBehaviorContext(String::toFloat, String::toFloatOrNull).apply {

            assertProduce("+77.0", 77.0f)
            assertProduce("-1e39", Float.NEGATIVE_INFINITY)
            assertProduce("1000000000000000000000000000000000000000", Float.POSITIVE_INFINITY)
            assertFailsOrNull("dark side")
        }
    }

    @test fun toDouble() {
        CompareBehaviorContext(String::toDouble, String::toDoubleOrNull).apply {

            assertProduce("-77", -77.0)
            assertProduce("77.", 77.0)
            assertProduce("77.0", 77.0)
            assertProduce("-1.77", -1.77)
            assertProduce("+.77", 0.77)
            assertProduce("\t-77 \n", -77.0)
            assertProduce("7.7e1", 77.0)
            assertProduce("+770e-1", 77.0)

            assertProduce("-NaN", -Double.NaN)
            assertProduce("+Infinity", Double.POSITIVE_INFINITY)
            assertProduce("0x77p1", (0x77 shl 1).toDouble())
            assertProduce("0x.77P8", 0x77.toDouble())

            assertFailsOrNull("7..7")
            assertFailsOrNull("0x77e1")
            assertFailsOrNull("007 not a number")
        }
    }
}


private class CompareBehaviorContext<T: Any>(val convertOrFail: (String) -> T,
                                             val convertOrNull: (String) -> T?) {
    fun assertProduce(input: String, output: T) {
        assertEquals(output, convertOrFail(input))
        assertEquals(output, convertOrNull(input))
    }

    fun assertFailsOrNull(input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\"") { convertOrFail(input) }
        assertNull (convertOrNull(input), message = "On input \"$input\"")
    }
}
