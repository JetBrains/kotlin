/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io.encoding

import kotlin.test.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption.*

class Base64Test {

    private fun Base64.PaddingOption.isPresentOnEncode(): Boolean =
        this == PRESENT || this == PRESENT_OPTIONAL

    private fun Base64.PaddingOption.isOptionalOnDecode(): Boolean =
        this == PRESENT_OPTIONAL || this == ABSENT_OPTIONAL

    private fun Base64.PaddingOption.isAllowedOnDecode(): Boolean =
        this == PRESENT || isOptionalOnDecode()

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
        fun testEncode(codec: Base64, bytes: String, symbols: String) {
            if (codec.paddingOption.isPresentOnEncode()) {
                testEncode(codec, bytes.encodeToByteArray(), symbols)
            } else {
                testEncode(codec, bytes.encodeToByteArray(), symbols.trimEnd('='))
            }
        }

        fun testDecode(codec: Base64, symbols: String, bytes: String) {
            if (codec.paddingOption.isAllowedOnDecode()) {
                testDecode(codec, symbols, bytes.encodeToByteArray())
                if (codec.paddingOption.isOptionalOnDecode()) {
                    testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
                }
            } else {
                testDecode(codec, symbols.trimEnd('='), bytes.encodeToByteArray())
            }
        }

        fun testCoding(codec: Base64, bytes: String, symbols: String) {
            testEncode(codec, bytes, symbols)
            testDecode(codec, symbols, bytes)
        }

        for ((codec, scheme) in codecs) {
            // By default, padding option is set to PRESENT
            assertSame(codec, codec.withPadding(Base64.PaddingOption.PRESENT))

            for (paddingOption in Base64.PaddingOption.entries) {
                val configuredCodec = codec.withPadding(paddingOption)
                testCoding(configuredCodec, "", "")
                testCoding(configuredCodec, "f", "Zg==")
                testCoding(configuredCodec, "fo", "Zm8=")
                testCoding(configuredCodec, "foo", "Zm9v")
                testCoding(configuredCodec, "foob", "Zm9vYg==")
                testCoding(configuredCodec, "fooba", "Zm9vYmE=")
                testCoding(configuredCodec, "foobar", "Zm9vYmFy")

                val configuredScheme = "$scheme.withPadding(${paddingOption.name})"

                // at least two symbols are required
                val oneSymbol = listOf("Z", "=", "@")
                for (symbol in oneSymbol) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbol>") {
                        configuredCodec.decode(symbol)
                    }.also { exception ->
                        assertContains(exception.message!!, "Input should have at least 2 symbols for Base64 decoding")
                    }
                }

                // dangling single symbol at the end that does not have bits even for a byte
                val lastDanglingSymbol = listOf("Z=", "Z==", "Z===", "Zm9vZ", "Zm9vZ=", "Zm9vZ==", "Zm9vZ===")
                for (symbols in lastDanglingSymbol) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertEquals("The last unit of input does not have enough bits", exception.message)
                    }
                }

                // incorrect padding

                val redundantPadChar = listOf("Zm9v=", "Zm9vYmFy=")
                for (symbols in redundantPadChar) {
                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertEquals("Redundant pad character at index ${symbols.lastIndex}", exception.message)
                    }
                }

                val missingOnePadChar = listOf("Zg=", "Zg=a", "Zm9vYg=", "Zm9vYg=\u0000")
                for (symbols in missingOnePadChar) {
                    val errorMessage = if (paddingOption == Base64.PaddingOption.ABSENT)
                        "The padding option is set to ABSENT"
                    else
                        "Missing one pad character"

                    assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                        configuredCodec.decode(symbols)
                    }.also { exception ->
                        assertContains(exception.message!!, errorMessage)
                    }
                }

                if (paddingOption == Base64.PaddingOption.ABSENT) {
                    val withPadding = listOf("Zg==", "Zm8=", "Zm9vYg==", "Zm9vYmE=")
                    for (symbols in withPadding) {
                        assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                            configuredCodec.decode(symbols)
                        }.also { exception ->
                            assertContains(exception.message!!, "The padding option is set to ABSENT")
                        }
                    }
                }

                if (paddingOption == Base64.PaddingOption.PRESENT) {
                    val withoutPadding = listOf("Zg", "Zm8", "Zm9vYg", "Zm9vYmE")
                    for (symbols in withoutPadding) {
                        assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                            configuredCodec.decode(symbols)
                        }.also { exception ->
                            assertEquals(
                                "The padding option is set to PRESENT, but the input is not properly padded",
                                exception.message
                            )
                        }
                    }
                }

                if (paddingOption.isAllowedOnDecode()) {
                    val symbolAfterPadding = listOf("Zg==Zg==", "Zg===", "Zm9vYmE==", "Zm9vYmE=a")
                    for (symbols in symbolAfterPadding) {
                        assertFailsWith<IllegalArgumentException>("$configuredScheme <$symbols>") {
                            configuredCodec.decode(symbols)
                        }.also { exception ->
                            assertContains(exception.message!!, "prohibited after the pad character")
                        }
                    }
                }

                // the LSBs at positions 1, 2, 3, 4 are not zero, respectively
                val nonZeroPadBits = listOf("Zm9=", "Zm9vYmG=", "Zk==", "Zm9vYo==")
                for (symbols in nonZeroPadBits) {
                    assertFailsWith<IllegalArgumentException>(("$configuredScheme <$symbols>")) {
                        codec.decode(symbols)
                    }.also { exception ->
                        assertEquals("The pad bits must be zeros", exception.message)
                    }
                }
            }
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

    @Test
    fun encodeSize() {
        for ((codec, _) in codecs) {
            val lineSeparatorChars = if (codec.isMimeScheme) 2 else 0

            val paddingPresent = Base64.PaddingOption.entries.filter { it.isPresentOnEncode() }
            for (paddingOption in paddingPresent) {
                val configuredCodec = codec.withPadding(paddingOption)

                // One line in all schemes

                assertEquals(0, configuredCodec.encodeSize(0))
                assertEquals(4, configuredCodec.encodeSize(1))
                assertEquals(4, configuredCodec.encodeSize(2))
                assertEquals(4, configuredCodec.encodeSize(3))
                assertEquals(8, configuredCodec.encodeSize(4))
                assertEquals(8, configuredCodec.encodeSize(5))
                assertEquals(8, configuredCodec.encodeSize(6))
                assertEquals(12, configuredCodec.encodeSize(7))
                assertEquals(12, configuredCodec.encodeSize(8))
                assertEquals(12, configuredCodec.encodeSize(9))

                // Two lines in mime scheme

                assertEquals(76, configuredCodec.encodeSize(57))
                assertEquals(80 + lineSeparatorChars, configuredCodec.encodeSize(58)) // line separator
                assertEquals(80 + lineSeparatorChars, configuredCodec.encodeSize(59))

                // Three lines in mime scheme

                assertEquals(152 + lineSeparatorChars, configuredCodec.encodeSize(114))
                assertEquals(156 + 2 * lineSeparatorChars, configuredCodec.encodeSize(115)) // line separator
                assertEquals(156 + 2 * lineSeparatorChars, configuredCodec.encodeSize(116))

                // The maximum number of bytes that we can encode

                if (codec.isMimeScheme) {
                    val limit = 1_651_910_496 // lines = 21_17_83_39
                    assertEquals(2_147_483_646, configuredCodec.encodeSize(limit - 1))
                    assertEquals(2_147_483_646, configuredCodec.encodeSize(limit))
                    assertFailsWith<IllegalArgumentException> {
                        configuredCodec.encodeSize(limit + 1) // Int.MAX_VALUE + 3
                    }.also { exception ->
                        assertEquals("Input is too big", exception.message)
                    }
                } else {
                    val limit = 1_610_612_733
                    assertEquals(2_147_483_644, configuredCodec.encodeSize(limit))
                    assertFailsWith<IllegalArgumentException> {
                        configuredCodec.encodeSize(limit + 1) // Int.MAX_VALUE + 1
                    }.also { exception ->
                        assertEquals("Input is too big", exception.message)
                    }
                }

                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(Int.MAX_VALUE)
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }
            }

            val paddingAbsent = Base64.PaddingOption.entries - paddingPresent.toSet()
            for (paddingOption in paddingAbsent) {
                val configuredCodec = codec.withPadding(paddingOption)

                // One line in all schemes

                assertEquals(0, configuredCodec.encodeSize(0))
                assertEquals(2, configuredCodec.encodeSize(1))
                assertEquals(3, configuredCodec.encodeSize(2))
                assertEquals(4, configuredCodec.encodeSize(3))
                assertEquals(6, configuredCodec.encodeSize(4))
                assertEquals(7, configuredCodec.encodeSize(5))
                assertEquals(8, configuredCodec.encodeSize(6))
                assertEquals(10, configuredCodec.encodeSize(7))
                assertEquals(11, configuredCodec.encodeSize(8))
                assertEquals(12, configuredCodec.encodeSize(9))

                // Two lines in mime scheme

                assertEquals(76, configuredCodec.encodeSize(57))
                assertEquals(78 + lineSeparatorChars, configuredCodec.encodeSize(58)) // line separator
                assertEquals(79 + lineSeparatorChars, configuredCodec.encodeSize(59))

                // Three lines in mime scheme

                assertEquals(152 + lineSeparatorChars, configuredCodec.encodeSize(114))
                assertEquals(154 + 2 * lineSeparatorChars, configuredCodec.encodeSize(115)) // line separator
                assertEquals(155 + 2 * lineSeparatorChars, configuredCodec.encodeSize(116))

                // The maximum number of bytes that we can encode

                if (codec.isMimeScheme) {
                    val limit = 1_651_910_496 // lines = 21_17_83_39
                    assertEquals(2_147_483_645, configuredCodec.encodeSize(limit - 1))
                    assertEquals(2_147_483_646, configuredCodec.encodeSize(limit))
                    assertFailsWith<IllegalArgumentException> {
                        configuredCodec.encodeSize(limit + 1) // Int.MAX_VALUE + 1
                    }.also { exception ->
                        assertEquals("Input is too big", exception.message)
                    }
                } else {
                    val limit = 1_610_612_733
                    assertEquals(2_147_483_644, configuredCodec.encodeSize(limit))
                    assertEquals(2_147_483_646, configuredCodec.encodeSize(limit + 1))
                    assertEquals(2_147_483_647, configuredCodec.encodeSize(limit + 2)) // Int.MAX_VALUE
                    assertFailsWith<IllegalArgumentException> {
                        configuredCodec.encodeSize(limit + 3) // Int.MAX_VALUE + 1
                    }.also { exception ->
                        assertEquals("Input is too big", exception.message)
                    }
                }

                assertFailsWith<IllegalArgumentException> {
                    configuredCodec.encodeSize(Int.MAX_VALUE)
                }.also { exception ->
                    assertEquals("Input is too big", exception.message)
                }
            }
        }
    }

    @Test
    fun decodeSize() {
        fun testDecodeSize(codec: Base64, symbols: String, expectedSize: Int) {
            assertEquals(expectedSize, codec.decodeSize(symbols.encodeToByteArray(), 0, symbols.length))
            assertEquals(
                expectedSize,
                codec.decodeSize(
                    if (symbols.isEmpty())
                        ByteArray(10)
                    else
                        ByteArray(symbols.length + 10) { symbols[(it - 5).coerceIn(0, symbols.lastIndex)].code.toByte() },
                    startIndex = 5,
                    endIndex = symbols.length + 5
                )
            )
        }

        for ((codec, _) in codecs) {
            assertFailsWith<IllegalArgumentException> {
                codec.decode(ByteArray(1), 0, 1)
            }.also { exception ->
                assertEquals("Input should have at least 2 symbols for Base64 decoding, startIndex: 0, endIndex: 1", exception.message)
            }
            assertFailsWith<IllegalArgumentException> {
                codec.decode(ByteArray(11), 5, 6)
            }.also { exception ->
                assertEquals("Input should have at least 2 symbols for Base64 decoding, startIndex: 5, endIndex: 6", exception.message)
            }

            testDecodeSize(codec, "", 0)
            testDecodeSize(codec, "Zg==", 1)
            testDecodeSize(codec, "Zg=", 1)
            testDecodeSize(codec, "Zg", 1)
            testDecodeSize(codec, "Zm8=", 2)
            testDecodeSize(codec, "Zm8", 2)
            testDecodeSize(codec, "Zm9v", 3)
            testDecodeSize(codec, "Zm9vYg==", 4)
            testDecodeSize(codec, "Zm9vYg=", 4)
            testDecodeSize(codec, "Zm9vYg", 4)
            testDecodeSize(codec, "Zm9vYmE=", 5)
            testDecodeSize(codec, "Zm9vYmE", 5)
            testDecodeSize(codec, "Zm9vYmFy", 6)

            val longSymbols = "Zm9vYmFy".repeat(76)
            testDecodeSize(codec, longSymbols, 6 * 76)
            testDecodeSize(codec, longSymbols + "Zg==", 6 * 76 + 1)
            testDecodeSize(codec, longSymbols + "Zg=", 6 * 76 + 1)
            testDecodeSize(codec, longSymbols + "Zg", 6 * 76 + 1)

            if (codec.isMimeScheme) {
                testDecodeSize(codec, "Zg==" + longSymbols, 1)
                testDecodeSize(codec, "Zg=" + longSymbols, 1)
                testDecodeSize(codec, "Zg" + longSymbols, 1 + 6 * 76)
            } else {
                testDecodeSize(codec, "Zg==" + longSymbols, 3 + 6 * 76)
                testDecodeSize(codec, "Zg=" + longSymbols, 2 + 6 * 76)
                testDecodeSize(codec, "Zg" + longSymbols, 1 + 6 * 76)
            }
        }
    }
}