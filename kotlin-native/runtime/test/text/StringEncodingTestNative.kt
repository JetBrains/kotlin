/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(FreezingIsDeprecated::class)

package test.text

import kotlin.reflect.KClass
import kotlin.test.*
import kotlinx.cinterop.toKString
import test.assertArrayContentEquals

internal actual val surrogateCodePointDecoding: String = "\uFFFD".repeat(3)

internal actual val surrogateCharEncoding: ByteArray = byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())

// Native-specific part of stdlib/test/text/StringEncodingTest.kt
class StringEncodingTestNative {
    private fun bytes(vararg elements: Int) = ByteArray(elements.size) {
        val v = elements[it]
        require(v in Byte.MIN_VALUE..Byte.MAX_VALUE)
        v.toByte()
    }

    private fun testEncoding(isWellFormed: Boolean, expected: ByteArray, string: String) {
        assertArrayContentEquals(expected, string.encodeToByteArray())
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { string.encodeToByteArray(throwOnInvalidSequence = true) }
        } else {
            assertArrayContentEquals(expected, string.encodeToByteArray(throwOnInvalidSequence = true))
            assertEquals(string, string.encodeToByteArray(throwOnInvalidSequence = true).decodeToString())
        }
    }

    private fun testEncoding(isWellFormed: Boolean, expected: ByteArray, string: String, startIndex: Int, endIndex: Int) {
        assertArrayContentEquals(expected, string.encodeToByteArray(startIndex, endIndex))
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { string.encodeToByteArray(startIndex, endIndex, true) }
        } else {
            assertArrayContentEquals(expected, string.encodeToByteArray(startIndex, endIndex, true))
            string.encodeToByteArray(startIndex, endIndex, true).decodeToString()
        }
    }

    @Test
    fun encodeToByteArray() {
        // Valid strings.
        testEncoding(true, bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), "Hello")
        testEncoding(true, bytes(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126), "Привет")

        // Different kinds of input
        testEncoding(false, bytes(-16, -112, -128, -128, '1'.toInt(), -17, -65, -67, -17, -65, -67), "\uD800\uDC001\uDC00\uD800")
        // Lone surrogate
        testEncoding(false, bytes(-17, -65, -67, '1'.toInt(), '2'.toInt()), "\uD80012", )
        testEncoding(false, bytes(-17, -65, -67, '1'.toInt(), '2'.toInt()), "\uDC0012")
        testEncoding(false, bytes('1'.toInt(), '2'.toInt(), -17, -65, -67), "12\uD800")
    }

    @Test
    fun encodeToByteArraySlice() {
        // Valid strings.
        testEncoding(true, bytes(-16, -112, -128, -128, -16, -112, -128, -128), "\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00", 0, 4)
        testEncoding(true, bytes(-16, -112, -128, -128, -16, -112, -128, -128), "\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00", 2, 6)
        testEncoding(true, bytes(-16, -112, -128, -128, -16, -112, -128, -128), "\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00", 4, 8)

        // Illegal surrogate pair
        testEncoding(false, bytes(-17, -65, -67, -17, -65, -67, '1'.toInt()), "\uDC00\uD80012", 0, 3)
        testEncoding(false, bytes(-17, -65, -67, -17, -65, -67, '2'.toInt()), "1\uDC00\uD8002", 1, 4)
        testEncoding(false, bytes('2'.toInt(), -17, -65, -67, -17, -65, -67), "12\uDC00\uD800", 1, 4)
        // Lone surrogate
        testEncoding(false, bytes('1'.toInt(), -17, -65, -67), "1\uD800\uDC002", 0, 2)
        testEncoding(false, bytes(-17, -65, -67, '2'.toInt()), "1\uD800\uDC002", 2, 4)
    }

    private fun testDecoding(isWellFormed: Boolean, expected: String, bytes: ByteArray) {
        assertEquals(expected, bytes.decodeToString())
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { bytes.decodeToString(throwOnInvalidSequence = true) }
        } else {
            assertEquals(expected, bytes.decodeToString(throwOnInvalidSequence = true))
            assertArrayContentEquals(bytes, bytes.decodeToString(throwOnInvalidSequence = true).encodeToByteArray())
        }
    }

    private fun testDecoding(isWellFormed: Boolean, expected: String, bytes: ByteArray, startIndex: Int, endIndex: Int) {
        assertEquals(expected, bytes.decodeToString(startIndex, endIndex))
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { bytes.decodeToString(startIndex, endIndex, true) }
        } else {
            assertEquals(expected, bytes.decodeToString(startIndex, endIndex, true))
            assertArrayContentEquals(
                    bytes.sliceArray(startIndex until endIndex),
                    bytes.decodeToString(startIndex, endIndex, true).encodeToByteArray()
            )
        }
    }

    @Test
    fun decodeToString() {
        // Valid strings.
        testDecoding(true, "Hello", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
        testDecoding(true, "Привет", bytes(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
        testDecoding(true, "\uD800\uDC00", bytes(-16, -112, -128, -128))

        // Incorrect UTF-8 lead character.
        testDecoding(false, "\uFFFD1", bytes(-1, '1'.toInt()))

        // Incomplete codepoint.
        testDecoding(false, "\uFFFD1", bytes(-16, -97, -104, '1'.toInt()))
        testDecoding(false, "\uFFFD1\uFFFD", bytes(-16, -97, -104, '1'.toInt(), -16, -97, -104))
    }

    @Test
    fun decodeToStringSlice() {
        // Valid strings.
        testDecoding(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 0, 8)
        testDecoding(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 4, 12)
        testDecoding(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 8, 16)

        // Incorrect UTF-8 lead character.
        testDecoding(false, "\uFFFD1", bytes(-1, '1'.toInt(), '2'.toInt()), 0, 2)
        testDecoding(false, "\uFFFD2", bytes('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 3)
        testDecoding(false, "2\uFFFD", bytes('1'.toInt(), '2'.toInt(), -1), 1, 3)

        // Incomplete codepoint.
        testDecoding(false, "\uFFFD1", bytes(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
        testDecoding(false, "\uFFFD2", bytes('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 5)
        testDecoding(false, "2\uFFFD", bytes('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 5)
    }

    private fun testToKString(isWellFormed: Boolean, expected: String, bytes: ByteArray) {
        assertEquals(expected, bytes.toKString())
        assertEquals(expected, bytes.copyOf(bytes.size + 1).toKString())
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { bytes.toKString(throwOnInvalidSequence = true) }
            assertFailsWith<CharacterCodingException> { bytes.copyOf(bytes.size + 1).toKString(throwOnInvalidSequence = true) }
        } else {
            assertEquals(expected, bytes.toKString(throwOnInvalidSequence = true))
            assertEquals(expected, bytes.copyOf(bytes.size + 1).toKString(throwOnInvalidSequence = true))
        }
    }

    private fun testToKString(isWellFormed: Boolean, expected: String, bytes: ByteArray, startIndex: Int, endIndex: Int) {
        assertEquals(expected, bytes.toKString(startIndex, endIndex))
        assertEquals(expected, bytes.copyOf(bytes.size + 1).toKString(startIndex, endIndex))
        if (!isWellFormed) {
            assertFailsWith<CharacterCodingException> { bytes.toKString(startIndex, endIndex, true) }
            assertFailsWith<CharacterCodingException> { bytes.copyOf(bytes.size + 1).toKString(startIndex, endIndex, true) }
        } else {
            assertEquals(expected, bytes.toKString(startIndex, endIndex, true))
            assertEquals(expected, bytes.copyOf(bytes.size + 1).toKString(startIndex, endIndex, true))
        }
    }

    @Test
    fun toKString() {
        // Valid strings.
        testToKString(true, "Hello", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
        testToKString(true, "Hell", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 0, 'o'.toInt()))
        testToKString(true, "Привет", bytes(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
        testToKString(true, "При", bytes(-48, -97, -47, -128, -48, -72, 0, -48, -78, 0, -48, -75, -47, -126))
        testToKString(true, "\uD800\uDC00", bytes(-16, -112, -128, -128))
        testToKString(true, "\uD800\uDC00", bytes(-16, -112, -128, -128, 0, -16, -112, -128, -128))
        testToKString(true, "", bytes())
        testToKString(true, "", bytes(0, 'H'.toInt()))

        // Incorrect UTF-8 lead character.
        testToKString(false, "\uFFFD1", bytes(-1, '1'.toInt()))
        testToKString(false, "\uFFFD", bytes(-1, 0, '1'.toInt()))

        // Incomplete codepoint.
        testToKString(false, "\uFFFD1", bytes(-16, -97, -104, '1'.toInt()))
        testToKString(false, "\uFFFD", bytes(-16, -97, -104, 0, '1'.toInt()))
        testToKString(false, "\uFFFD1\uFFFD", bytes(-16, -97, -104, '1'.toInt(), -16, -97, -104))
        testToKString(false, "\uFFFD1", bytes(-16, -97, -104, '1'.toInt(), 0, -16, -97, -104))
    }

    @Test
    fun toKStringSlice() {
        assertFailsWith<IndexOutOfBoundsException> { bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()).toKString(-1, 3) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()).toKString(5, 15) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()).toKString(2, 12) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()).toKString(10, 10) }
        assertFailsWith<IllegalArgumentException> { bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()).toKString(3, 1) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0).toKString(-1, 3) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0).toKString(8, 18) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0).toKString(2, 12) }
        assertFailsWith<IndexOutOfBoundsException> { bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0).toKString(10, 10) }
        assertFailsWith<IllegalArgumentException> { bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0).toKString(3, 1) }

        // Valid strings.
        testToKString(true, "He", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 0, 2)
        testToKString(true, "ll", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 2, 4)
        testToKString(true, "lo", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 3, 5)
        testToKString(true, "", bytes('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 0, 0)
        testToKString(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 0, 8)
        testToKString(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 4, 12)
        testToKString(true, "\uD800\uDC00\uD800\uDC00", bytes(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128), 8, 16)
        testToKString(true, "aaa", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 0, 5)
        testToKString(true, "a", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 2, 4)
        testToKString(true, "", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 3, 5)
        testToKString(true, "bb", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 4, 6)
        testToKString(true, "bbb", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 4, 7)
        testToKString(true, "bbb", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 4, 8)
        testToKString(true, "bb", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 5, 8)
        testToKString(true, "b", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 6, 8)
        testToKString(true, "", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 7, 8)
        testToKString(true, "", bytes('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0), 8, 8)

        // Incorrect UTF-8 lead character.
        testToKString(false, "\uFFFD1", bytes(-1, '1'.toInt(), '2'.toInt()), 0, 2)
        testToKString(false, "\uFFFD2", bytes('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 3)
        testToKString(false, "2\uFFFD", bytes('1'.toInt(), '2'.toInt(), -1), 1, 3)

        // Incomplete codepoint.
        testToKString(false, "\uFFFD1", bytes(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
        testToKString(false, "\uFFFD2", bytes('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 5)
        testToKString(false, "2\uFFFD", bytes('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 5)
    }
}