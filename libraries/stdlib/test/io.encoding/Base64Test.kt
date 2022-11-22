/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.io.encoding

import kotlin.test.*
import kotlin.io.encoding.Base64

class Base64Test {

    private fun testCoding(codec: Base64, text: String, encodedText: String) {
        val bytes = ByteArray(text.length) { text[it].code.toByte() }
        assertEquals(encodedText, codec.encode(bytes))

        val encodedBytes = ByteArray(encodedText.length) { encodedText[it].code.toByte() }
        assertContentEquals(encodedBytes, codec.encodeToByteArray(bytes))

        assertContentEquals(bytes, codec.decode(encodedText))
        assertContentEquals(bytes, codec.decodeFromByteArray(encodedBytes))
    }

    @Test
    fun base64() {
        fun testBase64(text: String, encodedText: String) {
            testCoding(Base64, text, encodedText)
        }

        testBase64("", "")
        testBase64("f", "Zg==")
        testBase64("fo", "Zm8=")
        testBase64("foo", "Zm9v")
        testBase64("foob", "Zm9vYg==")
        testBase64("fooba", "Zm9vYmE=")
        testBase64("foobar", "Zm9vYmFy")
        // 0b11111011, 0b11110000
        testBase64("\u00FB\u00F0", "+/A=")

        // the padded bits are allowed to be non-zero
        assertEquals("fo", Base64.decode("Zm9=").decodeToString())
    }

    @Test
    fun base64Url() {
        fun testBase64(text: String, encodedText: String) {
            testCoding(Base64.UrlSafe, text, encodedText)
        }

        testBase64("", "")
        testBase64("f", "Zg==")
        testBase64("fo", "Zm8=")
        testBase64("foo", "Zm9v")
        testBase64("foob", "Zm9vYg==")
        testBase64("fooba", "Zm9vYmE=")
        testBase64("foobar", "Zm9vYmFy")
        // 0b11111011, 0b11110000
        testBase64("\u00FB\u00F0", "-_A=")

        // the padded bits are allowed to be non-zero
        assertEquals("fo", Base64.UrlSafe.decode("Zm9=").decodeToString())
    }
}