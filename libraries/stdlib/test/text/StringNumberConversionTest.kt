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

    @JvmVersion
    @Test fun toIntArabicDigits() {
        compareConversion({ it.toInt() }, { it.toIntOrNull() }) {
            assertProduces("٢٣١٩٦٠", 231960)
        }
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

    @JvmVersion
    @Test fun toLongArabicDigits() {
        compareConversion({ it.toLong() }, { it.toLongOrNull() }) {
            assertProduces("٢٣١٩٦٠٧٧٨٤٥٩", 231960778459)
        }
    }

    @kotlin.jvm.JvmVersion
    @Test fun toFloat() {
        compareConversion(String::toFloat, String::toFloatOrNull) {
            assertProduces("77.0", 77.0f)
            assertProduces("-1e39", Float.NEGATIVE_INFINITY)
            assertProduces("1000000000000000000000000000000000000000", Float.POSITIVE_INFINITY)
            assertFailsOrNull("dark side")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }
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

    @kotlin.jvm.JvmVersion
    @Test fun toHexDouble() {
        compareConversion(String::toDouble, String::toDoubleOrNull, ::doubleTotalOrderEquals) {
            assertProduces("0x77p1", (0x77 shl 1).toDouble())
            assertProduces("0x.77P8", 0x77.toDouble())

            assertFailsOrNull("0x77e1")
        }
    }

    @kotlin.jvm.JvmVersion
    @Test fun byteToStringWithRadix() {
        assertEquals("7a", 0x7a.toByte().toString(16))
        assertEquals("-80", Byte.MIN_VALUE.toString(radix = 16))
        assertEquals("3v", Byte.MAX_VALUE.toString(radix = 32))
        assertEquals("-40", Byte.MIN_VALUE.toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toByte().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toByte().toString(radix = 1) }
    }

    @kotlin.jvm.JvmVersion
    @Test fun shortToStringWithRadix() {
        assertEquals("7FFF", 0x7FFF.toShort().toString(radix = 16).toUpperCase())
        assertEquals("-8000", (-0x8000).toShort().toString(radix = 16))
        assertEquals("-sfs", (-29180).toShort().toString(radix = 32))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toShort().toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toShort().toString(radix = 1) }
    }

    @kotlin.jvm.JvmVersion
    @Test fun intToStringWithRadix() {
        assertEquals("-ff", (-255).toString(radix = 16))
        assertEquals("1100110", 102.toString(radix = 2))
        assertEquals("kona", 411787.toString(radix = 27))
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toString(radix = 1) }

    }

    @kotlin.jvm.JvmVersion
    @Test fun longToStringWithRadix() {
        assertEquals("7f11223344556677", 0x7F11223344556677.toString(radix = 16))
        assertEquals("hazelnut", 1356099454469L.toString(radix = 36))
        assertEquals("-8000000000000000", Long.MIN_VALUE.toString(radix = 16))

        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37L.toString(radix = 37) }
        assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1L.toString(radix = 1) }
    }

    @kotlin.jvm.JvmVersion
    @Test fun toBigInteger() {
        compareConversion(String::toBigInteger, String::toBigIntegerOrNull) {
            assertProduces("0", java.math.BigInteger.ZERO)
            assertProduces("1", java.math.BigInteger.ONE)
            assertProduces("-1", java.math.BigInteger.ONE.negate())
            assertProduces("100000000000000000000", java.math.BigInteger("100000000000000000000"))
            assertFailsOrNull("")
            assertFailsOrNull("-")
            assertFailsOrNull("a")
            assertFailsOrNull("-x")
            assertFailsOrNull("1000 000")
        }

        compareConversionWithRadix(String::toBigInteger, String::toBigIntegerOrNull) {
            assertProduces(16, "ABCDEF90ABCDEF9012345678", java.math.BigInteger("ABCDEF90ABCDEF9012345678", 16))
            assertProduces(36, "HazelnutHazelnut", java.math.BigInteger.valueOf(1356099454469L).let { it.multiply(java.math.BigInteger.valueOf(36).pow(8)).add(it) })

            assertFailsOrNull(16, "EFG")
            assertFailsOrNull(10, "-1A")
            assertFailsOrNull(2, "-")
            assertFailsOrNull(3, "")

            assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toBigInteger(radix = 37) }
            assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toBigIntegerOrNull(radix = 1) }
        }
    }

    @kotlin.jvm.JvmVersion
    @Test fun toBigDecimal() {
        fun bd(value: String) = java.math.BigDecimal(value)
        compareConversion(String::toBigDecimal, String::toBigDecimalOrNull) {

            assertProduces("-77", bd("-77"))
            assertProduces("-77.0", bd("-77.0"))
            assertProduces("77.", bd("77"))
            assertProduces("123456789012345678901234567890.123456789", bd("123456789012345678901234567890.123456789"))
            assertProduces("-1.77", bd("-1.77"))
            assertProduces("+.77", bd("0.77"))
            assertProduces("7.7e1", bd("77"))
            assertProduces("+770e-1", bd("77.0"))

            assertFailsOrNull("7..7")
            assertFailsOrNull("\t-77 \n")
            assertFailsOrNull("007 not a number")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        var mc = java.math.MathContext(3, java.math.RoundingMode.UP)
        compareConversion( { it.toBigDecimal(mc) }, { it.toBigDecimalOrNull(mc) }) {
            assertProduces("1.991", bd("2.00"))

            mc = java.math.MathContext(1, java.math.RoundingMode.UNNECESSARY)

            assertFailsWith<ArithmeticException> { "2.991".toBigDecimal(mc) }
            assertFailsWith<ArithmeticException> { "2.991".toBigDecimalOrNull(mc) }
        }
    }
}


private fun <T : Any> compareConversion(convertOrFail: (String) -> T,
                                        convertOrNull: (String) -> T?,
                                        equality: (T, T?) -> Boolean = { a, b -> a == b },
                                        assertions: ConversionContext<T>.() -> Unit) {
    ConversionContext(convertOrFail, convertOrNull, equality).assertions()
}


private fun <T : Any> compareConversionWithRadix(convertOrFail: String.(Int) -> T,
                                                 convertOrNull: String.(Int) -> T?,
                                                 assertions: ConversionWithRadixContext<T>.() -> Unit) {
    ConversionWithRadixContext(convertOrFail, convertOrNull).assertions()
}


private class ConversionContext<T: Any>(val convertOrFail: (String) -> T,
                                        val convertOrNull: (String) -> T?,
                                        val equality: (T, T?) -> Boolean) {

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

private class ConversionWithRadixContext<T: Any>(val convertOrFail: (String, Int) -> T,
                                                 val convertOrNull: (String, Int) -> T?) {
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
