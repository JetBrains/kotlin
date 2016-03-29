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
        assertEqualsOrFailsNullable(77.toByte(), "+77", String::toByte, String::toByteOrNull)
        assertEqualsOrFailsNullable(Byte.MIN_VALUE, "-128", String::toByte, String::toByteOrNull)
        assertEqualsOrFailsNullable(null, "128", String::toByte, String::toByteOrNull)
    }

    @test fun toShort() {
        assertEqualsOrFailsNullable(77.toShort(), "77", String::toShort, String::toShortOrNull)
        assertEqualsOrFailsNullable(Short.MIN_VALUE, "-32768", String::toShort, String::toShortOrNull)
        assertEqualsOrFailsNullable(null, "+32768", String::toShort, String::toShortOrNull)
    }

    @test fun toInt() {
        assertEqualsOrFailsNullable(77, "77", String::toInt, String::toIntOrNull)
        assertEqualsOrFailsNullable(Int.MAX_VALUE, "+2147483647", String::toInt, String::toIntOrNull)
        assertEqualsOrFailsNullable(Int.MIN_VALUE, "-2147483648", String::toInt, String::toIntOrNull)

        assertEqualsOrFailsNullable(null, "2147483648", String::toInt, String::toIntOrNull)
        assertEqualsOrFailsNullable(null, "-2147483649", String::toInt, String::toIntOrNull)
        assertEqualsOrFailsNullable(null, "239239kotlin", String::toInt, String::toIntOrNull)
    }

    @test fun toLong() {
        assertEqualsOrFailsNullable(77.toLong(), "77", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(Long.MAX_VALUE, "+9223372036854775807", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(Long.MIN_VALUE, "-9223372036854775808", String::toLong, String::toLongOrNull)

        assertEqualsOrFailsNullable(null, "9223372036854775808", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(null, "-9223372036854775809", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(null, "922337 75809", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(null, "92233,75809", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(null, "92233`75809", String::toLong, String::toLongOrNull)
        assertEqualsOrFailsNullable(null, "-922337KOTLIN775809", String::toLong, String::toLongOrNull)
    }

    @test fun toFloat() {
        assertEqualsOrFailsNullable(77.0f, "77.0", String::toFloat, String::toFloatOrNull)
        assertEqualsOrFailsNullable(Float.NEGATIVE_INFINITY, "-1e39", String::toFloat, String::toFloatOrNull)
        assertEqualsOrFailsNullable(Float.POSITIVE_INFINITY, "1000000000000000000000000000000000000000",
                String::toFloat, String::toFloatOrNull)

        assertEqualsOrFailsNullable(null, "dark side", String::toFloat, String::toFloatOrNull)
    }

    @test fun toDouble() {
        assertEqualsOrFailsNullable(-77.0, "-77", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(77.0, "77.", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(77.0, "77.0", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(-1.77, "-1.77", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(0.77, "+.77", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(-77.0, "\t-77 \n", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(77.0, "7.7e1", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(77.0, "+770e-1", String::toDouble, String::toDoubleOrNull)

        assertEqualsOrFailsNullable(-Double.NaN, "-NaN", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(Double.POSITIVE_INFINITY, "+Infinity", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable((0x77 shl 1).toDouble(), "0x77p1", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(0x77.toDouble(), "0x.77P8", String::toDouble, String::toDoubleOrNull)

        assertEqualsOrFailsNullable(null, "7..7", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(null, "0x77e1", String::toDouble, String::toDoubleOrNull)
        assertEqualsOrFailsNullable(null, "007 not a number", String::toDouble, String::toDoubleOrNull)
    }
}

private inline fun <T: Any> assertEqualsOrFailsNullable(output: T?,
                                                        input: String,
                                                        crossinline converOrFail: (String) -> T,
                                                        crossinline convertOrNull: (String) -> T?) {
    if(output == null) {
        assertFails { converOrFail(input) }
        assertNull (convertOrNull(input) )
    } else {
        assertEquals(output, converOrFail(input))
        assertEquals(output, convertOrNull(input))
    }
}
