/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class HexTest {

    @Test
    fun formatAndParse() {
        val hex = Hex(separator = ", ", prefix = "#")
        val bytes = byteArrayOf(0, 1, 2, 3, 124, 125, 126, 127)

        val hexString = hex.format(bytes)
        assertEquals("#00, #01, #02, #03, #7c, #7d, #7e, #7f", hexString)

        assertContentEquals(bytes, hex.parse(hexString))
    }

    @Test
    fun formatAndParseUpperCase() {
        val hex = Hex(separator = ":", upperCase = true)
        val bytes = byteArrayOf(0, 1, 2, 3, 124, 125, 126, 127)

        val hexString = hex.format(bytes)
        assertEquals("00:01:02:03:7C:7D:7E:7F", hexString)

        assertContentEquals(bytes, hex.parse(hexString))
    }

    @Test
    fun toHexDigits() {
        assertEquals("00000007", 7.toHexDigits())
        assertEquals("fffffff9", (-7).toHexDigits())
        assertEquals("FFFFFFF9", (-7).toHexDigits().uppercase())

        assertEquals("00000007", 7.toHexDigitsWithBytes())
        assertEquals("fffffff9", (-7).toHexDigitsWithBytes())
        assertEquals("FFFFFFF9", (-7).toHexDigitsWithBytes().uppercase())
    }

    @Test
    fun hexDigitsToInt() {
        assertEquals(7, "00000007".hexDigitsToInt())
        assertEquals(-7, "fffffff9".hexDigitsToInt())
        assertEquals(-7, "FFFFFFF9".hexDigitsToInt())
    }
}