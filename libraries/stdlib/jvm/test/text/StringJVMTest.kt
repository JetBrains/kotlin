/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.collections.assertArrayNotSameButEquals
import test.platformNull
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
        assertEquals("1,234,567.890", "%,.3f".format(null,           1234567.890))
        assertEquals("1,234,567.890", "%,.3f".format(platformNull<Locale>(), 1234567.890))

        assertEquals("1,234,567.890", String.format(Locale.ENGLISH, "%,.3f", 1234567.890))
        assertEquals("1.234.567,890", String.format(Locale.GERMAN,  "%,.3f", 1234567.890))
        assertEquals("1 234 567,890", String.format(Locale("fr"),   "%,.3f", 1234567.890))
        assertEquals("1,234,567.890", String.format(null,           "%,.3f", 1234567.890))
        assertEquals("1,234,567.890", String.format(platformNull<Locale>(), "%,.3f", 1234567.890))
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

    @Test fun capitalize() {
        fun testCapitalize(expected: String, string: String) {
            assertEquals(expected, string.capitalize())
            assertEquals(expected, string.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        }
        // Case mapping that results in multiple characters (validating Character.toUpperCase was not used).
        assertEquals("SSßß", "ßßß".capitalize())
        assertEquals("Ssßß", "ßßß".replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })

        // Case mapping where title case is different than uppercase and so Character.toTitleCase is preferred.
        testCapitalize("ǲǳǳ", "ǳǳǳ")
        testCapitalize("ǱǱǱ", "ǱǱǱ")
    }

    @Test fun decapitalize() {
        fun testDecapitalize(expected: String, string: String) {
            assertEquals(expected, string.decapitalize())
            assertEquals(expected, string.replaceFirstChar { it.lowercase(Locale.getDefault()) })
        }
        // Case mapping where title case is different than uppercase.
        testDecapitalize("ǳǳǳ", "Ǳǳǳ")
        testDecapitalize("ǳǳǳ", "ǲǳǳ")
    }

    @Test fun capitalizeLocale() {
        fun testCapitalizeLocale(expected: String, string: String, locale: Locale) {
            assertEquals(expected, string.capitalize(locale))
            assertEquals(expected, string.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() })
        }
        testCapitalizeLocale("ABC", "ABC", Locale.US)
        testCapitalizeLocale("Abc", "Abc", Locale.US)
        testCapitalizeLocale("Abc", "abc", Locale.US)

        // Locale-specific case mappings.
        testCapitalizeLocale("İii", "iii", Locale("tr", "TR"))
        testCapitalizeLocale("Iii", "iii", Locale.US)

        // Case mapping that results in multiple characters (validating Character.toUpperCase was not used).
        assertEquals("SSßß", "ßßß".capitalize(Locale.US))
        assertEquals("Ssßß", "ßßß".replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() })

        // Case mapping where title case is different than uppercase and so Character.toTitleCase is preferred.
        testCapitalizeLocale("ǲǳǳ", "ǳǳǳ", Locale.US)
        testCapitalizeLocale("ǱǱǱ", "ǱǱǱ", Locale.US)
    }

    @Test fun decapitalizeLocale() {
        fun testDecapitalizeLocale(expected: String, string: String, locale: Locale) {
            assertEquals(expected, string.decapitalize(locale))
            assertEquals(expected, string.replaceFirstChar { it.lowercase(locale) })
        }
        testDecapitalizeLocale("aBC", "ABC", Locale.US)
        testDecapitalizeLocale("abc", "Abc", Locale.US)
        testDecapitalizeLocale("abc", "abc", Locale.US)

        // Locale-specific case mappings.
        testDecapitalizeLocale("ıII", "III", Locale("tr", "TR"))
        testDecapitalizeLocale("iII", "III", Locale.US)

        // Case mapping where title case is different than uppercase.
        testDecapitalizeLocale("ǳǳǳ", "Ǳǳǳ", Locale.US)
        testDecapitalizeLocale("ǳǳǳ", "ǲǳǳ", Locale.US)
    }

    @Test
    fun stringToBoolean() {
        assertFalse(platformNull<String>().toBoolean())
    }

    @Test
    fun stringEquals() {
        assertFalse(platformNull<String>().equals("sample", ignoreCase = false))
        assertFalse(platformNull<String>().equals("sample", ignoreCase = true))
        assertFalse("sample".equals(platformNull<String>(), ignoreCase = false))
        assertFalse("sample".equals(platformNull(), ignoreCase = true))
        assertTrue(platformNull<String>().equals(platformNull(), ignoreCase = true))
        assertTrue(platformNull<String>().equals(platformNull<String>(), ignoreCase = false))
    }
}
