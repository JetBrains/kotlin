/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.io.encoding

import samples.*
import kotlin.io.encoding.*
import kotlin.test.*

@OptIn(ExperimentalEncodingApi::class)
class Base64Samples {
    @Sample
    fun encodeAndDecode() {
        val encoded = Base64.Default.encode("Hello, World!".encodeToByteArray())
        assertPrints(encoded, "SGVsbG8sIFdvcmxkIQ==")

        val decoded = Base64.Default.decode(encoded)
        assertPrints(decoded.decodeToString(), "Hello, World!")
    }

    @Sample
    fun padding() {
        // "base".length == 4, which is not multiple of 3;
        // base64-encoded data padded with 4 bits
        assertPrints(Base64.Default.encode("base".encodeToByteArray()), "YmFzZQ==")
        // "base6".length == 5, which is not multiple of 3;
        // base64-encoded data padded with 2 bits
        assertPrints(Base64.Default.encode("base6".encodeToByteArray()), "YmFzZTY=")
        // "base64".length == 6, which is the multiple of 3, so no padding is required
        assertPrints(Base64.Default.encode("base64".encodeToByteArray()), "YmFzZTY0")
    }

    @Sample
    fun encodingDifferences() {
        // Default encoding uses '/' and '+' as the last two characters of the Base64 alphabet
        assertPrints(Base64.Default.encode(byteArrayOf(-1, 0, -2, 0)), "/wD+AA==")
        // Mime's alphabet is the same as Default's
        assertPrints(Base64.Mime.encode(byteArrayOf(-1, 0, -2, 0)), "/wD+AA==")
        // UrlSafe encoding uses '_' and '-' as the last two Base64 alphabet characters
        assertPrints(Base64.UrlSafe.encode(byteArrayOf(-1, 0, -2, 0)), "_wD-AA==")

        // UrlSafe uses `-` and `_`, so the following string could not be decoded
        assertFailsWith<IllegalArgumentException> {
            Base64.UrlSafe.decode("/wD+AA==")
        }
    }

    @Sample
    fun defaultEncodingSample() {
        val encoded = Base64.Default.encode("Hello? :> ".encodeToByteArray())
        assertPrints(encoded, "SGVsbG8/IDo+IA==")
    }

    @Sample
    fun mimeEncodingSample() {
        val encoded = Base64.Mime.encode("Hello? :> ".encodeToByteArray())
        assertPrints(encoded, "SGVsbG8/IDo+IA==")

        // Mime encoding ignores all characters not belonging to its alphabet
        val decoded = Base64.Mime.decode("Y@{mFz!Z!TY}0")
        assertPrints(decoded.decodeToString(), "base64")

        // let's encode some long text
        val sourceText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa" +
                " qui officia deserunt mollit anim id est laborum."

        // each line consists of 76 characters
        val expectedText = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdCwg\r\n" +
                "c2VkIGRvIGVpdXNtb2QgdGVtcG9yIGluY2lkaWR1bnQgdXQgbGFib3JlIGV0IGRvbG9yZSBtYWdu\r\n" +
                "YSBhbGlxdWEuIFV0IGVuaW0gYWQgbWluaW0gdmVuaWFtLCBxdWlzIG5vc3RydWQgZXhlcmNpdGF0\r\n" +
                "aW9uIHVsbGFtY28gbGFib3JpcyBuaXNpIHV0IGFsaXF1aXAgZXggZWEgY29tbW9kbyBjb25zZXF1\r\n" +
                "YXQuIER1aXMgYXV0ZSBpcnVyZSBkb2xvciBpbiByZXByZWhlbmRlcml0IGluIHZvbHVwdGF0ZSB2\r\n" +
                "ZWxpdCBlc3NlIGNpbGx1bSBkb2xvcmUgZXUgZnVnaWF0IG51bGxhIHBhcmlhdHVyLiBFeGNlcHRl\r\n" +
                "dXIgc2ludCBvY2NhZWNhdCBjdXBpZGF0YXQgbm9uIHByb2lkZW50LCBzdW50IGluIGN1bHBhIHF1\r\n" +
                "aSBvZmZpY2lhIGRlc2VydW50IG1vbGxpdCBhbmltIGlkIGVzdCBsYWJvcnVtLg=="

        assertTrue(Base64.Mime.encode(sourceText.encodeToByteArray()).contentEquals(expectedText))
    }

    @Sample
    fun urlSafeEncodingSample() {
        val encoded = Base64.UrlSafe.encode("Hello? :> ".encodeToByteArray())
        assertPrints(encoded, "SGVsbG8_IDo-IA==")
    }

    @Sample
    fun encodeToByteArraySample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = Base64.encodeToByteArray(data)
        assertTrue(encoded.contentEquals("/wD+AP0=".encodeToByteArray()))

        val encodedFromSubRange = Base64.encodeToByteArray(data, startIndex = 1, endIndex = 3)
        assertTrue(encodedFromSubRange.contentEquals("AP4=".encodeToByteArray()))
    }

    @Sample
    fun encodeIntoByteArraySample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)
        val outputBuffer = ByteArray(1024)

        var bufferPosition = 0
        // encode data into buffer using Base64 encoding
        // and keep track of the number of bytes written
        bufferPosition += Base64.encodeIntoByteArray(data, outputBuffer)
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "/wD+AP0=")

        outputBuffer[bufferPosition++] = '|'.code.toByte()

        // encode data subrange to the buffer, writing it from the given offset
        bufferPosition += Base64.encodeIntoByteArray(
            data,
            destination = outputBuffer,
            destinationOffset = bufferPosition,
            startIndex = 1,
            endIndex = 3
        )
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "/wD+AP0=|AP4=")
    }

    @Sample
    fun encodeToStringSample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = Base64.encode(data)
        assertPrints(encoded, "/wD+AP0=")

        val encodedFromSubRange = Base64.encode(data, startIndex = 1, endIndex = 3)
        assertPrints(encodedFromSubRange, "AP4=")
    }

    @Sample
    fun encodeToAppendableSample() {
        val data = byteArrayOf(-1, 0, -2, 0, -3)

        val encoded = buildString {
            append("{ \"data\": \"")
            Base64.encodeToAppendable(data, destination = this)
            append("\" }")
        }
        assertPrints(encoded, "{ \"data\": \"/wD+AP0=\" }")

        val encodedFromSubRange = buildString {
            append("{ \"data\": \"")
            Base64.encodeToAppendable(data, destination = this, startIndex = 1, endIndex = 3)
            append("\" }")
        }
        assertPrints(encodedFromSubRange, "{ \"data\": \"AP4=\" }")
    }

    @Sample
    fun decodeFromByteArraySample() {
        // get a byte array filled with data, for instance, by reading it from a network
        val data = byteArrayOf(0x61, 0x47, 0x56, 0x73, 0x62, 0x47, 0x38, 0x3d)
        // decode data from the array
        assertTrue(Base64.decode(data).contentEquals("hello".encodeToByteArray()))

        val dataInTheMiddle = byteArrayOf(0x00, 0x00, 0x61, 0x47, 0x56, 0x73, 0x62, 0x47, 0x38, 0x3d, 0x00, 0x00)
        // decode base64-encoded data from the middle of a buffer
        val decoded = Base64.decode(dataInTheMiddle, startIndex = 2, endIndex = 10)
        assertTrue(decoded.contentEquals("hello".encodeToByteArray()))
    }

    @Sample
    fun decodeFromStringSample() {
        assertTrue(Base64.decode("/wD+AP0=").contentEquals(byteArrayOf(-1, 0, -2, 0, -3)))

        // padding character may be omitted
        assertTrue(Base64.decode("/wD+AP0").contentEquals(byteArrayOf(-1, 0, -2, 0, -3)))

        val embeddedB64 = "Data is: \"/wD+AP0=\""
        // find '"' indices and extract base64-encoded data in between
        val decoded = Base64.decode(
            embeddedB64,
            startIndex = embeddedB64.indexOf('"') + 1,
            endIndex = embeddedB64.lastIndexOf('"')
        )
        assertTrue(decoded.contentEquals(byteArrayOf(-1, 0, -2, 0, -3)))
    }

    @Sample
    fun decodeIntoByteArraySample() {
        val outputBuffer = ByteArray(1024)
        val inputChunks = listOf("Q2h1bmsx", "U2Vjb25kQ2h1bms=", "Y2h1bmsjMw==")

        var bufferPosition = 0
        val chunkIterator = inputChunks.iterator()
        while (bufferPosition < outputBuffer.size && chunkIterator.hasNext()) {
            val chunk = chunkIterator.next()
            // fill buffer with data decoded from base64-encoded chunks
            // and increment current position by the number of decoded bytes
            bufferPosition += Base64.decodeIntoByteArray(
                chunk,
                destination = outputBuffer,
                destinationOffset = bufferPosition
            )
        }
        // consume outputBuffer
        assertPrints(outputBuffer.decodeToString(endIndex = bufferPosition), "Chunk1SecondChunkchunk#3")
    }

    @Sample
    fun decodeIntoByteArrayFromByteArraySample() {
        val outputBuffer = ByteArray(1024)
        // {data:\"ZW5jb2RlZA==\"}
        val data = byteArrayOf(
            123, 100, 97, 116, 97,
            58, 34, 90, 87, 53, 106,
            98, 50, 82, 108, 90, 65,
            61, 61, 34, 125
        )
        val from = data.indexOf('"'.code.toByte()) + 1
        val until = data.lastIndexOf('"'.code.toByte())

        // decode subrange of input data into buffer and remember how many bytes were written
        val bytesWritten = Base64.decodeIntoByteArray(data, destination = outputBuffer, startIndex = from, endIndex = until)
        assertPrints(outputBuffer.decodeToString(endIndex = bytesWritten), "encoded")
    }
}
