/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.decodingWith
import kotlin.io.encoding.encodingWith
import kotlin.test.*

class Base64IOStreamTest {

    private fun testCoding(base64: Base64, text: String, encodedText: String) {
        val encodedBytes = ByteArray(encodedText.length) { encodedText[it].code.toByte() }
        val bytes = ByteArray(text.length) { text[it].code.toByte() }
        encodedBytes.inputStream().decodingWith(base64).use { inputStream ->
            assertEquals(text, inputStream.reader().readText())
        }
        encodedBytes.inputStream().decodingWith(base64).use { inputStream ->
            assertContentEquals(bytes, inputStream.readBytes())
        }
        ByteArrayOutputStream().let { outputStream ->
            outputStream.encodingWith(base64).use {
                it.write(bytes)
            }
            assertContentEquals(encodedBytes, outputStream.toByteArray())
        }
    }

    @Test
    fun base64() {
        fun testBase64(text: String, encodedText: String) {
            testCoding(Base64, text, encodedText)
            testCoding(Base64.Mime, text, encodedText)
        }

        testBase64("", "")
        testBase64("f", "Zg==")
        testBase64("fo", "Zm8=")
        testBase64("foo", "Zm9v")
        testBase64("foob", "Zm9vYg==")
        testBase64("fooba", "Zm9vYmE=")
        testBase64("foobar", "Zm9vYmFy")
    }

    @Test
    fun readDifferentOffsetAndLength() {
        val repeat = 10_000
        val symbols = "Zm9vYmFy".repeat(repeat) + "Zm8="
        val expected = "foobar".repeat(repeat) + "fo"

        val bytes = ByteArray(expected.length)

        symbols.byteInputStream().decodingWith(Base64).use { input ->
            var read = 0
            repeat(6) {
                bytes[read++] = input.read().toByte()
            }

            var toRead = 1
            while (read < bytes.size) {
                val length = minOf(toRead, bytes.size - read)
                val result = input.read(bytes, read, length)

                assertEquals(length, result)

                read += result
                toRead += toRead * 10 / 9
            }

            assertEquals(-1, input.read(bytes))
            assertEquals(-1, input.read())
            assertEquals(expected, bytes.decodeToString())
        }
    }

    @Test
    fun readDifferentOffsetAndLengthMime() {
        val repeat = 10_000
        val symbols = ("Zm9vYmFy".repeat(repeat) + "Zm8=").chunked(76).joinToString(separator = "\r\n")
        val expected = "foobar".repeat(repeat) + "fo"

        val bytes = ByteArray(expected.length)

        symbols.byteInputStream().decodingWith(Base64.Mime).use { input ->
            var read = 0
            repeat(6) {
                bytes[read++] = input.read().toByte()
            }

            var toRead = 1
            while (read < bytes.size) {
                val length = minOf(toRead, bytes.size - read)
                val result = input.read(bytes, read, length)

                assertEquals(length, result)

                read += result
                toRead += toRead * 10 / 9
            }

            assertEquals(-1, input.read(bytes))
            assertEquals(-1, input.read())
            assertEquals(expected, bytes.decodeToString())
        }
    }

    @Test
    fun writeDifferentOffsetAndLength() {
        val repeat = 10_000
        val bytes = ("foobar".repeat(repeat) + "fo").encodeToByteArray()
        val expected = "Zm9vYmFy".repeat(repeat) + "Zm8="

        val underlying = ByteArrayOutputStream()

        underlying.encodingWith(Base64).use { output ->
            var written = 0
            repeat(8) {
                output.write(bytes[written++].toInt())
            }
            var toWrite = 1
            while (written < bytes.size) {
                val length = minOf(toWrite, bytes.size - written)
                output.write(bytes, written, length)

                written += length
                toWrite += toWrite * 10 / 9
            }
        }

        assertEquals(expected, underlying.toString())
    }

    @Test
    fun writeDifferentOffsetAndLengthMime() {
        val repeat = 10_000
        val bytes = ("foobar".repeat(repeat) + "fo").encodeToByteArray()
        val expected = ("Zm9vYmFy".repeat(repeat) + "Zm8=").chunked(76).joinToString(separator = "\r\n")

        val underlying = ByteArrayOutputStream()

        underlying.encodingWith(Base64.Mime).use { output ->
            var written = 0
            repeat(8) {
                output.write(bytes[written++].toInt())
            }
            var toWrite = 1
            while (written < bytes.size) {
                val length = minOf(toWrite, bytes.size - written)
                output.write(bytes, written, length)

                written += length
                toWrite += toWrite * 10 / 9
            }
        }

        assertEquals(expected, underlying.toString())
    }


    @Test
    fun inputStreamClosesUnderlying() {
        val underlying = object : InputStream() {
            var isClosed: Boolean = false

            override fun close() {
                isClosed = true
                super.close()
            }

            override fun read(): Int {
                return 0
            }
        }
        val wrapper = underlying.decodingWith(Base64)
        wrapper.close()
        assertTrue(underlying.isClosed)
    }

    @Test
    fun outputStreamClosesUnderlying() {
        val underlying = object : OutputStream() {
            var isClosed: Boolean = false

            override fun close() {
                isClosed = true
                super.close()
            }

            override fun write(b: Int) {
                // ignore
            }
        }
        val wrapper = underlying.encodingWith(Base64)
        wrapper.close()
        assertTrue(underlying.isClosed)
    }


    @Test
    fun correctPadding() {
        val inputStream = "Zg==Zg==".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())

            // in the wrapped IS the chars after the padding are not consumed
            assertContentEquals("Zg==".toByteArray(), inputStream.readBytes())
        }

        assertFailsWith<IOException> {
            wrapper.read()
        }
    }


    @Test
    fun correctPaddingMime() {
        val inputStream = "Zg==Zg==".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64.Mime)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())

            // in the wrapped IS the chars after the padding are not consumed
            assertContentEquals("Zg==".toByteArray(), inputStream.readBytes())
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun illegalSymbol() {
        val inputStream = "Zm\u00FF9vYg==".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64)

        wrapper.use {
            // one group of 4 symbols is read for decoding, that group includes illegal '\u00FF'
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun illegalSymbolMime() {
        val inputStream = "Zm\u00FF9vYg==".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64.Mime)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun incorrectPadding() {
        for (base64 in listOf(Base64, Base64.Mime)) {
            val inputStream = "Zm9vZm=9v".byteInputStream()
            val wrapper = inputStream.decodingWith(base64)

            wrapper.use {
                assertEquals('f'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('o'.code, it.read())

                // the second group is incorrectly padded
                assertFailsWith<IllegalArgumentException> {
                    it.read()
                }
            }

            // closed
            assertFailsWith<IOException> {
                wrapper.read()
            }
        }
    }

    @Test
    fun withoutPadding() {
        for (base64 in listOf(Base64, Base64.Mime)) {
            val inputStream = "Zm9vYg".byteInputStream()
            val wrapper = inputStream.decodingWith(base64)

            wrapper.use {
                assertEquals('f'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('o'.code, it.read())
                assertEquals('b'.code, it.read())
                assertEquals(-1, it.read())
                assertEquals(-1, it.read())
            }

            // closed
            assertFailsWith<IOException> {
                wrapper.read()
            }
        }
    }

    @Test
    fun separatedPadSymbols() {
        val inputStream = "Zm9vYg=[,.|^&*@#]=".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())

            // the second group contains illegal symbols
            assertFailsWith<IllegalArgumentException> {
                it.read()
            }
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }

    @Test
    fun separatedPadSymbolsMime() {
        val inputStream = "Zm9vYg=[,.|^&*@#]=".byteInputStream()
        val wrapper = inputStream.decodingWith(Base64.Mime)

        wrapper.use {
            assertEquals('f'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('o'.code, it.read())
            assertEquals('b'.code, it.read())
            assertEquals(-1, it.read())
            assertEquals(-1, it.read())
        }

        // closed
        assertFailsWith<IOException> {
            wrapper.read()
        }
    }
}
