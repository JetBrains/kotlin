/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.*
import kotlin.test.*

class StringNumberConversionTest {

    @Test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
    }

    @Test fun toByte() {
        compareConversion({it.toByte()}, {it.toByteOrNull()}) {
            assertProduces("127", Byte.MAX_VALUE)
            assertProduces("+77", 77.toByte())
            assertProduces("-128", Byte.MIN_VALUE)
            assertFailsOrNull("128")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        compareConversionWithRadix(String::toByte, String::toByteOrNull) {
            assertProduces(16, "7a", 0x7a.toByte())
            assertProduces(16, "+7F", 127.toByte())
            assertProduces(16, "-80", (-128).toByte())
            assertFailsOrNull(2, "10000000")
            assertFailsOrNull(8, "")
            assertFailsOrNull(8, "   ")
        }
    }

    @Test fun toShort() {
        compareConversion({it.toShort()}, {it.toShortOrNull()}) {
            assertProduces("+77", 77.toShort())
            assertProduces("32767", Short.MAX_VALUE)
            assertProduces("-32768", Short.MIN_VALUE)
            assertFailsOrNull("+32768")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        compareConversionWithRadix(String::toShort, String::toShortOrNull) {
            assertProduces(16, "7FFF", 0x7FFF.toShort())
            assertProduces(16, "-8000", (-0x8000).toShort())
            assertFailsOrNull(5, "10000000")
            assertFailsOrNull(2, "")
            assertFailsOrNull(2, "   ")
        }
    }

    @Test fun toInt() {
        compareConversion({it.toInt()}, {it.toIntOrNull()}) {
            assertProduces("77", 77)
            assertProduces("+2147483647", Int.MAX_VALUE)
            assertProduces("-2147483648", Int.MIN_VALUE)

            assertFailsOrNull("2147483648")
            assertFailsOrNull("-2147483649")
            assertFailsOrNull("239239kotlin")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        compareConversionWithRadix(String::toInt, String::toIntOrNull) {
            assertProduces(10, "0", 0)
            assertProduces(10, "473", 473)
            assertProduces(10, "+42", 42)
            assertProduces(10, "-0", 0)
            assertProduces(10, "2147483647", 2147483647)
            assertProduces(10, "-2147483648", -2147483648)

            assertProduces(16, "-FF", -255)
            assertProduces(16, "-ff", -255)
            assertProduces(2, "1100110", 102)
            assertProduces(27, "Kona", 411787)

            assertFailsOrNull(10, "2147483648")
            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Kona")
            assertFailsOrNull(16, "")
            assertFailsOrNull(16, "  ")
        }

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toInt(radix = 1) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toIntOrNull(radix = 37) }
    }


    @Test fun toLong() {
        compareConversion({it.toLong()}, {it.toLongOrNull()}) {
            assertProduces("77", 77.toLong())
            assertProduces("+9223372036854775807", Long.MAX_VALUE)
            assertProduces("-9223372036854775808", Long.MIN_VALUE)

            assertFailsOrNull("9223372036854775808")
            assertFailsOrNull("-9223372036854775809")
            assertFailsOrNull("922337 75809")
            assertFailsOrNull("92233,75809")
            assertFailsOrNull("92233`75809")
            assertFailsOrNull("-922337KOTLIN775809")
            assertFailsOrNull("")
            assertFailsOrNull("  ")
        }

        compareConversionWithRadix(String::toLong, String::toLongOrNull) {
            assertProduces(10, "0", 0L)
            assertProduces(10, "473", 473L)
            assertProduces(10, "+42", 42L)
            assertProduces(10, "-0", 0L)

            assertProduces(16, "7F11223344556677", 0x7F11223344556677)
            assertProduces(16, "+7faabbccddeeff00", 0x7faabbccddeeff00)
            assertProduces(16, "-8000000000000000", Long.MIN_VALUE)
            assertProduces(2, "1100110", 102L)
            assertProduces(36, "Hazelnut", 1356099454469L)

            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Hazelnut")
            assertFailsOrNull(4, "")
            assertFailsOrNull(4, "  ")
        }

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toLong(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toLongOrNull(radix = 1) }
    }


    @Test fun toDouble() {
        compareConversion(String::toDouble, String::toDoubleOrNull, ::doubleTotalOrderEquals) {
            assertProduces("-77", -77.0)
            assertProduces("77.", 77.0)
            assertProduces("77.0", 77.0)
            assertProduces("-1.77", -1.77)
            assertProduces("+.77", 0.77)
            assertProduces("\t-77 \n", -77.0)
            assertProduces("7.7e1", 77.0)
            assertProduces("+770e-1", 77.0)

            assertProduces("-NaN", -Double.NaN)
            assertProduces("+Infinity", Double.POSITIVE_INFINITY)

            assertFailsOrNull("7..7")
            assertFailsOrNull("007 not a number")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }
    }



    @Test fun toUByte() {
        compareConversion({it.toUByte()}, {it.toUByteOrNull()}) {
            assertProduces("255", UByte.MAX_VALUE)
            assertProduces("+77", 77.toUByte())
            assertProduces("128", 128u.toUByte())
            assertFailsOrNull("-1")
            assertFailsOrNull("256")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        compareConversionWithRadix(String::toUByte, String::toUByteOrNull) {
            assertProduces(16, "7a", 0x7a.toUByte())
            assertProduces(16, "+8F", 0x8Fu.toUByte())
            assertProduces(16, "80", 128u.toUByte())
            assertProduces(16, "Ff", 255u.toUByte())
            assertFailsOrNull(2, "100000000")
            assertFailsOrNull(8, "")
            assertFailsOrNull(8, "   ")
        }
    }

    @Test fun toUShort() {
        compareConversion({it.toUShort()}, {it.toUShortOrNull()}) {
            assertProduces("+77", 77.toUShort())
            assertProduces("65535", UShort.MAX_VALUE)
            assertFailsOrNull("+65536")
            assertFailsOrNull("-32768")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        compareConversionWithRadix(String::toUShort, String::toUShortOrNull) {
            assertProduces(16, "+7FFF", 0x7FFF.toUShort())
            assertProduces(16, "FfFf", UShort.MAX_VALUE)
            assertFailsOrNull(16, "-8000")
            assertFailsOrNull(5, "10000000")
            assertFailsOrNull(2, "")
            assertFailsOrNull(2, "   ")
        }
    }

    @Test fun toUInt() {
        compareConversion({it.toUInt()}, {it.toUIntOrNull()}) {
            assertProduces("+77", 77u)
            assertProduces("4294967295", UInt.MAX_VALUE)

            assertFailsOrNull("-1")
            assertFailsOrNull("4294967296")
            assertFailsOrNull("42949672940")
            assertFailsOrNull("-2147483649")
            assertFailsOrNull("239239kotlin")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        @Suppress("SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED")
        compareConversionWithRadix(String::toUInt, String::toUIntOrNull) {
            assertProduces(10, "0", 0u)
            assertProduces(10, "473", 473u)
            assertProduces(10, "+42", 42u)
            assertProduces(10, "2147483647", 2147483647u)

            assertProduces(16, "FF", 255)
            assertProduces(16, "ffFFff01", 0u - 255u)
            assertProduces(2, "1100110", 102)
            assertProduces(27, "Kona", 411787)

            assertFailsOrNull(10, "-0")
            assertFailsOrNull(10, "42949672940")
            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Kona")
            assertFailsOrNull(16, "")
            assertFailsOrNull(16, "  ")
        }

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toUInt(radix = 1) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toUIntOrNull(radix = 37) }
    }


    @Test fun toULong() {
        compareConversion({it.toULong()}, {it.toULongOrNull()}) {
            assertProduces("77", 77uL)
            assertProduces("+18446744073709551615", ULong.MAX_VALUE)

            assertFailsOrNull("-1")
            assertFailsOrNull("18446744073709551616")
            assertFailsOrNull("922337 75809")
            assertFailsOrNull("92233,75809")
            assertFailsOrNull("92233`75809")
            assertFailsOrNull("-922337KOTLIN775809")
            assertFailsOrNull("")
            assertFailsOrNull("  ")
        }

        compareConversionWithRadix(String::toULong, String::toULongOrNull) {
            assertProduces(10, "0", 0uL)
            assertProduces(10, "473", 473uL)
            assertProduces(10, "+42", 42uL)

            assertProduces(16, "7F11223344556677", 0x7F11223344556677uL)
            assertProduces(16, "+7faabbccddeeff00", 0x7faabbccddeeff00uL)
            assertProduces(16, "8000000000000000", Long.MIN_VALUE.toULong())
            assertProduces(16, "FFFFffffFFFFffff", ULong.MAX_VALUE)
            assertProduces(2, "1100110", 102uL)
            assertProduces(36, "Hazelnut", 1356099454469uL)

            assertFailsOrNull(8, "-7")
            assertFailsOrNull(8, "99")
            assertFailsOrNull(10, "Hazelnut")
            assertFailsOrNull(4, "")
            assertFailsOrNull(4, "  ")
        }

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toULong(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toULongOrNull(radix = 1) }
    }



    @Test fun byteToStringWithRadix() {
        assertEquals("7a", 0x7a.toByte().toString(16))
        assertEquals("-80", Byte.MIN_VALUE.toString(radix = 16))
        assertEquals("3v", Byte.MAX_VALUE.toString(radix = 32))
        assertEquals("-40", Byte.MIN_VALUE.toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toByte().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toByte().toString(radix = 1) }
    }

    @Test fun shortToStringWithRadix() {
        assertEquals("7FFF", 0x7FFF.toShort().toString(radix = 16).toUpperCase())
        assertEquals("-8000", (-0x8000).toShort().toString(radix = 16))
        assertEquals("-sfs", (-29180).toShort().toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toShort().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toShort().toString(radix = 1) }
    }

    @Test fun intToStringWithRadix() {
        assertEquals("-ff", (-255).toString(radix = 16))
        assertEquals("1100110", 102.toString(radix = 2))
        assertEquals("kona", 411787.toString(radix = 27))
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toString(radix = 1) }

    }

    @Test fun longToStringWithRadix() {
        assertEquals("7f11223344556677", 0x7F11223344556677.toString(radix = 16))
        assertEquals("hazelnut", 1356099454469L.toString(radix = 36))
        assertEquals("-8000000000000000", Long.MIN_VALUE.toString(radix = 16))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37L.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1L.toString(radix = 1) }
    }

    @Test fun ubyteToStringWithRadix() {
        assertEquals("7a", 0x7a.toUByte().toString(16))
        assertEquals("80", Byte.MIN_VALUE.toUByte().toString(radix = 16))
        assertEquals("ff", UByte.MAX_VALUE.toString(radix = 16))

        assertEquals("40", Byte.MIN_VALUE.toUByte().toString(radix = 32))
        assertEquals("7v", UByte.MAX_VALUE.toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toUByte().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toUByte().toString(radix = 1) }
    }

    @Test fun ushortToStringWithRadix() {
        assertEquals("7FFF", 0x7FFF.toUShort().toString(radix = 16).toUpperCase())
        assertEquals("8000", 0x8000.toUShort().toString(radix = 16))
        assertEquals("ffff", UShort.MAX_VALUE.toString(radix = 16))

        assertEquals("1ekf", UShort.MAX_VALUE.toString(radix = 36))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toUShort().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toUShort().toString(radix = 1) }
    }

    @Test fun uintToStringWithRadix() {
        assertEquals("ffffff01", (-255).toUInt().toString(radix = 16))
        assertEquals("ffffffff", UInt.MAX_VALUE.toString(radix = 16))

        assertEquals("1100110", 102u.toString(radix = 2))
        assertEquals("kona", 411787u.toString(radix = 27))
        assertEquals("3vvvvvv", UInt.MAX_VALUE.toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37u.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1u.toString(radix = 1) }

    }

    @Test fun ulongToStringWithRadix() {
        assertEquals("7f11223344556677", 0x7F11223344556677u.toString(radix = 16))
        assertEquals("89aabbccddeeff11", 0x89AABBCCDDEEFF11u.toString(radix = 16))
        assertEquals("8000000000000000", Long.MIN_VALUE.toULong().toString(radix = 16))
        assertEquals("ffffffffffffffff", ULong.MAX_VALUE.toString(radix = 16))

        assertEquals("hazelnut", 1356099454469uL.toString(radix = 36))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37uL.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1uL.toString(radix = 1) }
    }
}

internal fun doubleTotalOrderEquals(a: Double?, b: Double?): Boolean = (a as Any?) == b

internal fun <T : Any> compareConversion(
    convertOrFail: (String) -> T,
    convertOrNull: (String) -> T?,
    equality: (T, T?) -> Boolean = { a, b -> a == b },
    assertions: ConversionContext<T>.() -> Unit
) {
    ConversionContext(convertOrFail, convertOrNull, equality).assertions()
}


internal fun <T : Any> compareConversionWithRadix(
    convertOrFail: String.(Int) -> T,
    convertOrNull: String.(Int) -> T?,
    assertions: ConversionWithRadixContext<T>.() -> Unit
) {
    ConversionWithRadixContext(convertOrFail, convertOrNull).assertions()
}


internal class ConversionContext<T : Any>(
    val convertOrFail: (String) -> T,
    val convertOrNull: (String) -> T?,
    val equality: (T, T?) -> Boolean
) {

    private fun assertEquals(expected: T, actual: T?, input: String, operation: String) {
        assertTrue(equality(expected, actual), "Expected $operation('$input') to produce $expected but was $actual")
    }

    fun assertProduces(input: String, output: T) {
        assertEquals(output, convertOrFail(input.removeLeadingPlusOnJava6()), input, "convertOrFail")
        assertEquals(output, convertOrNull(input), input, "convertOrNull")
    }

    fun assertFailsOrNull(input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\"") { convertOrFail(input) }
        assertNull(convertOrNull(input), message = "On input \"$input\"")
    }
}

internal class ConversionWithRadixContext<T : Any>(
    val convertOrFail: (String, Int) -> T,
    val convertOrNull: (String, Int) -> T?
) {
    fun assertProduces(radix: Int, input: String, output: T) {
        assertEquals(output, convertOrFail(input.removeLeadingPlusOnJava6(), radix))
        assertEquals(output, convertOrNull(input, radix))
    }

    fun assertFailsOrNull(radix: Int, input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\" with radix $radix",
                                               { convertOrFail(input, radix) })

        assertNull(convertOrNull(input, radix), message = "On input \"$input\" with radix $radix")
    }
}
