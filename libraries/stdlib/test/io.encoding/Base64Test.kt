/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io.encoding

import kotlin.test.*
import kotlin.io.encoding.Base64

class Base64Test {

    private fun testEncode(codec: Base64, bytes: ByteArray, expected: String) {
        assertEquals(expected, codec.encode(bytes))
        assertContentEquals(expected.encodeToByteArray(), codec.encodeToByteArray(bytes))
    }

    private fun testDecode(codec: Base64, symbols: String, expected: ByteArray) {
        assertContentEquals(expected, codec.decode(symbols))
        assertContentEquals(expected, codec.decode(symbols.encodeToByteArray()))
    }

    private fun testCoding(codec: Base64, bytes: ByteArray, symbols: String) {
        testEncode(codec, bytes, symbols)
        testDecode(codec, symbols, bytes)
    }

    private fun bytes(vararg values: Int): ByteArray {
        return ByteArray(values.size) { values[it].toByte() }
    }

    private val codecs = listOf(
        Base64 to "Basic",
        Base64.UrlSafe to "UrlSafe",
        Base64.Mime to "Mime"
    )

    @Test
    fun index() {
        val bytes = bytes(0b0000_0100, 0b0010_0000, 0b1100_0100, 0b0001_0100, 0b0110_0001, 0b1100_1000)
        val symbols = "BCDEFGHI"

        // encode
        for ((base64, scheme) in codecs) {
            testEncode(base64, bytes, symbols)
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.encode(bytes, startIndex = -1) }
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.encode(bytes, endIndex = bytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.encode(bytes, startIndex = bytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.encode(bytes, startIndex = 3, endIndex = 0) }

            assertEquals(symbols.substring(0, 4), base64.encode(bytes, endIndex = 3))
            assertEquals(symbols.substring(4), base64.encode(bytes, startIndex = 3))

            val destination = StringBuilder()
            base64.encodeToAppendable(bytes, destination, endIndex = 3)
            assertEquals(symbols.substring(0, 4), destination.toString())
            base64.encodeToAppendable(bytes, destination, startIndex = 3)
            assertEquals(symbols, destination.toString())
        }

        // encodeToByteArray
        for ((base64, scheme) in codecs) {
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.encodeToByteArray(bytes, startIndex = -1) }
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.encodeToByteArray(bytes, endIndex = bytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.encodeToByteArray(bytes, startIndex = bytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.encodeToByteArray(bytes, startIndex = 3, endIndex = 0) }

            assertContentEquals(symbols.encodeToByteArray(0, 4), base64.encodeToByteArray(bytes, endIndex = 3))
            assertContentEquals(symbols.encodeToByteArray(4), base64.encodeToByteArray(bytes, startIndex = 3))

            val destination = ByteArray(8)
            assertFailsWith<IndexOutOfBoundsException> { base64.encodeIntoByteArray(bytes, destination, destinationOffset = -1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.encodeIntoByteArray(bytes, destination, destinationOffset = destination.size + 1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.encodeIntoByteArray(bytes, destination, destinationOffset = 1) }

            assertTrue(destination.all { it == 0.toByte() })

            var length = base64.encodeIntoByteArray(bytes, destination, endIndex = 3)
            assertContentEquals(symbols.encodeToByteArray(0, 4), destination.copyOf(length))
            length += base64.encodeIntoByteArray(bytes, destination, destinationOffset = length, startIndex = 3)
            assertContentEquals(symbols.encodeToByteArray(), destination)
        }

        // decode(CharSequence)
        for ((base64, scheme) in codecs) {
            testDecode(base64, symbols, bytes)
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.decode(symbols, startIndex = -1) }
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.decode(symbols, endIndex = symbols.length + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.decode(symbols, startIndex = symbols.length + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.decode(symbols, startIndex = 4, endIndex = 0) }

            assertContentEquals(bytes.copyOfRange(0, 3), base64.decode(symbols, endIndex = 4))
            assertContentEquals(bytes.copyOfRange(3, bytes.size), base64.decode(symbols, startIndex = 4))

            val destination = ByteArray(6)
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbols, destination, destinationOffset = -1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbols, destination, destinationOffset = destination.size + 1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbols, destination, destinationOffset = 1) }

            assertTrue(destination.all { it == 0.toByte() })

            var length = base64.decodeIntoByteArray(symbols, destination, endIndex = 4)
            assertContentEquals(bytes.copyOfRange(0, 3), destination.copyOf(length))
            length += base64.decodeIntoByteArray(symbols, destination, destinationOffset = length, startIndex = 4)
            assertContentEquals(bytes, destination)
        }

        // decode(ByteArray)
        val symbolBytes = symbols.encodeToByteArray()
        for ((base64, scheme) in codecs) {
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.decode(symbolBytes, startIndex = -1) }
            assertFailsWith<IndexOutOfBoundsException>(scheme) { base64.decode(symbolBytes, endIndex = symbolBytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.decode(symbolBytes, startIndex = symbolBytes.size + 1) }
            assertFailsWith<IllegalArgumentException>(scheme) { base64.decode(symbolBytes, startIndex = 4, endIndex = 0) }

            assertContentEquals(bytes.copyOfRange(0, 3), base64.decode(symbolBytes, endIndex = 4))
            assertContentEquals(bytes.copyOfRange(3, bytes.size), base64.decode(symbolBytes, startIndex = 4))

            val destination = ByteArray(6)
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbolBytes, destination, destinationOffset = -1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbolBytes, destination, destinationOffset = destination.size + 1) }
            assertFailsWith<IndexOutOfBoundsException> { base64.decodeIntoByteArray(symbolBytes, destination, destinationOffset = 1) }

            assertTrue(destination.all { it == 0.toByte() })

            var length = base64.decodeIntoByteArray(symbolBytes, destination, endIndex = 4)
            assertContentEquals(bytes.copyOfRange(0, 3), destination.copyOf(length))
            length += base64.decodeIntoByteArray(symbolBytes, destination, destinationOffset = length, startIndex = 4)
            assertContentEquals(bytes, destination)
        }
    }

    @Test
    fun common() {
        fun testEncode(bytes: ByteArray, symbols: String) {
            testEncode(Base64, bytes, symbols)
            testEncode(Base64.UrlSafe, bytes, symbols)
            testEncode(Base64.Mime, bytes, symbols)
        }

        fun testDecode(symbols: String, bytes: ByteArray) {
            testDecode(Base64, symbols, bytes)
            testDecode(Base64.UrlSafe, symbols, bytes)
            testDecode(Base64.Mime, symbols, bytes)
        }

        fun testCoding(text: String, symbols: String) {
            val bytes = text.encodeToByteArray()
            testEncode(bytes, symbols)
            testDecode(symbols, bytes)
        }

        testCoding("", "")
        testCoding("f", "Zg==")
        testCoding("fo", "Zm8=")
        testCoding("foo", "Zm9v")
        testCoding("foob", "Zm9vYg==")
        testCoding("fooba", "Zm9vYmE=")
        testCoding("foobar", "Zm9vYmFy")

        // the padded bits are allowed to be non-zero
        testDecode("Zm9=", "fo".encodeToByteArray())

        // paddings not required
        testDecode("Zg", "f".encodeToByteArray())
        testDecode("Zm9vYmE", "fooba".encodeToByteArray())

        for ((codec, scheme) in codecs) {
            // dangling single symbol at the end that does not have bits even for a byte
            val lastDandlingSymbol = listOf("Z", "Z=", "Z==", "Z===", "Zm9vZ", "Zm9vZ=", "Zm9vZ==", "Zm9vZ===")
            for (symbols in lastDandlingSymbol) {
                assertFailsWith<IllegalArgumentException>("$scheme <$symbols>") { codec.decode(symbols) }
            }

            // incorrect padding
            assertFailsWith<IllegalArgumentException>(scheme) { codec.decode("Zg=") }
            assertFailsWith<IllegalArgumentException>(scheme) { codec.decode("Zm9vYmE==") }

            // padding in the middle
            assertFailsWith<IllegalArgumentException>(scheme) { codec.decode("Zg==Zg==") }
        }
    }

    private val basicAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val urlSafeAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val alphabetBytes = ByteArray(48) {
        val symbol = it / 3 * 4
        when (it % 3) {
            0 -> (symbol shl 2) + ((symbol + 1) shr 4)
            1 -> ((symbol + 1) and 0xF shl 4) + ((symbol + 2) shr 2)
            else -> ((symbol + 2) and 0x3 shl 6) + (symbol + 3)
        }.toByte()
    }

    @Test
    fun basic() {
        testCoding(Base64, bytes(0b1111_1011, 0b1111_0000), "+/A=")

        // all symbols from alphabet
        testCoding(Base64, alphabetBytes, basicAlphabet)

        // decode line separator
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9v\r\nYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9v\nYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9\rvYg==") }

        // decode illegal char
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9vY(==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm[@]9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9v-Yg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9vYg=(%^)=") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm\u00FF9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("\uFFFFZm9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.decode("Zm9vYg==\uD800\uDC00") }

        // no line separator inserted
        val expected = "Zm9vYmFy".repeat(76)
        testEncode(Base64, "foobar".repeat(76).encodeToByteArray(), expected)
    }

    @Test
    fun urlSafe() {
        testCoding(Base64.UrlSafe, bytes(0b1111_1011, 0b1111_0000), "-_A=")

        // all symbols from alphabet
        testCoding(Base64.UrlSafe, alphabetBytes, urlSafeAlphabet)

        // decode line separator
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9v\r\nYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9v\nYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9\rvYg==") }

        // decode illegal char
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9vY(==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm[@]9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9v+Yg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9vYg=(%^)=") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm\u00FF9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("\uFFFFZm9vYg==") }
        assertFailsWith<IllegalArgumentException> { Base64.UrlSafe.decode("Zm9vYg==\uD800\uDC00") }

        // no line separator inserted
        val expected = "Zm9vYmFy".repeat(76)
        testEncode(Base64.UrlSafe, "foobar".repeat(76).encodeToByteArray(), expected)
    }

    @Test
    fun mime() {
        testCoding(Base64.Mime, bytes(0b1111_1011, 0b1111_0000), "+/A=")

        // all symbols from alphabet
        testCoding(Base64.Mime, alphabetBytes, basicAlphabet)

        // dangling single symbol
        assertFailsWith<IllegalArgumentException> { Base64.Mime.decode("Zm9vY(==") }

        // decode line separator
        testDecode(Base64.Mime, "Zm9v\r\nYg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm9v\nYg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm9\rvYg==", "foob".encodeToByteArray())

        // decode illegal char
        testDecode(Base64.Mime, "Zm9vYg(==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm[@]9vYg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm9v-Yg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm9vYg=(%^)=", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm\u00FF9vYg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "\uFFFFZm9vYg==", "foob".encodeToByteArray())
        testDecode(Base64.Mime, "Zm9vYg==\uD800\uDC00", "foob".encodeToByteArray())

        // inserts line separator, but not to the end of the output
        val expected = "Zm9vYmFy".repeat(76).chunked(76).joinToString(separator = "\r\n")
        testEncode(Base64.Mime, "foobar".repeat(76).encodeToByteArray(), expected)
    }
}