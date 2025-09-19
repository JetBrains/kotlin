/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.regex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecursiveRegularExpressions {
    @Test
    fun eagerRegex() {
        val match = Regex("([ab])*").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun repeatedNewLine() {
        val res = Regex("(^$)*LINE", RegexOption.MULTILINE).findAll("\n\n\nLINE").toList()
        assertEquals(1, res.size)
        assertEquals("LINE", res[0].value)
    }

    @Test
    fun repeatedEmptySet() {
        assertTrue(Regex("(){3,5}S").matches("S"))
        assertTrue(Regex("(){3,}S").matches("S"))
        assertTrue(Regex("()+S").matches("S"))
        assertTrue(Regex("()?S").matches("S"))
        assertTrue(Regex("()*S").matches("S"))
    }

    @Test
    fun possessiveRegex() {
        val match = Regex("([ab])*+").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun reluctantRegex() {
        val match = Regex("([ab])*?").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun repeatedReluctantEmptySet() {
        assertTrue(Regex("(){3,5}?S").matches("S"))
        assertTrue(Regex("(){3,}?S").matches("S"))
        assertTrue(Regex("()+?S").matches("S"))
        assertTrue(Regex("()??S").matches("S"))
        assertTrue(Regex("()*?S").matches("S"))
    }

    @Test
    fun repeatedReluctantNewLine() {
        val res = Regex("(^$)*?LINE", RegexOption.MULTILINE).findAll("\n\n\nLINE").toList()
        assertEquals(1, res.size)
        assertEquals("LINE", res[0].value)
    }

    @Test
    fun reluctantNonTrivialGroup() {
        val re = Regex("(aa|a)+?a")
        assertTrue(re.matches("aa"))
    }
}
