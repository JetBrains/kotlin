/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*
import kotlin.text.codec.Base16
import kotlin.text.codec.Base32
import kotlin.text.codec.Base64
import kotlin.text.codec.BaseNCodec

class BaseNCodecTest {

    private fun testCoding(codec: BaseNCodec, text: String, encodedText: String) {
        val bytes = ByteArray(text.length) { text[it].code.toByte() }
        assertEquals(encodedText, codec.encodeToString(bytes))

        val encodedBytes = ByteArray(encodedText.length) { encodedText[it].code.toByte() }
        assertContentEquals(encodedBytes, codec.encode(bytes))

        assertContentEquals(bytes, codec.decodeFromString(encodedText))
        assertContentEquals(bytes, codec.decode(encodedBytes))
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
    }

    @Test
    fun base32() {
        fun testBase32(text: String, encodedText: String) {
            testCoding(Base32, text, encodedText)
        }

        testBase32("", "")
        testBase32("f", "MY======")
        testBase32("fo", "MZXQ====")
        testBase32("foo", "MZXW6===")
        testBase32("foob", "MZXW6YQ=")
        testBase32("fooba", "MZXW6YTB")
        testBase32("foobar", "MZXW6YTBOI======")
    }

    @Test
    fun base32Hex() {
        fun testBase32Hex(text: String, encodedText: String) {
            testCoding(Base32(extendedHex = true), text, encodedText)
        }

        testBase32Hex("", "")
        testBase32Hex("f", "CO======")
        testBase32Hex("fo", "CPNG====")
        testBase32Hex("foo", "CPNMU===")
        testBase32Hex("foob", "CPNMUOG=")
        testBase32Hex("fooba", "CPNMUOJ1")
        testBase32Hex("foobar", "CPNMUOJ1E8======")
    }


    @Test
    fun base16() {
        fun testBase16(text: String, encodedText: String) {
            testCoding(Base16, text, encodedText)
        }

        testBase16("", "")
        testBase16("f", "66")
        testBase16("fo", "666F")
        testBase16("foo", "666F6F")
        testBase16("foob", "666F6F62")
        testBase16("fooba", "666F6F6261")
        testBase16("foobar", "666F6F626172")
    }

    @Test
    fun base16Lower() {
        fun testBase16Lower(text: String, encodedText: String) {
            testCoding(Base16(lowercase = true), text, encodedText)
        }

        testBase16Lower("", "")
        testBase16Lower("f", "66")
        testBase16Lower("fo", "666f")
        testBase16Lower("foo", "666f6f")
        testBase16Lower("foob", "666f6f62")
        testBase16Lower("fooba", "666f6f6261")
        testBase16Lower("foobar", "666f6f626172")
    }
}