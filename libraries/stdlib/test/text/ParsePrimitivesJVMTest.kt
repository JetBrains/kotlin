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
        CompareBehaviourContext(String::toByte, String::toByteOrNull).apply {

            assertProduce(77.toByte(), "+77")
            assertProduce(Byte.MIN_VALUE, "-128")
            assertProduce(null, "128")
        }
    }

    @test fun toShort() {
        CompareBehaviourContext(String::toShort, String::toShortOrNull).apply {

            assertProduce(77.toShort(), "77")
            assertProduce(Short.MIN_VALUE, "-32768")
            assertProduce(null, "+32768")
        }
    }

    @test fun toInt() {
        CompareBehaviourContext(String::toInt, String::toIntOrNull).apply {

            assertProduce(77, "77")
            assertProduce(Int.MAX_VALUE, "+2147483647")
            assertProduce(Int.MIN_VALUE, "-2147483648")

            assertProduce(null, "2147483648")
            assertProduce(null, "-2147483649")
            assertProduce(null, "239239kotlin")
        }
    }

    @test fun toLong() {
        CompareBehaviourContext(String::toLong, String::toLongOrNull).apply {

            assertProduce(77.toLong(), "77")
            assertProduce(Long.MAX_VALUE, "+9223372036854775807")
            assertProduce(Long.MIN_VALUE, "-9223372036854775808")

            assertProduce(null, "9223372036854775808")
            assertProduce(null, "-9223372036854775809")
            assertProduce(null, "922337 75809")
            assertProduce(null, "92233,75809")
            assertProduce(null, "92233`75809")
            assertProduce(null, "-922337KOTLIN775809")
        }
    }

    @test fun toFloat() {
        CompareBehaviourContext(String::toFloat, String::toFloatOrNull).apply {

            assertProduce(77.0f, "77.0")
            assertProduce(Float.NEGATIVE_INFINITY, "-1e39")
            assertProduce(Float.POSITIVE_INFINITY, "1000000000000000000000000000000000000000")
            assertProduce(null, "dark side")
        }
    }

    @test fun toDouble() {
        CompareBehaviourContext(String::toDouble, String::toDoubleOrNull).apply {

            assertProduce(-77.0, "-77")
            assertProduce(77.0, "77.")
            assertProduce(77.0, "77.0")
            assertProduce(-1.77, "-1.77")
            assertProduce(0.77, "+.77")
            assertProduce(-77.0, "\t-77 \n")
            assertProduce(77.0, "7.7e1")
            assertProduce(77.0, "+770e-1")

            assertProduce(-Double.NaN, "-NaN")
            assertProduce(Double.POSITIVE_INFINITY, "+Infinity")
            assertProduce((0x77 shl 1).toDouble(), "0x77p1")
            assertProduce(0x77.toDouble(), "0x.77P8")

            assertProduce(null, "7..7")
            assertProduce(null, "0x77e1")
            assertProduce(null, "007 not a number")
        }
    }
}


private class CompareBehaviourContext<T: Any>(val convertOrFail: (String) -> T,
                                              val convertOrNull: (String) -> T?) {
    fun assertProduce(output: T?, input: String) {
        if(output == null) {
            assertFails { convertOrFail(input) }
            assertNull (convertOrNull(input) )
        } else {
            assertEquals(output, convertOrFail(input))
            assertEquals(output, convertOrNull(input))
        }
    }
}
