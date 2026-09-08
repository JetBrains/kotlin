/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.codepoints

import kotlin.text.unicode.*
import kotlin.test.*

@OptIn(ExperimentalUnicodeApi::class)
class CodePointJVMTest {
    @Test
    fun conversions() {
        val allCodePoints = CodePointTest.bmpCodePoints + CodePointTest.supplementaryCodePoints

        for (c in allCodePoints) {
            assertTrue(Character.isValidCodePoint(c.code))
            assertEquals(Character.charCount(c.code), c.size)
            if (c.size == 2) {
                c.toSurrogatePair { high, low ->
                    assertEquals(Character.lowSurrogate(c.code), low)
                    assertEquals(Character.highSurrogate(c.code), high)
                    assertContentEquals(Character.toChars(c.code), c.toCharArray())
                }
            }
        }
    }

    @Test
    fun appendCodePoint() {
        val sb1 = StringBuilder()
        val sb2 = StringBuilder()

        repeat(1000) {
            val cp = (CodePoint.MIN_VALUE..CodePoint.MAX_VALUE).random()
            sb1.appendCodePoint(cp)
            sb2.appendCodePoint(cp.code)
        }
        assertEquals(sb2.toString(), sb1.toString())
    }
}
