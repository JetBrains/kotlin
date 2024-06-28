/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.assertArrayContentEquals
import kotlin.test.*

// When decoding utf-8, JVM and JS implementations replace the sequence reflecting a surrogate code point differently.
// JS replaces each byte of the sequence by the replacement char, whereas JVM replaces the whole sequence with a single replacement char.
// See corresponding actual to find out the replacement.
internal expect val surrogateCodePointDecoding: String

// The byte sequence used to replace a surrogate char.
// JVM default replacement sequence consist of single 0x3F byte.
// JS and Native replacement byte sequence is [0xEF, 0xBF, 0xBD].
internal expect val surrogateCharEncoding: ByteArray

class StringEncodingTest {
    private fun bytes(vararg elements: Int) = ByteArray(elements.size) { elements[it].toByte() }

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
            assertEquals(
                string.substring(startIndex, endIndex),
                string.encodeToByteArray(startIndex, endIndex, true).decodeToString()
            )
        }
    }

    // https://youtrack.jetbrains.com/issue/KT-31614
    private fun string(vararg codeUnits: Int): String {
        return buildString { codeUnits.forEach { append(Char(it)) } }
    }

    @Test
    fun encodeToByteArray() {
        // empty string
        testEncoding(true, bytes(), "")

        // 1-byte chars
        testEncoding(true, bytes(0), "\u0000")
        testEncoding(true, bytes(0x2D), "-")
        testEncoding(true, bytes(0x7F), "\u007F")

        // 2-byte chars
        testEncoding(true, bytes(0xC2, 0x80), "\u0080")
        testEncoding(true, bytes(0xC2, 0xBF), "¿")
        testEncoding(true, bytes(0xDF, 0xBF), "\u07FF")

        // 3-byte chars
        testEncoding(true, bytes(0xE0, 0xA0, 0x80), "\u0800")
        testEncoding(true, bytes(0xE6, 0x96, 0xA4), "斤")
        testEncoding(true, bytes(0xED, 0x9F, 0xBF), "\uD7FF")

        // surrogate chars
        testEncoding(false, surrogateCharEncoding, string(0xD800))
        testEncoding(false, surrogateCharEncoding, string(0xDB6A))
        testEncoding(false, surrogateCharEncoding, string(0xDFFF))

        // 3-byte chars
        testEncoding(true, bytes(0xEE, 0x80, 0x80), "\uE000")
        testEncoding(true, bytes(0xEF, 0x98, 0xBC), "\uF63C")
        testEncoding(true, bytes(0xEF, 0xBF, 0xBF), "\uFFFF")

        // 4-byte surrogate pairs
        testEncoding(true, bytes(0xF0, 0x90, 0x80, 0x80), "\uD800\uDC00")
        testEncoding(true, bytes(0xF2, 0xA2, 0x97, 0xBC), "\uDA49\uDDFC")
        testEncoding(true, bytes(0xF4, 0x8F, 0xBF, 0xBF), "\uDBFF\uDFFF")

        // reversed surrogate pairs
        testEncoding(false, surrogateCharEncoding + surrogateCharEncoding, string(0xDC00, 0xD800))
        testEncoding(false, surrogateCharEncoding + surrogateCharEncoding, string(0xDDFC, 0xDA49))
        testEncoding(false, surrogateCharEncoding + surrogateCharEncoding, string(0xDFFF, 0xDBFF))

        testEncoding(
            false,
            bytes(
                0, /**/ 0x2D, /**/ 0x7F, /**/ 0xC2, 0x80, /**/ 0xC2, 0xBF, /**/ 0xDF, 0xBF, /**/ 0xE0, 0xA0, 0x80, /**/
                0xE6, 0x96, 0xA4, /**/ 0xED, 0x9F, 0xBF, /**/ 0x7A
            ) /**/ + surrogateCharEncoding /**/ + surrogateCharEncoding /**/ + 0x7A /**/ + surrogateCharEncoding /**/ + 0x7A /**/ + surrogateCharEncoding,
            "\u0000-\u007F\u0080¿\u07FF\u0800斤\uD7FFz" + string(0xDFFF, 0xD800, 0x7A, 0xDB6A, 0x7A, 0xDB6A)
        )

        testEncoding(
            true,
            bytes(
                0xEE, 0x80, 0x80, /**/ 0xEF, 0x98, 0xBC, /**/ 0xC2, 0xBF, /**/ 0xEF, 0xBF, 0xBF, /**/
                0xF0, 0x90, 0x80, 0x80, /**/ 0xF2, 0xA2, 0x97, 0xBC, /**/ 0xF4, 0x8F, 0xBF, 0xBF
            ),
            "\uE000\uF63C¿\uFFFF\uD800\uDC00\uDA49\uDDFC\uDBFF\uDFFF"
        )

        val longChars = CharArray(200_000) { 'k' }
        val longBytes = longChars.concatToString().encodeToByteArray()
        assertEquals(200_000, longBytes.size)
        assertTrue { longBytes.all { it == 0x6B.toByte() } }
    }

    @Test
    fun encodeToByteArraySlice() {
        assertFailsWith<IllegalArgumentException> { "".encodeToByteArray(startIndex = 1) }
        assertFailsWith<IllegalArgumentException> { "123".encodeToByteArray(startIndex = 10) }
        assertFailsWith<IndexOutOfBoundsException> { "123".encodeToByteArray(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { "123".encodeToByteArray(endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { "123".encodeToByteArray(endIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { "123".encodeToByteArray(startIndex = 5, endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { "123".encodeToByteArray(startIndex = 5, endIndex = 2) }
        assertFailsWith<IndexOutOfBoundsException> { "123".encodeToByteArray(startIndex = 1, endIndex = 4) }

        testEncoding(true, bytes(), "abc", 0, 0)
        testEncoding(true, bytes(), "abc", 3, 3)
        testEncoding(true, bytes(0x62, 0x63), "abc", 1, 3)
        testEncoding(true, bytes(0x61, 0x62), "abc", 0, 2)
        testEncoding(true, bytes(0x62), "abc", 1, 2)

        testEncoding(true, bytes(0x2D), "-", 0, 1)
        testEncoding(true, bytes(0xC2, 0xBF), "¿", 0, 1)
        testEncoding(true, bytes(0xE6, 0x96, 0xA4), "斤", 0, 1)

        testEncoding(false, surrogateCharEncoding, string(0xDB6A), 0, 1)

        testEncoding(true, bytes(0xEF, 0x98, 0xBC), "\uF63C", 0, 1)

        testEncoding(true, bytes(0xF2, 0xA2, 0x97, 0xBC), "\uDA49\uDDFC", 0, 2)
        testEncoding(false, surrogateCharEncoding, "\uDA49\uDDFC", 0, 1)
        testEncoding(false, surrogateCharEncoding, "\uDA49\uDDFC", 1, 2)

        testEncoding(
            false,
            bytes(0xE6, 0x96, 0xA4, /**/ 0xED, 0x9F, 0xBF, /**/ 0x7A) /**/ + surrogateCharEncoding /**/ + surrogateCharEncoding,
            "\u0000-\u007F\u0080¿\u07FF\u0800斤\uD7FFz" + string(0xDFFF, 0xD800, 0x7A, 0xDB6A, 0x7A, 0xDB6A),
            startIndex = 7,
            endIndex = 12
        )

        testEncoding(
            false,
            bytes(0xC2, 0xBF, /**/ 0xEF, 0xBF, 0xBF, /**/ 0xF0, 0x90, 0x80, 0x80, /**/ 0xF2, 0xA2, 0x97, 0xBC) /**/ + surrogateCharEncoding,
            "\uE000\uF63C¿\uFFFF\uD800\uDC00\uDA49\uDDFC\uDBFF\uDFFF",
            startIndex = 2,
            endIndex = 9
        )

        val longChars = CharArray(200_000) { 'k' }
        val longBytes = longChars.concatToString().encodeToByteArray(startIndex = 5000, endIndex = 195_000)
        assertEquals(190_000, longBytes.size)
        assertTrue { longBytes.all { it == 0x6B.toByte() } }
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

    private fun truncatedSurrogateDecoding() =
        surrogateCodePointDecoding.let { if (it.length > 1) it.dropLast(1) else it }

    @Test
    fun decodeToString() {
        testDecoding(true, "", bytes()) // empty
        testDecoding(true, "\u0000", bytes(0x0)) // null char
        testDecoding(true, "zC", bytes(0x7A, 0x43)) // 1-byte chars

        testDecoding(false, "��", bytes(0x85, 0xAF)) // invalid bytes starting with 1 bit
        testDecoding(true, "¿", bytes(0xC2, 0xBF)) // 2-byte char
        testDecoding(false, "�z", bytes(0xCF, 0x7A)) // 2-byte char, second byte starts with 0 bit
        testDecoding(false, "��", bytes(0xC1, 0xAA)) // 1-byte char written in two bytes

        testDecoding(false, "�z", bytes(0xEF, 0xAF, 0x7A)) // 3-byte char, third byte starts with 0 bit
        testDecoding(false, "���", bytes(0xE0, 0x9F, 0xAF)) // 2-byte char written in three bytes
        testDecoding(false, "�z", bytes(0xE0, 0xAF, 0x7A)) // 3-byte char, third byte starts with 0 bit
        testDecoding(true, "\u1FFF", bytes(0xE1, 0xBF, 0xBF)) // 3-byte char

        testDecoding(false, surrogateCodePointDecoding, bytes(0xED, 0xAF, 0xBF)) // 3-byte high-surrogate char
        testDecoding(false, surrogateCodePointDecoding, bytes(0xED, 0xB3, 0x9A)) // 3-byte low-surrogate char
        testDecoding(
            false,
            surrogateCodePointDecoding + surrogateCodePointDecoding,
            bytes(0xED, 0xAF, 0xBF, /**/ 0xED, 0xB3, 0x9A)
        ) // surrogate pair chars
        testDecoding(false, "�z", bytes(0xEF, 0x7A)) // 3-byte char, second byte starts with 0 bit, third byte missing
        testDecoding(false, "�¿", bytes(0xF0, 0xC2, 0xBF)) // 2-byte char preceded with a 4-byte sequence starting byte
        testDecoding(false, "�ფ", bytes(0xF0, 0xE1, 0x83, 0xA4)) // 3-byte char preceded with a 4-byte sequence starting byte
        testDecoding(false, "�ფ", bytes(0xC0, 0xE1, 0x83, 0xA4)) // 3-byte char preceded with a 2-byte sequence starting byte
        testDecoding(false, "�¿", bytes(0xE1, 0xC2, 0xBF)) // 2-byte char preceded with a 3-byte sequence starting byte

        testDecoding(false, "�����", bytes(0xF9, 0x94, 0x80, 0x80, 0x80)) // 5-byte code point larger than 0x10FFFF
        testDecoding(false, "������", bytes(0xFD, 0x94, 0x80, 0x80, 0x80, 0x80)) // 6-byte code point larger than 0x10FFFF

        // Ill-Formed Sequences for Surrogates
        testDecoding(
            false,
            surrogateCodePointDecoding + surrogateCodePointDecoding + truncatedSurrogateDecoding() + "A",
            bytes(0xED, 0xA0, 0x80, /**/ 0xED, 0xBF, 0xBF, /**/ 0xED, 0xAF, /**/ 0x41)
        )
        // Truncated Sequences
        testDecoding(false, "����A", bytes(0xE1, 0x80, /**/ 0xE2, /**/ 0xF0, 0x91, 0x92, /**/ 0xF1, 0xBF, /**/ 0x41))

        testDecoding(false, "�", bytes(0xC2)) // 2-byte sequences, the last byte is missing

        testDecoding(false, "�", bytes(0xE0, 0xAF)) // 3-byte char, third byte missing
        testDecoding(false, "�", bytes(0xE0)) // 3-byte sequence, but the last two bytes are missing

        testDecoding(true, "\uD83D\uDFDF", bytes(0xF0, 0x9F, 0x9F, 0x9F)) // 4-byte char
        testDecoding(false, "����", bytes(0xF0, 0x8F, 0x9F, 0x9F)) // 3-byte char written in four bytes
        testDecoding(false, "����", bytes(0xF4, 0x9F, 0x9F, 0x9F)) // 4-byte code point larger than 0x10FFFF
        testDecoding(false, "����", bytes(0xF5, 0x80, 0x80, 0x80)) // 4-byte code point larger than 0x10FFFF

        testDecoding(false, "�", bytes(0xF0)) // 4-byte sequence, but the last three bytes are missing
        testDecoding(false, "�", bytes(0xF0, 0x93)) // 4-byte sequence, but the last two bytes are missing
        testDecoding(false, "�", bytes(0xF0, 0x93, 0x88)) // 4-byte sequence, but the last byte is missing

        // a sequence consisting of three 4-byte sequence starting bytes
        testDecoding(false, "���", bytes(0xF0, 0xF0, 0xF0))

        // Non-Shortest Form Sequences
        testDecoding(false, "��������A", bytes(0xC0, 0xAF, /**/ 0xE0, 0x80, 0xBF, /**/ 0xF0, 0x81, 0x82, /**/ 0x41))
        // Other Ill-Formed Sequences
        testDecoding(false, "�����A��B", bytes(0xF4, 0x91, 0x92, 0x93, /**/ 0xFF, /**/ 0x41, /**/ 0x80, 0xBF, /**/ 0x42))

        val longBytes = ByteArray(200_000) { 0x6B.toByte() }
        val longString = longBytes.decodeToString()
        assertEquals(200_000, longString.length)
        assertTrue { longString.all { it == 'k' } }
    }

    @Test
    fun decodeToStringSlice() {
        assertFailsWith<IllegalArgumentException> { bytes().decodeToString(1, 0) }
        assertFailsWith<IllegalArgumentException> { bytes(0x61, 0x62, 0x63).decodeToString(startIndex = 10) }
        assertFailsWith<IndexOutOfBoundsException> { bytes(0x61, 0x62, 0x63).decodeToString(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { bytes(0x61, 0x62, 0x63).decodeToString(endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { bytes(0x61, 0x62, 0x63).decodeToString(endIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { bytes(0x61, 0x62, 0x63).decodeToString(startIndex = 5, endIndex = 10) }
        assertFailsWith<IllegalArgumentException> { bytes(0x61, 0x62, 0x63).decodeToString(startIndex = 5, endIndex = 2) }
        assertFailsWith<IndexOutOfBoundsException> { bytes(0x61, 0x62, 0x63).decodeToString(startIndex = 1, endIndex = 4) }

        testDecoding(true, "", bytes(), startIndex = 0, endIndex = 0)
        testDecoding(true, "", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 0)
        testDecoding(true, "", bytes(0x61, 0x62, 0x63), startIndex = 3, endIndex = 3)
        testDecoding(true, "abc", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 3)
        testDecoding(true, "ab", bytes(0x61, 0x62, 0x63), startIndex = 0, endIndex = 2)
        testDecoding(true, "bc", bytes(0x61, 0x62, 0x63), startIndex = 1, endIndex = 3)
        testDecoding(true, "b", bytes(0x61, 0x62, 0x63), startIndex = 1, endIndex = 2)

        testDecoding(true, "¿", bytes(0xC2, 0xBF), startIndex = 0, endIndex = 2)
        testDecoding(false, "�", bytes(0xC2, 0xBF), startIndex = 0, endIndex = 1)
        testDecoding(false, "�", bytes(0xC2, 0xBF), startIndex = 1, endIndex = 2)

        testDecoding(false, "�", bytes(0xEF, 0xAF, 0x7A), startIndex = 0, endIndex = 2)
        testDecoding(false, "�z", bytes(0xEF, 0xAF, 0x7A), startIndex = 1, endIndex = 3)
        testDecoding(true, "z", bytes(0xEF, 0xAF, 0x7A), startIndex = 2, endIndex = 3)

        testDecoding(false, surrogateCodePointDecoding, bytes(0xED, 0xAF, 0xBF), startIndex = 0, endIndex = 3)
        testDecoding(false, truncatedSurrogateDecoding(), bytes(0xED, 0xB3, 0x9A), startIndex = 0, endIndex = 2)
        testDecoding(false, "���", bytes(0xED, 0xAF, 0xBF, 0xED, 0xB3, 0x9A), startIndex = 1, endIndex = 4)
        testDecoding(false, "�", bytes(0xEF, 0x7A), startIndex = 0, endIndex = 1)
        testDecoding(true, "z", bytes(0xEF, 0x7A), startIndex = 1, endIndex = 2)

        testDecoding(true, "\uD83D\uDFDF", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 0, endIndex = 4)
        testDecoding(false, "��", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 2, endIndex = 4)
        testDecoding(false, "��", bytes(0xF0, 0x9F, 0x9F, 0x9F), startIndex = 1, endIndex = 3)

        val longBytes = ByteArray(200_000) { 0x6B.toByte() }
        val longString = longBytes.decodeToString(startIndex = 5000, endIndex = 195_000)
        assertEquals(190_000, longString.length)
        assertTrue { longString.all { it == 'k' } }
    }

    @Test
    fun kotlinxIOUnicodeTest() {
        fun String.readHex(): ByteArray = split(" ")
            .filter { it.isNotBlank() }
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val smokeTestData = "\ud83c\udf00"
        val smokeTestDataCharArray: CharArray = smokeTestData.toCharArray()
        val smokeTestDataAsBytes = "f0 9f 8c 80".readHex()

        val testData = "file content with unicode " +
                "\ud83c\udf00 :" +
                " \u0437\u0434\u043e\u0440\u043e\u0432\u0430\u0442\u044c\u0441\u044f :" +
                " \uc5ec\ubcf4\uc138\uc694 :" +
                " \u4f60\u597d :" +
                " \u00f1\u00e7"
        val testDataCharArray: CharArray = testData.toCharArray()
        val testDataAsBytes: ByteArray = ("66 69 6c 65 20 63 6f 6e 74 65 6e 74 20 77 69 74 " +
                " 68 20 75 6e 69 63 6f 64 65 20 f0 9f 8c 80 20 3a 20 d0 b7 d0 b4 d0 be d1 " +
                "80 d0 be d0 b2 d0 b0 d1 82 d1 8c d1 81 d1 8f 20 3a 20 ec 97 ac eb b3 b4 ec " +
                " 84 b8 ec 9a 94 20 3a 20 e4 bd a0 e5 a5 bd 20 3a 20 c3 b1 c3 a7").readHex()


        assertArrayContentEquals(smokeTestDataAsBytes, smokeTestData.encodeToByteArray())
        assertArrayContentEquals(testDataAsBytes, testData.encodeToByteArray())

        assertEquals(smokeTestData, smokeTestDataAsBytes.decodeToString())
        assertEquals(testData, testDataAsBytes.decodeToString())

        assertEquals(smokeTestData, smokeTestDataCharArray.concatToString())
        assertEquals(testData, testDataCharArray.concatToString())

        assertArrayContentEquals(smokeTestDataCharArray, smokeTestData.toCharArray())
        assertArrayContentEquals(testDataCharArray, testData.toCharArray())

        assertArrayContentEquals(smokeTestDataAsBytes, smokeTestDataCharArray.concatToString().encodeToByteArray())
        assertArrayContentEquals(testDataAsBytes, testDataCharArray.concatToString().encodeToByteArray())

        assertArrayContentEquals(smokeTestDataCharArray, smokeTestDataAsBytes.decodeToString().toCharArray())
        assertArrayContentEquals(testDataCharArray, testDataAsBytes.decodeToString().toCharArray())

        assertEquals("\uD858\uDE18\n", bytes(0xF0, 0xA6, 0x88, 0x98, 0x0a).decodeToString())
        assertEquals("\u0BF5\n", bytes(0xE0, 0xAF, 0xB5, 0x0A).decodeToString())
        assertEquals("\u041a\n", bytes(0xD0, 0x9A, 0x0A).decodeToString())
    }
}
