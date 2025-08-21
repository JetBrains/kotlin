/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.TestPlatform
import test.testExceptOn
import test.testOn
import kotlin.math.pow
import kotlin.math.round
import kotlin.test.*

private fun testOnNativeAndJvm(action: () -> Unit) {
    testOn({ p -> p == TestPlatform.Jvm || p == TestPlatform.Native }, action)
}

class StringNumberConversionTest {

    @Test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
        assertEquals(false, (null as String?).toBoolean())
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
            assertProduces("0", 0.0)
            assertProduces("-0", -0.0)
            assertProduces("+0", 0.0)
            assertProduces("-77", -77.0)
            assertProduces("77.", 77.0)
            assertProduces("77.0", 77.0)
            assertProduces("-1.77", -1.77)
            assertProduces("+.77", 0.77)
            assertProduces("\t-77 \n", -77.0)
            assertProduces("7.7e1", 77.0)
            assertProduces("+770e-1", 77.0)

            assertProduces("NaN", Double.NaN)
            assertProduces("Infinity", Double.POSITIVE_INFINITY)
            assertProduces("+Infinity", Double.POSITIVE_INFINITY)
            assertProduces("-NaN", -Double.NaN)
            assertProduces("-Infinity", Double.NEGATIVE_INFINITY)

            assertFailsOrNull("7..7")
            assertFailsOrNull("007 not a number")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
            assertFailsOrNull("2.-")
            assertFailsOrNull("0e12notvalid3")
            assertFailsOrNull("Inf1N1tY")
            assertFailsOrNull("0xNaN")
            assertFailsOrNull("-")
            assertFailsOrNull("+")

            testExceptOn(TestPlatform.Js) {
                assertFailsOrNull("naN")
                assertFailsOrNull("-nAn")
                assertFailsOrNull("-infinity")
            }

            testExceptOn(TestPlatform.Native) {
                assertProduces("123e2147483647", Double.POSITIVE_INFINITY)
                assertProduces("-123e2147483647", Double.NEGATIVE_INFINITY)

                assertProduces("123e20000000000", Double.POSITIVE_INFINITY)
                assertProduces("-123e20000000000", Double.NEGATIVE_INFINITY)

                assertProduces("123e30000000000", Double.POSITIVE_INFINITY)
                assertProduces("-123e30000000000", Double.NEGATIVE_INFINITY)
            }

            assertProduces("123e-2147483647", 0.0)
            assertProduces("-123e-2147483647", -0.0)

            assertProduces("123e-20000000000", 0.0)
            assertProduces("-123e-20000000000", -0.0)

            assertProduces("123e-30000000000", 0.0)
            assertProduces("-123e-30000000000", -0.0)

            assertProduces("0e9999999999999", 0.0)
            assertProduces("-0e9999999999999", -0.0)

            assertFailsOrNull(".")

            // Test invalid hex notations
            // 1. No integer nor fractional part
            assertFailsOrNull("0x.")
            assertFailsOrNull("0x.p")
            assertFailsOrNull("0x.p1")
            // 2. Missing exponent
            // TODO: run on all platforms after fixing KT-72509
            testExceptOn(TestPlatform.Js) {
                assertFailsOrNull("0x11ff33")
                assertFailsOrNull("0x1f")
                assertFailsOrNull("0x2D")
            }
            assertFailsOrNull("0x.11ff33")
            // 3. Invalid exponent
            assertFailsOrNull("0x11ff33.22ee44")
            assertFailsOrNull("0x11ff33P")
            assertFailsOrNull("0x.11ff33p")
            assertFailsOrNull("0x11ff33.22ee44P")

            testOnNativeAndJvm {
                // Valid hex numbers
                // 1. No fractional part
                assertProduces("0x11ff33p2", 4_717_772.0)
                assertProduces("0x11ff33P2", 4_717_772.0)
                assertProduces("0x11ff33.P2", 4_717_772.0)
                // 2. No integer part
                assertProduces("0x.11ff33P2", 0.2812011241912842)
                // 3. Both integer and fractional parts
                assertProduces("0x11.ff33P2", 71.98748779296875)
                // 4. Negative exponent
                assertProduces("0x11.ff33p-2", 4.499217987060547)
                // 5. Negative mantissa
                assertProduces("-0x11.ff33p-2", -4.499217987060547)
            }

            // Invalid exponent notations
            assertFailsOrNull("123z4")
            assertFailsOrNull("123e4t")
            assertFailsOrNull(".123z4")
            assertFailsOrNull(".123e4t")
            assertFailsOrNull("2.123z4")
            assertFailsOrNull("2.123e4t")
            assertFailsOrNull("123e+")
            assertFailsOrNull("123e-")

            testOnNativeAndJvm {
                // Valid float suffix
                assertProduces("1f", 1.0)
                assertProduces("1.5f", 1.5)
                assertProduces("1234f", 1234.0)
                assertProduces("123e4f", 1230_000.0)
                assertProduces("1F", 1.0)
                assertProduces("1.5F", 1.5)
                assertProduces("1234F", 1234.0)
                assertProduces("123e4F", 1230_000.0)
                assertProduces("0x34.0P0F", 52.0)
                assertProduces("0x34.P0f", 52.0)
                assertProduces("0x.340P6F", 13.0)

                // Valid double suffix
                assertProduces("1d", 1.0)
                assertProduces("1.5d", 1.5)
                assertProduces("1234d", 1234.0)
                assertProduces("123e4d", 1230_000.0)
                assertProduces("1D", 1.0)
                assertProduces("1.5D", 1.5)
                assertProduces("1234D", 1234.0)
                assertProduces("123e4D", 1230_000.0)
                assertProduces("0x34.0P0D", 52.0)
                assertProduces("0x34.P0d", 52.0)
                assertProduces("0x.340P6D", 13.0)

                // Invalid hexadecimal exponent
                assertFailsOrNull("0x23P+")
                assertFailsOrNull("0x23p-")
            }

            // Invalid float suffix
            assertFailsOrNull("1g")
            assertFailsOrNull("1.5g")
            assertFailsOrNull("1234g")
            assertFailsOrNull("123e4g")

            // Test exponent signs
            assertProduces("1e+1", 10.0)
            assertProduces("1e-1", 0.1)

            // Invalid exponents
            assertFailsOrNull("1ez1")
            assertFailsOrNull("1e+z1")
            assertFailsOrNull("1e-z1")

            testOnNativeAndJvm {
                // Test special whitespace characters as trailing or leading characters
                for (i in 0..0x20) {
                    assertProduces("${i.toChar()}77", 77.0)
                    assertProduces("77${i.toChar()}", 77.0)
                }
            }
        }
    }

    @Test fun toFloat() {
        compareConversion(String::toFloat, String::toFloatOrNull, ::floatTotalOrderEquals) {
            assertProduces("0", 0f)
            assertProduces("-0", -0f)
            assertProduces("+0", 0f)
            assertProduces("-77", -77.0f)
            assertProduces("77.", 77.0f)
            assertProduces("77.0", 77.0f)
            assertProduces("-1.77", -1.77f)
            assertProduces("+.77", 0.77f)
            assertProduces("\t-77 \n", -77.0f)
            assertProduces("7.7e1", 77.0f)
            assertProduces("+770e-1", 77.0f)

            assertProduces("NaN", Float.NaN)
            assertProduces("Infinity", Float.POSITIVE_INFINITY)
            assertProduces("+Infinity", Float.POSITIVE_INFINITY)
            assertProduces("-NaN", -Float.NaN)
            assertProduces("-Infinity", Float.NEGATIVE_INFINITY)

            assertFailsOrNull("7..7")
            assertFailsOrNull("007 not a number")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
            assertFailsOrNull("2.-")
            assertFailsOrNull("0e12notvalid3")
            assertFailsOrNull("Inf1N1tY")
            assertFailsOrNull("0xNaN")
            assertFailsOrNull("-")
            assertFailsOrNull("+")

            testExceptOn(TestPlatform.Js) {
                assertFailsOrNull("naN")
                assertFailsOrNull("-nAn")
                assertFailsOrNull("-infinity")
            }

            testExceptOn(TestPlatform.Native) {
                assertProduces("123e2147483647", Float.POSITIVE_INFINITY)
                assertProduces("-123e2147483647", Float.NEGATIVE_INFINITY)

                assertProduces("123e20000000000", Float.POSITIVE_INFINITY)
                assertProduces("-123e20000000000", Float.NEGATIVE_INFINITY)

                assertProduces("123e30000000000", Float.POSITIVE_INFINITY)
                assertProduces("-123e30000000000", Float.NEGATIVE_INFINITY)
            }

            assertProduces("123e-2147483647", 0.0f)
            assertProduces("-123e-2147483647", -0.0f)

            assertProduces("123e-20000000000", 0.0f)
            assertProduces("-123e-20000000000", -0.0f)

            assertProduces("123e-30000000000", 0.0f)
            assertProduces("-123e-30000000000", -0.0f)

            assertProduces("0e9999999999999", 0.0f)
            assertProduces("-0e9999999999999", -0.0f)

            assertFailsOrNull(".")

            // Test invalid hex notations
            // 1. No integer nor fractional part
            assertFailsOrNull("0x.")
            assertFailsOrNull("0x.p")
            assertFailsOrNull("0x.p1")
            // 2. Missing exponent
            // TODO: run on all platforms after fixing KT-72509
            testExceptOn(TestPlatform.Js) {
                assertFailsOrNull("0x11ff33")
                assertFailsOrNull("0x1f")
                assertFailsOrNull("0x2D")
            }
            assertFailsOrNull("0x.11ff33")
            // 3. Invalid exponent
            assertFailsOrNull("0x11ff33.22ee44")
            assertFailsOrNull("0x11ff33P")
            assertFailsOrNull("0x.11ff33p")
            assertFailsOrNull("0x11ff33.22ee44P")

            testOnNativeAndJvm {
                // Valid hex numbers
                // 1. No fractional part
                assertProduces("0x11ff33p2", 4_717_772.0f)
                assertProduces("0x11ff33P2", 4_717_772.0f)
                assertProduces("0x11ff33.P2", 4_717_772.0f)
                // 2. No integer part
                assertProduces("0x.11ff33P2", 0.28120112f)
                // 3. Both integer and fractional parts
                assertProduces("0x11.ff33P2", 71.98749f)
                // 4. Negative exponent
                assertProduces("0x11.ff33p-2", 4.499218f)
                // 5. Negative mantissa
                assertProduces("-0x11.ff33p-2", -4.499218f)
            }

            // Invalid exponent notations
            assertFailsOrNull("123z4")
            assertFailsOrNull("123e4t")
            assertFailsOrNull(".123z4")
            assertFailsOrNull(".123e4t")
            assertFailsOrNull("2.123z4")
            assertFailsOrNull("2.123e4t")

            testOnNativeAndJvm {
                // Valid float suffix
                assertProduces("1f", 1.0f)
                assertProduces("1.5f", 1.5f)
                assertProduces("1234f", 1234.0f)
                assertProduces("123e4f", 1230_000.0f)
                assertProduces("1F", 1.0f)
                assertProduces("1.5F", 1.5f)
                assertProduces("1234F", 1234.0f)
                assertProduces("123e4F", 1230_000.0f)
                assertProduces("0x34.0P0F", 52f)
                assertProduces("0x34.P0f", 52f)
                assertProduces("0x.340P6F", 13f)

                // Valid double suffix
                assertProduces("1d", 1.0f)
                assertProduces("1.5d", 1.5f)
                assertProduces("1234d", 1234.0f)
                assertProduces("123e4d", 1230_000.0f)
                assertProduces("1D", 1.0f)
                assertProduces("1.5D", 1.5f)
                assertProduces("1234D", 1234.0f)
                assertProduces("123e4D", 1230_000.0f)
                assertProduces("0x34.0P0D", 52f)
                assertProduces("0x34.P0d", 52f)
                assertProduces("0x.340P6D", 13f)

                // Invalid hexadecimal exponent
                assertFailsOrNull("0x23P+")
                assertFailsOrNull("0x23p-")
            }

            // Invalid float suffix
            assertFailsOrNull("1g")
            assertFailsOrNull("1.5g")
            assertFailsOrNull("1234g")
            assertFailsOrNull("123e4g")

            // Test exponent signs
            assertProduces("1e+1", 10.0f)
            assertProduces("1e-1", 0.1f)

            // Invalid exponents
            assertFailsOrNull("1ez1")
            assertFailsOrNull("1e+z1")
            assertFailsOrNull("1e-z1")

            testOnNativeAndJvm {
                // Test special whitespace characters as trailing or leading characters
                for (i in 0..0x20) {
                    assertProduces("${i.toChar()}77", 77.0f)
                    assertProduces("77${i.toChar()}", 77.0f)
                }
            }
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

        compareConversionWithRadix(String::toUInt, String::toUIntOrNull) {
            assertProduces(10, "0", 0u)
            assertProduces(10, "473", 473u)
            assertProduces(10, "+42", 42u)
            assertProduces(10, "2147483647", 2147483647u)

            assertProduces(16, "FF", 255u)
            assertProduces(16, "ffFFff01", 0u - 255u)
            assertProduces(2, "1100110", 102u)
            assertProduces(27, "Kona", 411787u)

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
        assertEquals("7FFF", 0x7FFF.toShort().toString(radix = 16).uppercase())
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

        val values = listOf(Int.MAX_VALUE.toLong(), Int.MIN_VALUE.toLong(), Int.MAX_VALUE + 2L, Long.MAX_VALUE, Long.MIN_VALUE)
        val expected = listOf(
            2 to listOf("1111111111111111111111111111111", "-10000000000000000000000000000000", "10000000000000000000000000000001", "111111111111111111111111111111111111111111111111111111111111111", "-1000000000000000000000000000000000000000000000000000000000000000"),
            3 to listOf("12112122212110202101", "-12112122212110202102", "12112122212110202110", "2021110011022210012102010021220101220221", "-2021110011022210012102010021220101220222"),
            4 to listOf("1333333333333333", "-2000000000000000", "2000000000000001", "13333333333333333333333333333333", "-20000000000000000000000000000000"),
            5 to listOf("13344223434042", "-13344223434043", "13344223434044", "1104332401304422434310311212", "-1104332401304422434310311213"),
            6 to listOf("553032005531", "-553032005532", "553032005533", "1540241003031030222122211", "-1540241003031030222122212"),
            7 to listOf("104134211161", "-104134211162", "104134211163", "22341010611245052052300", "-22341010611245052052301"),
            8 to listOf("17777777777", "-20000000000", "20000000001", "777777777777777777777", "-1000000000000000000000"),
            9 to listOf("5478773671", "-5478773672", "5478773673", "67404283172107811827", "-67404283172107811828"),
            10 to listOf("2147483647", "-2147483648", "2147483649", "9223372036854775807", "-9223372036854775808"),
            11 to listOf("a02220281", "-a02220282", "a02220283", "1728002635214590697", "-1728002635214590698"),
            12 to listOf("4bb2308a7", "-4bb2308a8", "4bb2308a9", "41a792678515120367", "-41a792678515120368"),
            13 to listOf("282ba4aaa", "-282ba4aab", "282ba4aac", "10b269549075433c37", "-10b269549075433c38"),
            14 to listOf("1652ca931", "-1652ca932", "1652ca933", "4340724c6c71dc7a7", "-4340724c6c71dc7a8"),
            15 to listOf("c87e66b7", "-c87e66b8", "c87e66b9", "160e2ad3246366807", "-160e2ad3246366808"),
            16 to listOf("7fffffff", "-80000000", "80000001", "7fffffffffffffff", "-8000000000000000"),
            17 to listOf("53g7f548", "-53g7f549", "53g7f54a", "33d3d8307b214008", "-33d3d8307b214009"),
            18 to listOf("3928g3h1", "-3928g3h2", "3928g3h3", "16agh595df825fa7", "-16agh595df825fa8"),
            19 to listOf("27c57h32", "-27c57h33", "27c57h34", "ba643dci0ffeehh", "-ba643dci0ffeehi"),
            20 to listOf("1db1f927", "-1db1f928", "1db1f929", "5cbfjia3fh26ja7", "-5cbfjia3fh26ja8"),
            21 to listOf("140h2d91", "-140h2d92", "140h2d93", "2heiciiie82dh97", "-2heiciiie82dh98"),
            22 to listOf("ikf5bf1", "-ikf5bf2", "ikf5bf3", "1adaibb21dckfa7", "-1adaibb21dckfa8"),
            23 to listOf("ebelf95", "-ebelf96", "ebelf97", "i6k448cf4192c2", "-i6k448cf4192c3"),
            24 to listOf("b5gge57", "-b5gge58", "b5gge59", "acd772jnc9l0l7", "-acd772jnc9l0l8"),
            25 to listOf("8jmdnkm", "-8jmdnkn", "8jmdnko", "64ie1focnn5g77", "-64ie1focnn5g78"),
            26 to listOf("6oj8ion", "-6oj8ioo", "6oj8iop", "3igoecjbmca687", "-3igoecjbmca688"),
            27 to listOf("5ehncka", "-5ehnckb", "5ehnckc", "27c48l5b37oaop", "-27c48l5b37oaoq"),
            28 to listOf("4clm98f", "-4clm98g", "4clm98h", "1bk39f3ah3dmq7", "-1bk39f3ah3dmq8"),
            29 to listOf("3hk7987", "-3hk7988", "3hk7989", "q1se8f0m04isb", "-q1se8f0m04isc"),
            30 to listOf("2sb6cs7", "-2sb6cs8", "2sb6cs9", "hajppbc1fc207", "-hajppbc1fc208"),
            31 to listOf("2d09uc1", "-2d09uc2", "2d09uc3", "bm03i95hia437", "-bm03i95hia438"),
            32 to listOf("1vvvvvv", "-2000000", "2000001", "7vvvvvvvvvvvv", "-8000000000000"),
            33 to listOf("1lsqtl1", "-1lsqtl2", "1lsqtl3", "5hg4ck9jd4u37", "-5hg4ck9jd4u38"),
            34 to listOf("1d8xqrp", "-1d8xqrq", "1d8xqrr", "3tdtk1v8j6tpp", "-3tdtk1v8j6tpq"),
            35 to listOf("15v22um", "-15v22un", "15v22uo", "2pijmikexrxp7", "-2pijmikexrxp8"),
            36 to listOf("zik0zj", "-zik0zk", "zik0zl", "1y2p0ij32e8e7", "-1y2p0ij32e8e8"),
        )
        for ((base, expectedValues) in expected) {
            for ((index, value) in values.withIndex()) {
                assertEquals(expectedValues[index], value.toString(base), "$value in base $base")
            }
        }

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
        assertEquals("7FFF", 0x7FFF.toUShort().toString(radix = 16).uppercase())
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

internal fun floatTotalOrderEquals(a: Float?, b: Float?): Boolean = (a as Any?) == b

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
        assertEquals(output, convertOrFail(input), input, "convertOrFail")
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
        assertEquals(output, convertOrFail(input, radix))
        assertEquals(output, convertOrNull(input, radix))
    }

    fun assertFailsOrNull(radix: Int, input: String) {
        assertFailsWith<NumberFormatException>("Expected to fail on input \"$input\" with radix $radix",
                                               { convertOrFail(input, radix) })

        assertNull(convertOrNull(input, radix), message = "On input \"$input\" with radix $radix")
    }
}

class FpNumberToStringTest {
    @Test fun doubleTest() {
        assertEquals((0.5).toString(), "0.5")
        assertEquals((-0.5).toString(), "-0.5")
        testExceptOn(TestPlatform.Js) {
            assertEquals((0.0).toString(), "0.0")
            assertEquals((-0.0).toString(), "-0.0")
        }
        assertEquals(Double.NaN.toString(), "NaN")
        assertEquals(Double.POSITIVE_INFINITY.toString(), "Infinity")
        assertEquals(Double.NEGATIVE_INFINITY.toString(), "-Infinity")
    }

    @Test fun floatTest() {
        assertEquals((0.5f).toString(), "0.5")
        assertEquals((-0.5f).toString(), "-0.5")
        testExceptOn(TestPlatform.Js) {
            assertEquals((0.0f).toString(), "0.0")
            assertEquals((-0.0f).toString(), "-0.0")
        }
        assertEquals(Float.NaN.toString(), "NaN")
        assertEquals(Float.POSITIVE_INFINITY.toString(), "Infinity")
        assertEquals(Float.NEGATIVE_INFINITY.toString(), "-Infinity")
    }

    @Test
    fun kt74441() {
        val a = identity(1e-45)
        // Exact string is platform-dependent: "1.0E-45" or "1e-45"
        assertEquals(a, a.toString().toDouble())
    }

    @Test
    fun kt69107() {
        val a = identity(0.30000001192092F)
        assertEquals("0.3", (round(a * 10f) / 10f).toString())
        assertEquals("0.3", (0.3f as Any).toString())
    }

    @Test
    fun kt68948() {
        val inlineTemplate = "${identity(3.4f)}"
        assertEquals("3.4", inlineTemplate)

        val floatVariable = identity(3.4f)
        val variableTemplate = "$floatVariable"
        assertEquals("3.4", variableTemplate)
    }

    @Test
    fun kt59118() {
        fun Float.roundDecimalPlaces(places: Int): Float {
            if (places < 0) return this
            val placesFactor: Float = 10f.pow(places.toFloat())
            return round(this * placesFactor) / placesFactor
        }

        assertEquals("0.031398475", "" + identity(0.031398475f))
        assertEquals("0.03", "" + 0.031398475f.roundDecimalPlaces(2))
    }

    // Prevent compiler constant folding
    private fun <T> identity(x: T): T = x
}
