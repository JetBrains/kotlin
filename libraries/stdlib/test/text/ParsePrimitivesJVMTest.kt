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
        CompareBehaviorContext({it.toByte()}, {it.toByteOrNull()}).apply {
            assertProduce("+77", 77.toByte())
            assertProduce("-128", Byte.MIN_VALUE)
            assertFailsOrNull("128")
        }

        CompareBehaviorWithRadixContext(String::toByte, String::toByteOrNull).apply {
            assertProduce(16, "7F", 127.toByte())
            assertFailsOrNull(2, "10000000")
        }
    }

    @test fun toShort() {
        CompareBehaviorContext({it.toShort()}, {it.toShortOrNull()}).apply {
            assertProduce("77", 77.toShort())
            assertProduce("-32768", Short.MIN_VALUE)
            assertFailsOrNull("+32768")
        }

        CompareBehaviorWithRadixContext(String::toShort, String::toShortOrNull).apply {
            assertProduce(16, "7F", 127.toShort())
            assertFailsOrNull(5, "10000000")
        }
    }

    @test fun toInt() {
        CompareBehaviorContext({it.toInt()}, {it.toIntOrNull()}).apply {
            assertProduce("77", 77)
            assertProduce("+2147483647", Int.MAX_VALUE)
            assertProduce("-2147483648", Int.MIN_VALUE)

            assertFailsOrNull("2147483648")
            assertFailsOrNull("-2147483649")
            assertFailsOrNull("239239kotlin")
        }

        CompareBehaviorWithRadixContext(String::toInt, String::toIntOrNull).apply {
            assertProduce(10, "0", 0)
            assertProduce(10, "473", 473)
            assertProduce(10, "+42", 42)
            assertProduce(10, "-0", 0)
            assertProduce(10, "2147483647", 2147483647)
            assertProduce(10, "-2147483648", -2147483648)

            assertProduce(16, "-FF", -255)
            assertProduce(2, "1100110", 102)
            assertProduce(27, "Kona", 411787)

            assertFailsOrNull(10, "2147483648")
            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Kona")
        }

        assertFailsWith<NumberFormatException>("Expected to fail with radix 1") { "1".toInt(radix = 1) }
        assertFailsWith<NumberFormatException>("Expected to fail with radix 37") { "37".toIntOrNull(radix = 37) }
    }

    @test fun toLong() {
        CompareBehaviorContext({it.toLong()}, {it.toLongOrNull()}).apply {
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

        CompareBehaviorWithRadixContext(String::toLong, String::toLongOrNull).apply {
            assertProduce(10, "0", 0L)
            assertProduce(10, "473", 473L)
            assertProduce(10, "+42", 42L)
            assertProduce(10, "-0", 0L)

            assertProduce(16, "-FF", -255L)
            assertProduce(2, "1100110", 102L)
            assertProduce(36, "Hazelnut", 1356099454469L)

            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Hazelnut")
        }

        assertFailsWith<NumberFormatException>("Expected to fail with radix 37") { "37".toLong(radix = 37) }
        assertFailsWith<NumberFormatException>("Expected to fail with radix 1") { "1".toLongOrNull(radix = 1) }
    }

    @test fun toFloat() {
        CompareBehaviorContext({it.toFloat()}, {it.toFloatOrNull()}).apply {
            assertProduce("77.0", 77.0f)
            assertProduce("-1e39", Float.NEGATIVE_INFINITY)
            assertProduce("1000000000000000000000000000000000000000", Float.POSITIVE_INFINITY)
            assertFailsOrNull("dark side")
        }
    }

    @test fun toDouble() {
        CompareBehaviorContext({it.toDouble()}, {it.toDoubleOrNull()}).apply {
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

private class CompareBehaviorWithRadixContext<T: Any>(val convertOrFail: String.(Int) -> T,
                                                      val convertOrNull: String.(Int) -> T?) {
    fun assertProduce(radix: Int, input: String, output: T) {
        assertEquals(output, input.convertOrFail(radix))
        assertEquals(output, input.convertOrNull(radix))
    }

    fun assertFailsOrNull(radix: Int, input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\" with radix $radix",
                                               { input.convertOrFail(radix) })

        assertNull (input.convertOrNull(radix), message = "On input \"$input\" with radix $radix")
    }
}
