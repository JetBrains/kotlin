/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

// Native-specific part of stdlib/test/text/StringNumberConversionTest.kt
class StringNumberConversionNativeTest {
    @Test
    fun toDouble() {
        assertEquals(0.5, "0.5".toDouble())
        assertEquals(-5000000000.0, "-00000000000000000000.5e10".toDouble())
        assertEquals(-0.005, "-00000000000000000000.5e-2".toDouble())
        assertEquals(50000000000.0, "+5e10".toDouble())
        assertEquals(50000000000.0, "   +5e10   ".toDouble())
        assertEquals(520.0, "+5.2e2d".toDouble())
        assertEquals(0.052, "+5.2e-2d".toDouble())
        assertEquals(52340000000.0, "+5.234e+10d".toDouble())
        assertEquals(5.234E123, "+5.234e+123d".toDouble())
        assertEquals(5.234E123, "+5.234e+123f".toDouble())
        assertEquals(5.234E123, "+5.234e+123".toDouble())
        assertEquals(5.5, "5.5f".toDouble())
        assertEquals(2.71, "\u0009 \u000A 2.71 \u000D".toDouble())
        assertEquals(42.3, "\n 42.3 ".toDouble())

        assertFailsWith<NumberFormatException> {
            "+-5.0".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "d".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "5.5.3e123d".toDouble()
        }

        // regression of incorrect processing of long lines - such values returned Infinity
        assertFailsWith<NumberFormatException> {
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "+-my free text           with different letters $3213#.  e ".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "eeeeeEEEEEeeeeeee".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "InfinityN".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "NaNPICEZy".toDouble()
        }

        assertFailsWith<NumberFormatException> { "\u20293.14".toDouble() }
        assertFailsWith<NumberFormatException> { "3.14\u200B".toDouble() }
        assertFailsWith<NumberFormatException> { "3.14\u200B ABC".toDouble() }

        // Illegal surrogate pair
        assertFailsWith<NumberFormatException> { "\uDC00\uD800".toDouble() }
        // Different kinds of input (including illegal one)
        assertFailsWith<NumberFormatException> { "\uD800\uDC001\uDC00\uD800".toDouble() }
        // Lone surrogate
        assertFailsWith<NumberFormatException> { "\uD80012".toDouble() }
        assertFailsWith<NumberFormatException> { "\uDC0012".toDouble() }
        assertFailsWith<NumberFormatException> { "12\uD800".toDouble() }
    }

    @Test
    fun toFloat() {
        assertEquals(0.5f, "0.5".toFloat())
        assertEquals(-5000000000f, "-00000000000000000000.5e10f".toFloat())
        assertEquals(-0.005f, "-00000000000000000000.5e-2f".toFloat())
        assertEquals(50000000000f, "+5e10".toFloat())
        assertEquals(50000000000f, "    +5e10    ".toFloat())
        assertEquals(0.052f, "+5.2e-2f".toFloat())
        assertEquals(520f, "+5.2e2f".toFloat())
        assertEquals(0.052f, "+5.2e-2f".toFloat())
        assertEquals(52340000000f, "+5.234e+10f".toFloat())
        assertEquals(Float.POSITIVE_INFINITY, "+5.234e+123f".toFloat())
        assertEquals(7.15f, "\u0019 7.15  ".toFloat())
        assertEquals(2.71f, "\u0009 \u000A 2.71 \u000D".toFloat())

        assertFailsWith<NumberFormatException> {
            "+-5.0f".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "f".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "5.5.3e123f".toFloat()
        }

        // regression of incorrect processing of long lines - such values returned Infinity
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "this string is not a numb3r, am I right?????????????".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "+-my free text           with different letters $3213#.  e ".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "eeeeeEEEEEeeeeeee".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "InfinityN".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "NaNPICEZy".toFloat()
        }

        assertFailsWith<NumberFormatException> { "\u202F3.14".toFloat() }

        // Illegal surrogate pair
        assertFailsWith<NumberFormatException> { "\uDC00\uD800".toFloat() }
        // Different kinds of input (including illegal one)
        assertFailsWith<NumberFormatException> { "\uD800\uDC001\uDC00\uD800".toFloat() }
        // Lone surrogate
        assertFailsWith<NumberFormatException> { "\uD80012".toFloat() }
        assertFailsWith<NumberFormatException> { "\uDC0012".toFloat() }
        assertFailsWith<NumberFormatException> { "12\uD800".toFloat() }
    }

    @Test
    fun byteToString() {
        assertEquals("13", 13.toByte().toString())
        assertEquals("-1", (-1).toByte().toString())
        assertEquals("-128", Byte.MIN_VALUE.toString())
        assertEquals("127", Byte.MAX_VALUE.toString())
    }

    @Test
    fun shortToString() {
        assertEquals("239", 239.toShort().toString())
        assertEquals("-32768", Short.MIN_VALUE.toString())
        assertEquals("32767", Short.MAX_VALUE.toString())
    }

    @Test
    fun intToString() {
        assertEquals("1122334455", 1122334455.toString())
        assertEquals("-2147483648", Int.MIN_VALUE.toString())
        assertEquals("2147483647", Int.MAX_VALUE.toString())
    }

    @Test
    fun longToString() {
        assertEquals("112233445566778899", 112233445566778899L.toString())
        assertEquals("-9223372036854775808", Long.MIN_VALUE.toString())
        assertEquals("9223372036854775807", Long.MAX_VALUE.toString())
    }

    @Test
    fun floatToString() {
        assertEquals("1.0E27", 1e27.toFloat().toString())
        assertEquals("1.4E-45", Float.MIN_VALUE.toString())
        assertEquals("3.4028235E38", Float.MAX_VALUE.toString())
        assertEquals("-Infinity", Float.NEGATIVE_INFINITY.toString())
        assertEquals("Infinity", Float.POSITIVE_INFINITY.toString())
        assertEquals("NaN", Float.NaN.toString())
    }

    @Test
    fun doubleToString() {
        assertEquals("3.14159265358", 3.14159265358.toString())
        assertEquals("1.0E7", 1e7.toString())
        assertEquals("1.0E-300", 1e-300.toDouble().toString())
        assertEquals("4.9E-324", Double.MIN_VALUE.toString())
        assertEquals("1.7976931348623157E308", Double.MAX_VALUE.toString())
        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.toString())
        assertEquals("Infinity", Double.POSITIVE_INFINITY.toString())
        assertEquals("NaN", Double.NaN.toString())
    }

    @Test
    fun booleanToString() {
        assertEquals("true", true.toString())
        assertEquals("false", false.toString())
    }
}