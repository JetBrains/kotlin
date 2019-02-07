/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.collections.assertArrayNotSameButEquals
import java.util.*
import kotlin.test.*


class StringJVMTest {

    @Test fun testSplitByPattern() = withOneCharSequenceArg("ab1cd2def3") { s ->
        val isDigit = "\\d".toRegex()
        assertEquals(listOf("ab", "cd", "def", ""), s.split(isDigit))
        assertEquals(listOf("ab", "cd", "def3"), s.split(isDigit, 3))

        // deprecation replacement equivalence
        assertEquals("\\d".toPattern().split(s).toList(), s.split("\\d".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().toList())

        assertFails {
            s.split(isDigit, -1)
        }
    }

    @Test fun sliceCharSequenceFails() = withOneCharSequenceArg { arg1 ->
        assertFails {
            arg1("abc").slice(1..4)
        }
        assertFails {
            arg1("ABCDabcd").slice(listOf(10))
        }
    }

    @Test fun formatter() {
        assertEquals("12", "%d%d".format(1, 2))
        assertEquals("12", String.format("%d%d", 1, 2))

        assertEquals("1,234,567.890", "%,.3f".format(Locale.ENGLISH, 1234567.890))
        assertEquals("1.234.567,890", "%,.3f".format(Locale.GERMAN,  1234567.890))
        assertEquals("1 234 567,890", "%,.3f".format(Locale("fr"),   1234567.890))
        assertEquals("1,234,567.890", String.format(Locale.ENGLISH, "%,.3f", 1234567.890))
        assertEquals("1.234.567,890", String.format(Locale.GERMAN,  "%,.3f", 1234567.890))
        assertEquals("1 234 567,890", String.format(Locale("fr"),   "%,.3f", 1234567.890))
    }

    @Test fun toByteArrayEncodings() {
        val s = "hello®"
        assertEquals(String(s.toByteArray()), String(s.toByteArray(Charsets.UTF_8)))
    }

    @Test fun toCharArray() {
        val s = "hello"
        val chars = s.toCharArray()
        assertArrayNotSameButEquals(charArrayOf('h', 'e', 'l', 'l', 'o'), chars)

        val buffer = CharArray(4)
        s.toCharArray(buffer, 2, 1, 3)
        assertArrayNotSameButEquals(charArrayOf('\u0000', '\u0000', 'e', 'l'), buffer)
    }


    @Test fun charsets() {
        assertEquals("UTF-32", Charsets.UTF_32.name())
        assertEquals("UTF-32LE", Charsets.UTF_32LE.name())
        assertEquals("UTF-32BE", Charsets.UTF_32BE.name())
    }

    @ExperimentalStdlibApi
    @Test fun capitalizeLocale() {
        assertEquals("ABC", "ABC".capitalize(Locale.US))
        assertEquals("Abc", "Abc".capitalize(Locale.US))
        assertEquals("Abc", "abc".capitalize(Locale.US))

        // Locale-specific case mappings.
        assertEquals("İii", "iii".capitalize(Locale("tr", "TR")))
        assertEquals("Iii", "iii".capitalize(Locale.US))

        // Case mapping that results in multiple characters (validating Character.toUpperCase was not used).
        assertEquals("SSßß", "ßßß".capitalize(Locale.US))

        // Case mapping where title case is different than uppercase and so Character.toTitleCase is preferred.
        assertEquals("ǲǳǳ", "ǳǳǳ".capitalize(Locale.US))
    }

    @ExperimentalStdlibApi
    @Test fun decapitalizeLocale() {
        assertEquals("aBC", "ABC".decapitalize(Locale.US))
        assertEquals("abc", "Abc".decapitalize(Locale.US))
        assertEquals("abc", "abc".decapitalize(Locale.US))

        // Locale-specific case mappings.
        assertEquals("ıII", "III".decapitalize(Locale("tr", "TR")))
        assertEquals("iII", "III".decapitalize(Locale.US))
    }
}
