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

    private fun testFormatAndParse(number: Long, string: String, format: HexFormat) {
        testFormat(number, string, format)
        testParse(string, number, format)
    }

    private fun testFormat(number: Long, string: String, format: HexFormat) {
        require(format.number.prefix.isEmpty() && format.number.suffix.isEmpty())

        assertEquals(string.takeLast(2), number.toByte().toHexString(format))
        assertEquals(string.takeLast(2), number.toUByte().toHexString(format))
        assertEquals(string.takeLast(4), number.toShort().toHexString(format))
        assertEquals(string.takeLast(4), number.toUShort().toHexString(format))
        assertEquals(string.takeLast(8), number.toInt().toHexString(format))
        assertEquals(string.takeLast(8), number.toUInt().toHexString(format))
        assertEquals(string, number.toHexString(format))
        assertEquals(string, number.toULong().toHexString(format))
    }

    private fun testParse(string: String, number: Long, format: HexFormat) {
        require(format.number.prefix.isEmpty() && format.number.suffix.isEmpty())

        assertEquals(number.toByte(), string.takeLast(2).hexToByte(format))
        assertEquals(number.toUByte(), string.takeLast(2).hexToUByte(format))
        assertEquals(number.toShort(), string.takeLast(4).hexToShort(format))
        assertEquals(number.toUShort(), string.takeLast(4).hexToUShort(format))
        assertEquals(number.toInt(), string.takeLast(8).hexToInt(format))
        assertEquals(number.toUInt(), string.takeLast(8).hexToUInt(format))
        assertEquals(number, string.hexToLong(format))
        assertEquals(number.toULong(), string.hexToULong(format))
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
    fun formatPrefixSuffix() {
        assertEquals("0x0000003a", 58.toHexString(HexFormat { number.prefix = "0x" }))
        assertEquals("0000003ah", 58.toHexString(HexFormat { number.suffix = "h" }))
        assertEquals("0x0000003ah", 58.toHexString(HexFormat { number.prefix = "0x"; number.suffix = "h" }))
    }

    @Test
    fun removeLeadingZeros() {
        val format = HexFormat { number.removeLeadingZeros = true }
        testFormatAndParse(58, "3a", format)
        testFormatAndParse(0, "0", format)
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
    fun parseLimitsHexLength() {
        testParse("3", 3, HexFormat.Default)  // length = 1
        testParse("00", 0, HexFormat.Default)  // length = 2
        testParse("3a", 58, HexFormat.Default)  // length = 2
        assertFailsWith<NumberFormatException> { "03a".hexToByte() } // length = 3
        assertFailsWith<NumberFormatException> { "03a".hexToUByte() } // length = 3
        testParse("03a", 58, HexFormat.Default)  // length = 3
        testParse("003a", 58, HexFormat.Default)  // length = 4
        assertFailsWith<NumberFormatException> { "0003a".hexToShort() } // length = 5
        assertFailsWith<NumberFormatException> { "0003a".hexToUShort() } // length = 5
        testParse("0003a", 58, HexFormat.Default)  // length = 5
        testParse("0000003a", 58, HexFormat.Default)  // length = 8
        assertFailsWith<NumberFormatException> { "00000003a".hexToInt() } // length = 9
        assertFailsWith<NumberFormatException> { "00000003a".hexToUInt() } // length = 9
        testParse("00000003a", 58, HexFormat.Default)  // length = 9
        testParse("000000000000003a", 58, HexFormat.Default)  // length = 16
        assertFailsWith<NumberFormatException> { "00000000000000003a".hexToLong() } // length = 17
        assertFailsWith<NumberFormatException> { "00000000000000003a".hexToULong() } // length = 17
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
}