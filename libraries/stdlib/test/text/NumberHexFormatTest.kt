/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NumberHexFormatTest {

    companion object {
        private const val longHex3a: String = "000000000000003a" // 16 hex digits
        private const val longHex3A: String = "000000000000003A" // 16 hex digits
    }

    private fun testFormatAndParse(number: Long, digits: String, format: HexFormat) {
        testFormat(number, digits, format)
        testParse(digits, number, format)
    }

    private fun testFormat(number: Long, digits: String, format: HexFormat) {
        fun String.withPrefixAndSuffix(): String = format.number.prefix + this + format.number.suffix

        assertEquals(digits.takeLast(2).withPrefixAndSuffix(), number.toByte().toHexString(format))
        assertEquals(digits.takeLast(2).withPrefixAndSuffix(), number.toUByte().toHexString(format))
        assertEquals(digits.takeLast(4).withPrefixAndSuffix(), number.toShort().toHexString(format))
        assertEquals(digits.takeLast(4).withPrefixAndSuffix(), number.toUShort().toHexString(format))
        assertEquals(digits.takeLast(8).withPrefixAndSuffix(), number.toInt().toHexString(format))
        assertEquals(digits.takeLast(8).withPrefixAndSuffix(), number.toUInt().toHexString(format))
        assertEquals(digits.withPrefixAndSuffix(), number.toHexString(format))
        assertEquals(digits.withPrefixAndSuffix(), number.toULong().toHexString(format))
    }

    private fun testParse(digits: String, number: Long, format: HexFormat) {
        fun String.withPrefixAndSuffix(): String = format.number.prefix + this + format.number.suffix

        assertEquals(number.toByte(), digits.takeLast(2).withPrefixAndSuffix().hexToByte(format))
        assertEquals(number.toUByte(), digits.takeLast(2).withPrefixAndSuffix().hexToUByte(format))
        assertEquals(number.toShort(), digits.takeLast(4).withPrefixAndSuffix().hexToShort(format))
        assertEquals(number.toUShort(), digits.takeLast(4).withPrefixAndSuffix().hexToUShort(format))
        assertEquals(number.toInt(), digits.takeLast(8).withPrefixAndSuffix().hexToInt(format))
        assertEquals(number.toUInt(), digits.takeLast(8).withPrefixAndSuffix().hexToUInt(format))
        assertEquals(number, digits.withPrefixAndSuffix().hexToLong(format))
        assertEquals(number.toULong(), digits.withPrefixAndSuffix().hexToULong(format))
    }

    @Test
    fun ignoreBytesFormat() {
        val format = HexFormat {
            bytes {
                bytesPerLine = 10
                bytesPerGroup = 4
                groupSeparator = "---"
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
        }

        testFormatAndParse(58, longHex3a, format)
    }

    @Test
    fun upperCase() {
        testFormatAndParse(58, longHex3A, HexFormat { upperCase = true })
        testFormatAndParse(58, longHex3A, HexFormat.UpperCase)
    }

    @Test
    fun lowerCase() {
        testFormatAndParse(58, longHex3a, HexFormat { upperCase = false })
    }

    @Test
    fun defaultCase() {
        testFormatAndParse(58, longHex3a, HexFormat.Default)
    }

    @Test
    fun formatAndParseZero() {
        testFormatAndParse(0, "0".repeat(16), HexFormat.Default)
        testParse("0", 0, HexFormat.Default)
    }

    @Test
    fun formatAndParseMax() {
        testFormatAndParse(Long.MAX_VALUE, "7" + "f".repeat(15), HexFormat.Default)
        testFormatAndParse(-1, "f".repeat(16), HexFormat.Default)
    }

    @Test
    fun formatAndParsePrefixSuffix() {
        testFormatAndParse(58, "000000000000003a", HexFormat { number.prefix = "0x" })
        testFormatAndParse(58, "000000000000003a", HexFormat { number.suffix = "h" })
        testFormatAndParse(58, "000000000000003a", HexFormat { number.prefix = "0x"; number.suffix = "h" })
    }

    @Test
    fun removeLeadingZeros() {
        val format = HexFormat { number.removeLeadingZeros = true }
        testFormatAndParse(58, "3a", format)
        testFormatAndParse(0, "0", format)
    }

    @Test
    fun formatMinLength() {
        // minLength < typeHexLength
        assertEquals(longHex3a, 58L.toHexString(HexFormat { number.minLength = 15 }))
        assertEquals(longHex3a.takeLast(15), 58L.toHexString(HexFormat { number { minLength = 15; removeLeadingZeros = true } }))
        assertEquals("3a", 58L.toHexString(HexFormat { number { minLength = 1; removeLeadingZeros = true } }))

        // minLength == typeHexLength
        assertEquals("003a", 58.toShort().toHexString(HexFormat { number.minLength = 4 }))
        assertEquals("003a", 58.toShort().toHexString(HexFormat { number { minLength = 4; removeLeadingZeros = true } }))

        // minLength > typeHexLength
        assertEquals(longHex3a, 58.toHexString(HexFormat { number.minLength = 16 }))
        assertEquals(longHex3a, 58.toHexString(HexFormat { number { minLength = 16; removeLeadingZeros = true } }))
    }

    @Test
    fun parseLongFromSubstring() {
        val url = "https://magnuschatt.medium.com/why-you-should-totally-switch-to-kotlin-c7bbde9e10d5"
        val articleId = url.substringAfterLast('-').hexToLong()
        assertEquals(0xc7bbde9e10d5, articleId)
    }

    // Number parsing strictness

    @Test
    fun parseRequiresPrefixSuffix() {
        assertEquals(58, "0x0000003a".hexToInt(HexFormat { number.prefix = "0x" }))
        assertEquals(58, "0x3a".hexToInt(HexFormat { number.prefix = "0x" }))
        assertEquals(10, "3a".hexToInt(HexFormat { number.prefix = "3" }))
        assertFailsWith<NumberFormatException> {
            "0000003a".hexToInt(HexFormat { number.prefix = "0x" })
        }
        assertEquals(58, "0000003ah".hexToInt(HexFormat { number.suffix = "h" }))
        assertEquals(58, "3ah".hexToInt(HexFormat { number.suffix = "h" }))
        assertEquals(3, "3a".hexToInt(HexFormat { number.suffix = "a" }))
        assertFailsWith<NumberFormatException> {
            "0000003a".hexToInt(HexFormat { number.suffix = "h" })
        }
    }

    @Test
    fun parseInvalidDigit() {
        assertFailsWith<NumberFormatException> { "3g".hexToByte() }
        assertFailsWith<NumberFormatException> { "3g".hexToShort() }
        assertFailsWith<NumberFormatException> { "3g".hexToInt() }
        assertFailsWith<NumberFormatException> { "3g".hexToLong() }
    }

    @Test
    fun parseRequiresAtLeastOneHexDigit() {
        assertFailsWith<NumberFormatException> {
            "".hexToInt()
        }
        assertFailsWith<NumberFormatException> {
            "3a".hexToInt(HexFormat { number { prefix = "3"; suffix = "a" } })
        }
        assertFailsWith<NumberFormatException> {
            "3a".hexToInt(HexFormat { number.prefix = "3a" })
        }
        assertFailsWith<NumberFormatException> {
            "3a".hexToInt(HexFormat { number.suffix = "3a" })
        }
        assertEquals(15, "0xfh".hexToInt(HexFormat { number { prefix = "0x"; suffix = "h" } }))
    }

    @Test
    fun parseIgnoresCase() {
        assertEquals(58, "0000003a".hexToInt())
        assertEquals(58, "3a".hexToInt())
        assertEquals(58, "0000003A".hexToInt())
        assertEquals(58, "3A".hexToInt())

        val format = HexFormat {
            upperCase = true
            number {
                prefix = "0X"
                suffix = "h"
            }
        }
        assertEquals(58, "0X0000003AH".hexToInt(format))
        assertEquals(58, "0x3Ah".hexToInt(format))
        assertEquals(58, "0X0000003aH".hexToInt(format))
        assertEquals(58, "0x3ah".hexToInt(format))
    }

    @Test
    fun parseIgnoresRemoveLeadingZeros() {
        assertEquals(58, "3a".hexToInt())
        assertEquals(58, "3a".hexToInt(HexFormat { number.removeLeadingZeros = false }))
        assertEquals(58, "0000003a".hexToInt(HexFormat { number.removeLeadingZeros = true }))
    }

    @Test
    fun parseIgnoresMinLength() {
        assertEquals(58, "3a".hexToInt(HexFormat { number.minLength = 4 }))
        assertEquals(58, "3a".hexToInt(HexFormat { number.minLength = 8 }))
        assertEquals(58L, "3a".hexToLong(HexFormat { number.minLength = 20 }))
    }

    @Test
    fun parseRequiresValueFitTheType() {
        val zeros = "0".repeat(20)
        val fs = "f".repeat(20)

        fun <T> test(typeHexLength: Int, toType: Long.() -> T, hexToType: String.() -> T) {
            assertEquals(0L.toType(), zeros.hexToType())

            val maxHex = fs.take(typeHexLength)
            assertEquals((-1L).toType(), maxHex.hexToType())

            val paddedMaxHex = "${zeros}${maxHex}"
            assertEquals((-1L).toType(), paddedMaxHex.hexToType())

            val hexLengthZeros = fs.take(typeHexLength)
            assertFailsWith<NumberFormatException> {
                "1$hexLengthZeros".hexToType()
            }
            assertFailsWith<NumberFormatException> {
                "${zeros}1$hexLengthZeros".hexToType()
            }
        }

        test(2, Long::toByte, String::hexToByte)
        test(2, Long::toUByte, String::hexToUByte)
        test(4, Long::toShort, String::hexToShort)
        test(4, Long::toUShort, String::hexToUShort)
        test(8, Long::toInt, String::hexToInt)
        test(8, Long::toUInt, String::hexToUInt)
        test(16, Long::toLong, String::hexToLong)
        test(16, Long::toULong, String::hexToULong)
    }

    // Invalid HexFormat configuration

    @Test
    fun prefixWithNewLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { number.prefix = "\n" } }
        assertFailsWith<IllegalArgumentException> { HexFormat { number.prefix = "\r" } }
    }

    @Test
    fun suffixWithNewLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { number.suffix = "ab\ncd" } }
        assertFailsWith<IllegalArgumentException> { HexFormat { number.suffix = "ab\rcd" } }
    }

    @Test
    fun minLengthNonPositive() {
        assertFailsWith<IllegalArgumentException> { HexFormat { number.minLength = 0 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { number.minLength = -1 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { number.minLength = Int.MIN_VALUE } }
    }
}