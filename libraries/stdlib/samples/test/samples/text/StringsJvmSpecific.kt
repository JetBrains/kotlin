/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*
import kotlin.test.*
import java.util.*
import java.util.regex.*

class StringsJvmSpecific {
    @Sample
    fun lowercaseLocale() {
        assertPrints("KOTLIN".lowercase(), "kotlin")
        val turkishLocale = Locale.forLanguageTag("tr")
        assertPrints("KOTLIN".lowercase(turkishLocale), "kotlın")
    }

    @Sample
    fun uppercaseLocale() {
        assertPrints("Kotlin".uppercase(), "KOTLIN")
        val turkishLocale = Locale.forLanguageTag("tr")
        assertPrints("Kotlin".uppercase(turkishLocale), "KOTLİN")
    }

    @Sample
    fun toPattern() {
        val string = "Kotlin [1-9]+\\.[0-9]\\.[0-9]+"
        val pattern = string.toPattern(Pattern.CASE_INSENSITIVE)
        assertPrints(pattern.pattern(), string)
        assertTrue(pattern.flags() == Pattern.CASE_INSENSITIVE)
        assertTrue(pattern.matcher("Kotlin 2.1.255").matches())
        assertTrue(pattern.matcher("kOtLiN 21.0.1").matches())
        assertFalse(pattern.matcher("Java 21.0.1").matches())
        assertFalse(pattern.matcher("Kotlin 2.0").matches())

        // the given regex is malformed
        assertFails { "[0-9".toPattern(Pattern.CASE_INSENSITIVE) }
    }

    @Sample
    fun formatWithLocaleExtension() {
        // format with German conventions
        val withGermanThousandsSeparator = "%,d".format(Locale.GERMANY, 12345)
        assertPrints(withGermanThousandsSeparator, "12.345")
        // 12.345

        // format with US conventions
        val withUSThousandsSeparator = "%,d".format(Locale.US, 12345)
        assertPrints(withUSThousandsSeparator, "12,345")
    }

    @Sample
    fun formatWithLocaleStatic() {
        // format with German conventions
        val withGermanThousandsSeparator = String.format(Locale.GERMANY, "%,d", 12345)
        assertPrints(withGermanThousandsSeparator, "12.345")

        // format with US conventions
        val withUSThousandsSeparator = String.format(Locale.US, "%,d", 12345)
        assertPrints(withUSThousandsSeparator, "12,345")
    }

    @Sample
    fun formatStatic() {
        // format negative number in parentheses
        val negativeNumberInParentheses = String.format("%(d means %1\$d", -31416)
        assertPrints(negativeNumberInParentheses, "(31416) means -31416")
    }

    @Sample
    fun formatExtension() {
        // format negative number in parentheses
        val negativeNumberInParentheses = "%(d means %1\$d".format(-31416)
        assertPrints(negativeNumberInParentheses, "(31416) means -31416")
    }

    @Sample
    fun codePointAt() {
        val str = "abc"
        // 'a'.code == 97
        assertPrints(str.codePointAt(0).toString(), "97")
        // 'b'.code == 98
        assertPrints(str.codePointAt(1).toString(), "98")
        // 'c'.code == 99
        assertPrints(str.codePointAt(2).toString(), "99")
        // 3 is out of the str bounds
        assertFailsWith<IndexOutOfBoundsException> { str.codePointAt(3) }
        // index is negative
        assertFailsWith<IndexOutOfBoundsException> { str.codePointAt(-1) }

        val broccoli = "🥦"
        // 🥦 has a code point value 0x1F966 (129382 in decimal), and it is represented as a UTF-16 surrogate pair 0xD83E, 0xDD66 (or 55358, 56678 in decimal)
        // Returns a code point value corresponding to the surrogate pair with a high surrogate at index 0
        assertPrints(broccoli.codePointAt(0), "129382")
        // Returns a code point value corresponding to the low surrogate
        assertPrints(broccoli.codePointAt(1), "56678")
    }

    @Sample
    fun codePointBefore() {
        val str = "abc"
        // 'a'.code == 97
        assertPrints(str.codePointBefore(1).toString(), "97")
        // 'b'.code == 98
        assertPrints(str.codePointBefore(2).toString(), "98")
        // 'c'.code == 99
        assertPrints(str.codePointBefore(3).toString(), "99")
        // There are no code points prior to index 0
        assertFailsWith<IndexOutOfBoundsException> { str.codePointBefore(0) }
        // The index is negative
        assertFailsWith<IndexOutOfBoundsException> { str.codePointBefore(-1) }
        // The index exceeds the length of the string
        assertFailsWith<IndexOutOfBoundsException> { str.codePointBefore(str.length + 1) }

        val broccoli = "🥦"
        // 🥦 has a code point value 0x1F966 (129382 in decimal), and it is represented as a UTF-16 surrogate pair 0xD83E, 0xDD66 (or 55358, 56678 in decimal)
        // Returns a code point value corresponding to the high surrogate
        assertPrints(broccoli.codePointBefore(1), "55358")
        // Returns a code point value corresponding to the whole surrogate pair
        assertPrints(broccoli.codePointBefore(2), "129382")
    }

    @Sample
    fun codePointCount() {
        val str = "abc"
        // The string contains three code points: 97, 98 and 99
        assertPrints(str.codePointCount(0, 3).toString(), "3")
        // There are two code points in between characters with code points 1 (inclusive) and 3 (exclusive)
        assertPrints(str.codePointCount(1, 3).toString(), "2")
        // There are no code points for an empty range
        assertPrints(str.codePointCount(2, 2).toString(), "0")
        // The begin index cannot exceed the end index
        assertFailsWith<IndexOutOfBoundsException> { str.codePointCount(3, 2) }
        // Indices cannot be negative
        assertFailsWith<IndexOutOfBoundsException> { str.codePointCount(-1, 2) }
        // The end index cannot exceed the length of the string
        assertFailsWith<IndexOutOfBoundsException> { str.codePointCount(0, str.length + 1) }

        val broccoli = "🥦"
        // 🥦 has a code point value 0x1F966, and it is represented as a UTF-16 surrogate pair 0xD83E, 0xDD66
        // The surrogate pair is counted as a single code point
        assertPrints(broccoli.codePointCount(0, broccoli.length /* = 2 */), "1")
        // The high-surrogate char is counted as a single code point as well
        assertPrints(broccoli.codePointCount(0, broccoli.length - 1 /* = 1 */), "1")
    }

    @Sample
    fun splitWithPattern() {
        val digitSplit = "apple123banana456cherry".split(Pattern.compile("\\d+"))
        assertPrints(digitSplit, "[apple, banana, cherry]")

        val wordBoundarySplit = "The quick brown fox".split(Pattern.compile("\\s+"))
        assertPrints(wordBoundarySplit, "[The, quick, brown, fox]")

        val limitSplit = "a,b,c,d,e".split(Pattern.compile(","), limit = 3)
        assertPrints(limitSplit, "[a, b, c,d,e]")

        val patternGroups = "abc-123def_456ghi".split(Pattern.compile("[\\-_]\\d+"))
        assertPrints(patternGroups, "[abc, def, ghi]")

        val caseInsensitiveSplit = "Apple123Banana45CHERRY".split(Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE))
        assertPrints(caseInsensitiveSplit, "[, 123, 45, ]")

        val emptyInputResult = "".split(Pattern.compile("sep"))
        assertTrue(emptyInputResult == listOf(""))

        val emptyDelimiterSplit = "abc".split(Pattern.compile(""))
        assertPrints(emptyDelimiterSplit, "[a, b, c, ]")

        val splitByMultipleSpaces = "a  b    c".split(Pattern.compile("\\s+"))
        assertPrints(splitByMultipleSpaces, "[a, b, c]")

        val splitBySingleSpace = "a  b    c".split(Pattern.compile("\\s"))
        assertPrints(splitBySingleSpace, "[a, , b, , , , c]")
    }
}
