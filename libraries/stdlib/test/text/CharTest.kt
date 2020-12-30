/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class CharTest {

    @Test
    fun charFromIntCode() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        assertEquals("0Azβ+Ö\uFFFC", codes.map { Char(it) }.joinToString(separator = ""))

        assertFails { Char(-1) }
        assertFails { Char(0x1_0000) }
        assertFails { Char(1_000_000) }
    }

    @Test
    fun charFromUShortCode() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        assertEquals("0Azβ+Ö\uFFFC", codes.map { Char(it.toUShort()) }.joinToString(separator = ""))

        assertEquals('\u0000', Char(UShort.MIN_VALUE))
        assertEquals('\uFFFF', Char(UShort.MAX_VALUE))
        assertEquals('\uFFFF', Char((-1).toUShort()))
        assertEquals('\u0000', Char(0x1_0000.toUShort()))
    }

    @Test
    fun code() {
        val codes = listOf(48, 65, 122, 946, '+'.code, 'Ö'.code, 0xFFFC)
        val chars = "0Azβ+Ö\uFFFC"
        assertEquals(codes, chars.map { it.code })
        assertEquals(0, Char.MIN_VALUE.code)
        assertEquals(0xFFFF, Char.MAX_VALUE.code)
    }

    @Test
    fun digitToInt() {
        fun testEquals(expected: Int, receiver: Char, radix: Int) {
            if (radix == 10) {
                assertEquals(expected, receiver.digitToInt())
                assertEquals(expected, receiver.digitToIntOrNull())
            }
            assertEquals(expected, receiver.digitToInt(radix))
            assertEquals(expected, receiver.digitToIntOrNull(radix))
        }

        fun testFails(receiver: Char, radix: Int) {
            if (radix == 10) {
                assertFails { receiver.digitToInt() }
                assertEquals(null, receiver.digitToIntOrNull())
            }
            assertFails { receiver.digitToInt(radix) }
            assertEquals(null, receiver.digitToIntOrNull(radix))
        }

        for (char in '0'..'9') {
            val digit = char - '0'

            for (radix in (digit + 1).coerceAtLeast(2)..36) {
                testEquals(digit, char, radix)
            }
            for (radix in 2..digit) {
                testFails(char, radix)
            }
        }

        for (char in 'A'..'Z') {
            val digit = 10 + (char - 'A')
            val lower = char.toLowerCase()

            for (radix in digit + 1..36) {
                testEquals(digit, char, radix)
                testEquals(digit, lower, radix)
            }
            for (radix in 2..digit) {
                testFails(char, radix)
                testFails(lower, radix)
            }
        }

        assertFails { '0'.digitToInt(radix = 37) }
        assertFails { '0'.digitToIntOrNull(radix = 37) }
        assertFails { '0'.digitToInt(radix = 1) }
        assertFails { '0'.digitToIntOrNull(radix = 1) }

        testFails('0' - 1, radix = 10)
        testFails('9' + 1, radix = 10)
        testFails('β', radix = 36)
        testFails('+', radix = 36)
    }

    @Test
    fun digitToChar() {
        fun testEquals(expected: Char, receiver: Int, radix: Int) {
            if (radix == 10) {
                assertEquals(expected, receiver.digitToChar())
            }
            assertEquals(expected, receiver.digitToChar(radix))
        }

        fun testFails(receiver: Int, radix: Int) {
            if (radix == 10) {
                assertFails { receiver.digitToChar() }
            }
            assertFails { receiver.digitToChar(radix) }
        }

        for (int in 0..9) {
            val digit = '0' + int

            for (radix in (int + 1).coerceAtLeast(2)..36) {
                testEquals(digit, int, radix)
            }
            for (radix in 2..int) {
                testFails(int, radix)
            }

            testFails(int, radix = 37)
        }

        for (int in 10..35) {
            val digit = 'A' + (int - 10)

            for (radix in int + 1..36) {
                testEquals(digit, int, radix)
            }
            for (radix in 2..int) {
                testFails(int, radix)
            }

            testFails(int, radix = 37)
        }

        assertFails { 0.digitToChar(radix = 37) }
        assertFails { 0.digitToChar(radix = 1) }

        testFails(-1, radix = 10)
        testFails(100, radix = 36)
        testFails(100, radix = 110)
    }
}
