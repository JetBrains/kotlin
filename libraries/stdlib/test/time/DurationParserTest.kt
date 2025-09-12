/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.*
import kotlin.time.*

class DurationParserTest {

    @Test
    fun testLongParserBasicParsing() {
        val parser = LongParser.default

        // Test basic positive numbers
        var result = parser.parse("123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(3, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        // Test with leading zeros
        result = parser.parse("000456", 0) { endIndex, sign, hasOverflow ->
            assertEquals(6, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(456L, result)

        // Test zero
        result = parser.parse("0", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)

        // Test multiple zeros
        result = parser.parse("0000", 0) { endIndex, sign, hasOverflow ->
            assertEquals(4, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)
    }

    @Test
    fun testLongParserWithSign() {
        val parser = LongParser.iso

        // Test positive sign
        var result = parser.parse("+123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(4, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        // Test negative sign
        result = parser.parse("-456", 0) { endIndex, sign, hasOverflow ->
            assertEquals(4, endIndex)
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(456L, result)

        // Test negative zero
        result = parser.parse("-0", 0) { endIndex, sign, hasOverflow ->
            assertEquals(2, endIndex)
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)
    }

    @Test
    fun testLongParserPartialParsing() {
        val parser = LongParser.default

        // Test parsing stops at non-digit
        var result = parser.parse("123abc", 0) { endIndex, sign, hasOverflow ->
            assertEquals(3, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        // Test parsing with a decimal point
        result = parser.parse("456.789", 0) { endIndex, sign, hasOverflow ->
            assertEquals(3, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(456L, result)

        // Test parsing with space
        result = parser.parse("789 123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(3, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(789L, result)
    }

    @Test
    fun testLongParserOverflow() {
        val parser = LongParser.default

        // Test maximum value without overflow
        var result = parser.parse("9223372036854775807", 0) { endIndex, sign, hasOverflow ->
            assertEquals(19, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(Long.MAX_VALUE, result)

        // Test overflow detection
        result = parser.parse("9223372036854775808", 0) { endIndex, sign, hasOverflow ->
            assertEquals(19, endIndex)
            assertEquals(1, sign)
            assertTrue(hasOverflow)
        }
        assertEquals(Long.MAX_VALUE, result)

        // Test significant overflow
        result = parser.parse("99999999999999999999", 0) { endIndex, sign, hasOverflow ->
            assertEquals(20, endIndex)
            assertEquals(1, sign)
            assertTrue(hasOverflow)
        }
        assertEquals(Long.MAX_VALUE, result)

        // Test negative overflow with iso parser (supports signs)
        val isoParser = LongParser.iso
        result = isoParser.parse("-${MAX_MILLIS + 1}", 0) { endIndex, sign, hasOverflow ->
            assertEquals(20, endIndex)
            assertEquals(-1, sign)
            assertTrue(hasOverflow)
        }
        assertEquals(MAX_MILLIS, result)
    }

    @Test
    fun testLongParserStartIndex() {
        val parser = LongParser.default

        // Test parsing from a middle of string
        var result = parser.parse("prefix123suffix", 6) { endIndex, sign, hasOverflow ->
            assertEquals(9, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        // Test with sign from the middle (using iso parser)
        val isoParser = LongParser.iso
        result = isoParser.parse("text-456end", 4) { endIndex, sign, hasOverflow ->
            assertEquals(8, endIndex)
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(456L, result)
    }

    @Test
    fun testLongParserEdgeCases() {
        val parser = LongParser.iso

        // Test empty number after sign
        var result = parser.parse("+", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)

        // Test single digit with the default parser
        val defaultParser = LongParser.default
        result = defaultParser.parse("5", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(5L, result)

        // Test leading zeros with significant digits
        result = defaultParser.parse("000000000000000000001", 0) { endIndex, sign, hasOverflow ->
            assertEquals(21, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(1L, result)

        // Test multiple consecutive signs - these should stop at the first invalid character
        result = parser.parse("+-123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)  // Stops after '+' since '-' is not a digit
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)

        result = parser.parse("++123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)  // Stops after the first '+' since the second '+' is not a digit
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)

        result = parser.parse("--123", 0) { endIndex, sign, hasOverflow ->
            assertEquals(1, endIndex)  // Stops after the first '-' since the second '-' is not a digit
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(0L, result)

        // Test parsing from index 1 with multiple signs
        result = parser.parse("+-123", 1) { endIndex, sign, hasOverflow ->
            assertEquals(5, endIndex)
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        result = parser.parse("++123", 1) { endIndex, sign, hasOverflow ->
            assertEquals(5, endIndex)
            assertEquals(1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)

        result = parser.parse("--123", 1) { endIndex, sign, hasOverflow ->
            assertEquals(5, endIndex)
            assertEquals(-1, sign)
            assertFalse(hasOverflow)
        }
        assertEquals(123L, result)
    }

    @Test
    fun testFractionalParserBasicParsing() {
        // Test single-digit fraction
        var result = FractionalParser.parse("5", 0) { endIndex ->
            assertEquals(1, endIndex)
        }
        assertEquals(500_000_000_000_000L, result)

        // Test two-digit fraction
        result = FractionalParser.parse("25", 0) { endIndex ->
            assertEquals(2, endIndex)
        }
        assertEquals(250_000_000_000_000L, result)

        // Test three-digit fraction
        result = FractionalParser.parse("125", 0) { endIndex ->
            assertEquals(3, endIndex)
        }
        assertEquals(125_000_000_000_000L, result)
    }

    @Test
    fun testFractionalParserFullPrecision() {
        // Test exactly 15 digits
        var result = FractionalParser.parse("123456789012345", 0) { endIndex ->
            assertEquals(15, endIndex)
        }
        assertEquals(123456789012345L, result)

        // Test 9 digits (boundary between high and low precision)
        result = FractionalParser.parse("123456789", 0) { endIndex ->
            assertEquals(9, endIndex)
        }
        assertEquals(123456789000000L, result)

        // Test 10 digits (crosses boundary)
        result = FractionalParser.parse("1234567891", 0) { endIndex ->
            assertEquals(10, endIndex)
        }
        assertEquals(123456789100000L, result)
    }

    @Test
    fun testFractionalParserOverflow() {
        // Test more than 15 digits (should skip excess)
        val result = FractionalParser.parse("1234567890123456789", 0) { endIndex ->
            assertEquals(19, endIndex)
        }
        assertEquals(123456789012345L, result)
    }

    @Test
    fun testFractionalParserPartialParsing() {
        // Test parsing stops at non-digit
        var result = FractionalParser.parse("123abc", 0) { endIndex ->
            assertEquals(3, endIndex)
        }
        assertEquals(123_000_000_000_000L, result)

        // Test parsing with unit suffix
        result = FractionalParser.parse("5s", 0) { endIndex ->
            assertEquals(1, endIndex)
        }
        assertEquals(500_000_000_000_000L, result)

        // Test parsing with space
        result = FractionalParser.parse("789 123", 0) { endIndex ->
            assertEquals(3, endIndex)
        }
        assertEquals(789_000_000_000_000L, result)
    }

    @Test
    fun testFractionalParserStartIndex() {
        // Test parsing from a middle of string
        var result = FractionalParser.parse("prefix123suffix", 6) { endIndex ->
            assertEquals(9, endIndex)
        }
        assertEquals(123_000_000_000_000L, result)

        result = FractionalParser.parse("1.5s", 2) { endIndex ->
            assertEquals(3, endIndex)
        }
        assertEquals(500_000_000_000_000L, result)
    }

    @Test
    fun testFractionalParserLeadingZeros() {
        // Test fraction with leading zeros
        var result = FractionalParser.parse("005", 0) { endIndex ->
            assertEquals(3, endIndex)
        }
        assertEquals(5_000_000_000_000L, result)

        // Test all zeros
        result = FractionalParser.parse("000000000000000", 0) { endIndex ->
            assertEquals(15, endIndex)
        }
        assertEquals(0L, result)
    }

    @Test
    fun testFractionalParserEdgeCases() {
        // Test empty string case (would need to handle in real code)
        var result = FractionalParser.parse("s", 0) { endIndex ->
            assertEquals(0, endIndex)
        }
        assertEquals(0L, result)

        // Test very small fraction
        result = FractionalParser.parse("000000000000001", 0) { endIndex ->
            assertEquals(15, endIndex)
        }
        assertEquals(1L, result)

        // Test maximum precision value
        result = FractionalParser.parse("999999999999999", 0) { endIndex ->
            assertEquals(15, endIndex)
        }
        assertEquals(999999999999999L, result)
    }
}
