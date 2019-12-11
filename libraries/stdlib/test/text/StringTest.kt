/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*
import test.*
import test.collections.behaviors.iteratorBehavior
import test.collections.compare
import kotlin.math.sign
import kotlin.random.Random


fun createString(content: String): CharSequence = content
fun createStringBuilder(content: String): CharSequence = StringBuilder((content as Any).toString()) // required for Rhino JS


val charSequenceBuilders = listOf(::createString, ::createStringBuilder)

fun withOneCharSequenceArg(f: ((String) -> CharSequence) -> Unit) {
    for (arg1Builder in charSequenceBuilders) f(arg1Builder)
}

fun withOneCharSequenceArg(arg1: String, f: (CharSequence) -> Unit) {
    withOneCharSequenceArg { arg1Builder -> f(arg1Builder(arg1)) }
}

fun withTwoCharSequenceArgs(f: ((String) -> CharSequence, (String) -> CharSequence) -> Unit) {
    for (arg1Builder in charSequenceBuilders)
        for (arg2Builder in charSequenceBuilders)
            f(arg1Builder, arg2Builder)
}

fun assertContentEquals(expected: String, actual: CharSequence, message: String? = null) {
    assertEquals(expected, actual.toString(), message)
}

// helper predicates available on both platforms

fun Char.isAsciiDigit() = this in '0'..'9'
fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'
fun Char.isAsciiUpperCase() = this in 'A'..'Z'

class StringTest {

    private fun testStringFromChars(expected: String, chars: CharArray, offset: Int, length: Int) {
        assertEquals(expected, String(chars, offset, length))
        assertEquals(expected, chars.concatToString(startIndex = offset, endIndex = offset + length))
    }

    private fun testStringFromChars(expected: String, chars: CharArray) {
        assertEquals(expected, String(chars))
        assertEquals(expected, chars.concatToString())
    }

    @Test fun stringFromCharArrayFullSlice() {
        val chars: CharArray = charArrayOf('K', 'o', 't', 'l', 'i', 'n')
        testStringFromChars("Kotlin", chars, 0, chars.size)
    }

    @Test fun stringFromCharArraySlice() {
        val chars: CharArray = charArrayOf('K', 'o', 't', 'l', 'i', 'n', ' ', 'r', 'u', 'l', 'e', 's')
        testStringFromChars("rule", chars, 7, 4)

        val longChars = CharArray(200_000) { 'k' }
        val longString = String(longChars, offset = 1000, length = 190_000)
        val longConcatString = longChars.concatToString(startIndex = 1000, endIndex = 191_000)
        assertEquals(190_000, longString.length)
        assertEquals(190_000, longConcatString.length)
        assertTrue(longString.all { it == 'k' })
        assertTrue(longConcatString.all { it == 'k' })
    }

    @Test fun stringFromCharArray() {
        val chars: CharArray = charArrayOf('K', 'o', 't', 'l', 'i', 'n')
        testStringFromChars("Kotlin", chars)

        val longChars = CharArray(200_000) { 'k' }
        val longString = String(longChars)
        val longConcatString = longChars.concatToString()
        assertEquals(200_000, longString.length)
        assertEquals(200_000, longConcatString.length)
        assertTrue(longString.all { it == 'k' })
        assertTrue(longConcatString.all { it == 'k' })
    }

    @Test fun stringFromCharArrayUnicodeSurrogatePairs() {
        val chars: CharArray = charArrayOf('Ц', '月', '語', '\u016C', '\u138D', '\uD83C', '\uDC3A')
        testStringFromChars("Ц月語Ŭᎍ🀺", chars)
        testStringFromChars("月", chars, 1, 1)
        testStringFromChars("Ŭᎍ🀺", chars, 3, 4)
    }

    @Test fun stringFromCharArrayOutOfBounds() {
        fun test(chars: CharArray) {
            assertFailsWith<IndexOutOfBoundsException> { String(chars, -1, 1) }
            assertFailsWith<IndexOutOfBoundsException> { String(chars, 1, -1) }
            assertFailsWith<IndexOutOfBoundsException> { String(chars, chars.size - 1, 2) }

            assertFailsWith<IndexOutOfBoundsException> { chars.concatToString(-1, 1) }
            assertFailsWith<IllegalArgumentException> { chars.concatToString(1, -1) }
            assertFailsWith<IllegalArgumentException> { chars.concatToString(chars.size - 1, 2) }
        }
        test(CharArray(16) { 'k' })
        test(CharArray(160_000) { 'k' })
    }

    @Test
    fun toCharArray() {
        val s = "hello"
        assertArrayContentEquals(charArrayOf('h', 'e', 'l', 'l', 'o'), s.toCharArray())
        assertArrayContentEquals(charArrayOf('e', 'l'), s.toCharArray(1, 3))

        assertFailsWith<IndexOutOfBoundsException> { s.toCharArray(-1) }
        assertFailsWith<IndexOutOfBoundsException> { s.toCharArray(0, 6) }
        assertFailsWith<IllegalArgumentException> { s.toCharArray(3, 1) }
    }

    @Test fun isEmptyAndBlank() = withOneCharSequenceArg { arg1 ->
        class Case(val value: String?, val isNull: Boolean = false, val isEmpty: Boolean = false, val isBlank: Boolean = false)

        val cases = listOf(
            Case(null,              isNull = true),
            Case("",                isEmpty = true, isBlank = true),
            Case("  \r\n\t\u00A0",  isBlank = true),
            Case(" Some ")
        )

        for (case in cases) {
            val value = case.value?.let { arg1(it) }
            assertEquals(case.isNull || case.isEmpty, value.isNullOrEmpty(), "failed for case '$value'")
            assertEquals(case.isNull || case.isBlank, value.isNullOrBlank(), "failed for case '$value'")
            if (value != null) {
                assertEquals(case.isEmpty, value.isEmpty(), "failed for case '$value'")
                assertEquals(case.isBlank, value.isBlank(), "failed for case '$value'")
            }
        }
    }

    @Test fun orEmpty() {
        val s: String? = "hey"
        val ns: String? = null

        assertEquals("hey", s.orEmpty())
        assertEquals("", ns.orEmpty())
    }

    @Test fun regionMatchesForCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        assertTrue(arg1("abcd").regionMatches(1, arg2("debc"), 2, 2))
        assertFalse(arg1("abcd").regionMatches(1, arg2("DEBc"), 2, 2, ignoreCase = false))
        assertTrue(arg1("abcd").regionMatches(1, arg2("DEBc"), 2, 2, ignoreCase = true))

        assertFalse(arg1("abcd").regionMatches(3, arg2(""), 2, 1))
        assertTrue(arg1("abcd").regionMatches(4, arg2(""), 0, 0))
    }

    @Test fun regionMatchesForString() {
        assertTrue("abcd".regionMatches(1, "debc", 2, 2))
        assertFalse("abcd".regionMatches(1, "DEBc", 2, 2, ignoreCase = false))
        assertTrue("abcd".regionMatches(1, "DEBc", 2, 2, ignoreCase = true))

        assertFalse("abcd".regionMatches(3, "", 2, 1))
        assertTrue("abcd".regionMatches(4, "", 0, 0))
    }

    @Test fun startsWithString() {
        assertTrue("abcd".startsWith("ab"))
        assertTrue("abcd".startsWith("abcd"))
        assertTrue("abcd".startsWith("a"))
        assertFalse("abcd".startsWith("abcde"))
        assertFalse("abcd".startsWith("b"))
        assertFalse("".startsWith("a"))
        assertTrue("some".startsWith(""))
        assertTrue("".startsWith(""))

        assertFalse("abcd".startsWith("aB", ignoreCase = false))
        assertTrue("abcd".startsWith("aB", ignoreCase = true))
    }

    @Test fun startsWithStringForCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.startsWithCs(prefix: String, ignoreCase: Boolean = false): Boolean =
            arg1(this).startsWith(arg2(prefix), ignoreCase)

        assertTrue("abcd".startsWithCs("ab"))
        assertTrue("abcd".startsWithCs("abcd"))
        assertTrue("abcd".startsWithCs("a"))
        assertFalse("abcd".startsWithCs("abcde"))
        assertFalse("abcd".startsWithCs("b"))
        assertFalse("".startsWithCs("a"))
        assertTrue("some".startsWithCs(""))
        assertTrue("".startsWithCs(""))

        assertFalse("abcd".startsWithCs("aB", ignoreCase = false))
        assertTrue("abcd".startsWithCs("aB", ignoreCase = true))
    }

    @Test fun endsWithString() {
        assertTrue("abcd".endsWith("d"))
        assertTrue("abcd".endsWith("abcd"))
        assertFalse("abcd".endsWith("b"))
        assertFalse("strö".endsWith("RÖ", ignoreCase = false))
        assertTrue("strö".endsWith("RÖ", ignoreCase = true))
        assertFalse("".endsWith("a"))
        assertTrue("some".endsWith(""))
        assertTrue("".endsWith(""))
    }

    @Test fun endsWithStringForCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.endsWithCs(suffix: String, ignoreCase: Boolean = false): Boolean =
            arg1(this).endsWith(arg2(suffix), ignoreCase)

        assertTrue("abcd".endsWithCs("d"))
        assertTrue("abcd".endsWithCs("abcd"))
        assertFalse("abcd".endsWithCs("b"))
        assertFalse("strö".endsWithCs("RÖ", ignoreCase = false))
        assertTrue("strö".endsWithCs("RÖ", ignoreCase = true))
        assertFalse("".endsWithCs("a"))
        assertTrue("some".endsWithCs(""))
        assertTrue("".endsWithCs(""))
    }

    @Test fun startsWithChar() = withOneCharSequenceArg { arg1 ->
        fun String.startsWith(char: Char, ignoreCase: Boolean = false): Boolean =
            arg1(this).startsWith(char, ignoreCase)

        assertTrue("abcd".startsWith('a'))
        assertFalse("abcd".startsWith('b'))
        assertFalse("abcd".startsWith('A', ignoreCase = false))
        assertTrue("abcd".startsWith('A', ignoreCase = true))
        assertFalse("".startsWith('a'))
    }

    @Test fun endsWithChar() = withOneCharSequenceArg { arg1 ->
        fun String.endsWith(char: Char, ignoreCase: Boolean = false): Boolean =
            arg1(this).endsWith(char, ignoreCase)

        assertTrue("abcd".endsWith('d'))
        assertFalse("abcd".endsWith('b'))
        assertFalse("strö".endsWith('Ö', ignoreCase = false))
        assertTrue("strö".endsWith('Ö', ignoreCase = true))
        assertFalse("".endsWith('a'))
    }

    @Test fun commonPrefix() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.commonPrefixWith(other: String, ignoreCase: Boolean = false): String =
            arg1(this).commonPrefixWith(arg2(other), ignoreCase)

        assertEquals("", "".commonPrefixWith(""))
        assertEquals("", "any".commonPrefixWith(""))
        assertEquals("", "".commonPrefixWith("any"))
        assertEquals("", "some".commonPrefixWith("any"))

        assertEquals("an", "annual".commonPrefixWith("any"))
        assertEquals("an", "annual".commonPrefixWith("Any", ignoreCase = true))
        assertEquals("", "annual".commonPrefixWith("Any", ignoreCase = false))
        // surrogate pairs
        val dth54 = "\uD83C\uDC58" // domino tile horizontal 5-4
        val dth55 = "\uD83C\uDC59" // domino tile horizontal 5-5
        assertEquals("", dth54.commonPrefixWith(dth55))
        assertEquals(dth54, "$dth54$dth54".commonPrefixWith("$dth54$dth55"))
    }

    @Test fun commonSuffix() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.commonSuffixWith(other: String, ignoreCase: Boolean = false): String =
            arg1(this).commonSuffixWith(arg2(other), ignoreCase)

        assertEquals("", "".commonSuffixWith(""))
        assertEquals("", "any".commonSuffixWith(""))
        assertEquals("", "".commonSuffixWith("any"))
        assertEquals("", "some".commonSuffixWith("any"))

        assertEquals("ly", "yearly".commonSuffixWith("monthly"))
        assertEquals("strö", "strö".commonSuffixWith("BISTRÖ", ignoreCase = true))
        assertEquals("", "yearly".commonSuffixWith("HARDLY", ignoreCase = false))
        // surrogate pairs
        val dth54  = "\uD83C\uDC58" // domino tile horizontal 5-4
        val kimono = "\uD83D\uDC58" // kimono
        assertEquals("", dth54.commonSuffixWith(kimono))
        assertEquals("$dth54", "d$dth54".commonSuffixWith("s$dth54"))
    }

    @Test fun capitalize() {
        assertEquals("A", "A".capitalize())
        assertEquals("A", "a".capitalize())
        assertEquals("Abcd", "abcd".capitalize())
        assertEquals("Abcd", "Abcd".capitalize())
    }

    @Test fun decapitalize() {
        assertEquals("a", "A".decapitalize())
        assertEquals("a", "a".decapitalize())
        assertEquals("abcd", "abcd".decapitalize())
        assertEquals("abcd", "Abcd".decapitalize())
        assertEquals("uRL", "URL".decapitalize())
    }

    @Test fun slice() {
        val iter = listOf(4, 3, 0, 1)
        // abcde
        // 01234
        assertEquals("bcd", "abcde".substring(1..3))
        assertEquals("dcb", "abcde".slice(3 downTo 1))
        assertEquals("edab", "abcde".slice(iter))
    }

    @Test fun sliceCharSequence() = withOneCharSequenceArg { arg1 ->
        val iter = listOf(4, 3, 0, 1)

        val data = arg1("ABCDabcd")
        // ABCDabcd
        // 01234567
        assertEquals("BCDabc", data.slice(1..6).toString())
        assertEquals("baD", data.slice(5 downTo 3).toString())
        assertEquals("aDAB", data.slice(iter).toString())
    }

    @Test fun reverse() {
        assertEquals("dcba", "abcd".reversed())
        assertEquals("4321", "1234".reversed())
        assertEquals("", "".reversed())
    }

    @Test fun reverseCharSequence() = withOneCharSequenceArg { arg1 ->
        fun String.reversedCs(): CharSequence = arg1(this).reversed()

        assertContentEquals("dcba", "abcd".reversedCs())
        assertContentEquals("4321", "1234".reversedCs())
        assertContentEquals("", "".reversedCs())
    }

    @Test fun indices() = withOneCharSequenceArg { arg1 ->
        fun String.indices(): IntRange = arg1(this).indices

        assertEquals(0..4, "abcde".indices())
        assertEquals(0..0, "a".indices())
        assertTrue("".indices().isEmpty())
    }

    @Test fun replaceRange() = withTwoCharSequenceArgs { arg1, arg2 ->
        val s = arg1("sample text")
        val replacement = arg2("??")

        assertContentEquals("sa??e text", s.replaceRange(2, 5, replacement))
        assertContentEquals("sa?? text", s.replaceRange(2..5, replacement))
        assertFails {
            s.replaceRange(5..2, replacement)
        }
        assertFails {
            s.replaceRange(5, 2, replacement)
        }

        // symmetry with indices
        assertContentEquals(replacement.toString(), s.replaceRange(s.indices, replacement))
    }

    @Test fun removeRange() = withOneCharSequenceArg("sample text") { s ->
        assertContentEquals("sae text", s.removeRange(2, 5))
        assertContentEquals("sa text", s.removeRange(2..5))

        assertContentEquals(s.toString(), s.removeRange(2, 2))

        // symmetry with indices
        assertContentEquals("", s.removeRange(s.indices))

        // symmetry with replaceRange
        assertContentEquals(s.toString().replaceRange(2, 5, ""), s.removeRange(2, 5))
        assertContentEquals(s.toString().replaceRange(2..5, ""), s.removeRange(2..5))
    }

    @Test fun substringDelimited() {
        val s = "-1,22,3+"
        // chars
        assertEquals("22,3+", s.substringAfter(','))
        assertEquals("3+", s.substringAfterLast(','))
        assertEquals("-1", s.substringBefore(','))
        assertEquals("-1,22", s.substringBeforeLast(','))

        // strings
        assertEquals("22,3+", s.substringAfter(","))
        assertEquals("3+", s.substringAfterLast(","))
        assertEquals("-1", s.substringBefore(","))
        assertEquals("-1,22", s.substringBeforeLast(","))

        // non-existing delimiter
        assertEquals("", s.substringAfter("+"))
        assertEquals("", s.substringBefore("-"))
        assertEquals(s, s.substringBefore("="))
        assertEquals(s, s.substringAfter("="))
        assertEquals("xxx", s.substringBefore("=", "xxx"))
        assertEquals("xxx", s.substringAfter("=", "xxx"))

    }

    @Test fun replaceDelimited() {
        val s = "/user/folder/file.extension"
        // chars
        assertEquals("/user/folder/file.doc", s.replaceAfter('.', "doc"))
        assertEquals("/user/folder/another.doc", s.replaceAfterLast('/', "another.doc"))
        assertEquals("new name.extension", s.replaceBefore('.', "new name"))
        assertEquals("/new/path/file.extension", s.replaceBeforeLast('/', "/new/path"))

        // strings
        assertEquals("/user/folder/file.doc", s.replaceAfter(".", "doc"))
        assertEquals("/user/folder/another.doc", s.replaceAfterLast("/", "another.doc"))
        assertEquals("new name.extension", s.replaceBefore(".", "new name"))
        assertEquals("/new/path/file.extension", s.replaceBeforeLast("/", "/new/path"))

        // non-existing delimiter
        assertEquals("/user/folder/file.extension", s.replaceAfter("=", "doc"))
        assertEquals("/user/folder/file.extension", s.replaceAfterLast("=", "another.doc"))
        assertEquals("/user/folder/file.extension", s.replaceBefore("=", "new name"))
        assertEquals("/user/folder/file.extension", s.replaceBeforeLast("=", "/new/path"))
        assertEquals("xxx", s.replaceBefore("=", "new name", "xxx"))
        assertEquals("xxx", s.replaceBeforeLast("=", "/new/path", "xxx"))
    }

    @Test fun repeat() = withOneCharSequenceArg { arg1 ->
        fun String.repeat(n: Int): String = arg1(this).repeat(n)

        assertFails { "foo".repeat(-1) }
        assertEquals("", "foo".repeat(0))
        assertEquals("foo", "foo".repeat(1))
        assertEquals("foofoo", "foo".repeat(2))
        assertEquals("foofoofoo", "foo".repeat(3))

        assertEquals("", "".repeat(Int.MAX_VALUE))
        assertEquals("aaaaaaaaaaaaa", "a".repeat(13))
    }

    @Test fun stringIterator() = withOneCharSequenceArg("239") { data ->
        var sum = 0
        for (c in data)
            sum += (c - '0')
        assertTrue(sum == 14)
    }

    @Test fun trimStart() = withOneCharSequenceArg { arg1 ->
        fun String.trimStartCS(): CharSequence = arg1(this).trimStart()
        assertContentEquals("", "".trimStartCS())
        assertContentEquals("a", "a".trimStartCS())
        assertContentEquals("a", " a".trimStartCS())
        assertContentEquals("a", "  a".trimStartCS())
        assertContentEquals("a  ", "  a  ".trimStartCS())
        assertContentEquals("a b", "  a b".trimStartCS())
        assertContentEquals("a b ", "  a b ".trimStartCS())
        assertContentEquals("a", " \u00A0 a".trimStartCS())

        assertContentEquals("a", "\ta".trimStartCS())
        assertContentEquals("a", "\t\ta".trimStartCS())
        assertContentEquals("a", "\ra".trimStartCS())
        assertContentEquals("a", "\na".trimStartCS())

        assertContentEquals("a=", arg1("-=-=a=").trimStart('-', '='))
        assertContentEquals("123a", arg1("ab123a").trimStart { !it.isAsciiDigit() })
    }

    @Test fun trimEnd() = withOneCharSequenceArg { arg1 ->
        fun String.trimEndCS(): CharSequence = arg1(this).trimEnd()
        assertContentEquals("", "".trimEndCS())
        assertContentEquals("a", "a".trimEndCS())
        assertContentEquals("a", "a ".trimEndCS())
        assertContentEquals("a", "a  ".trimEndCS())
        assertContentEquals("  a", "  a  ".trimEndCS())
        assertContentEquals("a b", "a b  ".trimEndCS())
        assertContentEquals(" a b", " a b  ".trimEndCS())
        assertContentEquals("a", "a \u00A0 ".trimEndCS())

        assertContentEquals("a", "a\t".trimEndCS())
        assertContentEquals("a", "a\t\t".trimEndCS())
        assertContentEquals("a", "a\r".trimEndCS())
        assertContentEquals("a", "a\n".trimEndCS())

        assertContentEquals("=a", arg1("=a=-=-").trimEnd('-', '='))
        assertContentEquals("ab123", arg1("ab123a").trimEnd { !it.isAsciiDigit() })
    }

    @Test fun trimStartAndEnd() = withOneCharSequenceArg { arg1 ->
        val examples = arrayOf(
            "a",
            " a ",
            "  a  ",
            "  a b  ",
            "\ta\tb\t",
            "\t\ta\t\t",
            "\ra\r",
            "\na\n",
            " \u00A0 a \u00A0 "
        )

        for ((source, example) in examples.map { it to arg1(it) }) {
            assertContentEquals(source.trimEnd().trimStart(), example.trim())
            assertContentEquals(source.trimStart().trimEnd(), example.trim())
        }

        val examplesForPredicate = arrayOf(
            "123",
            "-=123=-"
        )

        val trimChars = charArrayOf('-', '=')
        val trimPredicate = { it: Char -> !it.isAsciiDigit() }
        for ((source, example) in examplesForPredicate.map { it to arg1(it) }) {
            assertContentEquals(source.trimStart(*trimChars).trimEnd(*trimChars), example.trim(*trimChars))
            assertContentEquals(source.trimStart(trimPredicate).trimEnd(trimPredicate), example.trim(trimPredicate))
        }
    }

    @Test fun padStart() = withOneCharSequenceArg { arg1 ->
        val s = arg1("s")
        assertContentEquals("s", s.padStart(0))
        assertContentEquals("s", s.padStart(1))
        assertContentEquals("--s", s.padStart(3, '-'))
        assertContentEquals("  ", arg1("").padStart(2))
        assertFails {
            s.padStart(-1)
        }
    }

    @Test fun padEnd() = withOneCharSequenceArg { arg1 ->
        val s = arg1("s")
        assertContentEquals("s", s.padEnd(0))
        assertContentEquals("s", s.padEnd(1))
        assertContentEquals("s--", s.padEnd(3, '-'))
        assertContentEquals("  ", arg1("").padEnd(2))
        assertFails {
            s.padEnd(-1)
        }
    }

    @Test fun removePrefix() = withOneCharSequenceArg("pre") { prefix ->
        assertEquals("fix", "prefix".removePrefix(prefix), "Removes prefix")
        assertEquals("prefix", "preprefix".removePrefix(prefix), "Removes prefix once")
        assertEquals("sample", "sample".removePrefix(prefix))
        assertEquals("sample", "sample".removePrefix(""))
    }

    @Test fun removeSuffix() = withOneCharSequenceArg("fix") { suffix ->
        assertEquals("suf", "suffix".removeSuffix(suffix), "Removes suffix")
        assertEquals("suffix", "suffixfix".removeSuffix(suffix), "Removes suffix once")
        assertEquals("sample", "sample".removeSuffix(suffix))
        assertEquals("sample", "sample".removeSuffix(""))
    }

    @Test fun removeSurrounding() = withOneCharSequenceArg { arg1 ->
        val pre = arg1("<")
        val post = arg1(">")
        assertEquals("value", "<value>".removeSurrounding(pre, post))
        assertEquals("<value>", "<<value>>".removeSurrounding(pre, post), "Removes surrounding once")
        assertEquals("<value", "<value".removeSurrounding(pre, post), "Only removes surrounding when both prefix and suffix present")
        assertEquals("value>", "value>".removeSurrounding(pre, post), "Only removes surrounding when both prefix and suffix present")
        assertEquals("value", "value".removeSurrounding(pre, post))

        assertEquals("<->", "<->".removeSurrounding(arg1("<-"), arg1("->")), "Does not remove overlapping prefix and suffix")
    }

    @Test fun removePrefixCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.removePrefix(prefix: String) = arg1(this).removePrefix(arg2(prefix))
        val prefix = "pre"

        assertContentEquals("fix", "prefix".removePrefix(prefix), "Removes prefix")
        assertContentEquals("prefix", "preprefix".removePrefix(prefix), "Removes prefix once")
        assertContentEquals("sample", "sample".removePrefix(prefix))
        assertContentEquals("sample", "sample".removePrefix(""))
    }

    @Test fun removeSuffixCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.removeSuffix(suffix: String) = arg1(this).removeSuffix(arg2(suffix))
        val suffix = "fix"

        assertContentEquals("suf", "suffix".removeSuffix(suffix), "Removes suffix")
        assertContentEquals("suffix", "suffixfix".removeSuffix(suffix), "Removes suffix once")
        assertContentEquals("sample", "sample".removeSuffix(suffix))
        assertContentEquals("sample", "sample".removeSuffix(""))
    }

    @Test fun removeSurroundingCharSequence() = withTwoCharSequenceArgs { arg1, arg2 ->
        fun String.removeSurrounding(prefix: String, postfix: String) = arg1(this).removeSurrounding(arg2(prefix), arg2(postfix))

        assertContentEquals("value", "<value>".removeSurrounding("<", ">"))
        assertContentEquals("<value>", "<<value>>".removeSurrounding("<", ">"), "Removes surrounding once")
        assertContentEquals("<value", "<value".removeSurrounding("<", ">"), "Only removes surrounding when both prefix and suffix present")
        assertContentEquals("value>", "value>".removeSurrounding("<", ">"), "Only removes surrounding when both prefix and suffix present")
        assertContentEquals("value", "value".removeSurrounding("<", ">"))

        assertContentEquals("<->", "<->".removeSurrounding("<-", "->"), "Does not remove overlapping prefix and suffix")
    }

    /*
    // unit test commented out until rangesDelimitiedBy would become public

    test fun rangesDelimitedBy() {
        assertEquals(listOf(0..2, 4..3, 5..7), "abc--def".rangesDelimitedBy('-').toList())
        assertEquals(listOf(0..2, 5..7, 9..10), "abc--def-xy".rangesDelimitedBy("--", "-").toList())
        assertEquals(listOf(0..2, 7..9, 14..16), "123<br>456<BR>789".rangesDelimitedBy("<br>", ignoreCase = true).toList())
        assertEquals(listOf(2..2, 4..6), "a=b=c=d".rangesDelimitedBy("=", startIndex = 2, limit = 2).toList())

        val s = "sample"
        assertEquals(listOf(s.indices), s.rangesDelimitedBy("-").toList())
        assertEquals(listOf(s.indices), s.rangesDelimitedBy("-", startIndex = -1).toList())
        assertTrue(s.rangesDelimitedBy("-", startIndex = s.length).single().isEmpty())
    }
    */


    @Test fun split() = withOneCharSequenceArg { arg1 ->
        operator fun String.unaryPlus(): CharSequence = arg1(this)

        assertEquals(listOf(""), (+"").split(";"))
        assertEquals(listOf("test"), (+"test").split(*charArrayOf()), "empty list of delimiters, none matched -> entire string returned")
        assertEquals(listOf("test"), (+"test").split(*arrayOf<String>()), "empty list of delimiters, none matched -> entire string returned")

        assertEquals(listOf("abc", "def", "123;456"), (+"abc;def,123;456").split(';', ',', limit = 3))
        assertEquals(listOf("abc;def,123;456"), (+"abc;def,123;456").split(';', ',', limit = 1))

        assertEquals(listOf("abc", "def", "123", "456"), (+"abc=-def==123=456").split("==", "=-", "="))

        assertEquals(listOf("", "a", "b", "c", ""), (+"abc").split(""))
        assertEquals(listOf("", "a", "b", "b", "a", ""), (+"abba").split("", "a"))
        assertEquals(listOf("", "", "b", "b", "", ""), (+"abba").split("a", ""))
        assertEquals(listOf("", "bb", ""), (+"abba").split("a", "c"))
        assertEquals(listOf("", "bb", ""), (+"abba").split('a', 'c'))

    }

    @Test fun splitSingleDelimiter() = withOneCharSequenceArg { arg1 ->
        operator fun String.unaryPlus(): CharSequence = arg1(this)

        assertEquals(listOf(""), (+"").split(";"))

        assertEquals(listOf("abc", "def", "123,456"), (+"abc,def,123,456").split(',', limit = 3))
        assertEquals(listOf("abc", "def", "123,456"), (+"abc,def,123,456").split(",", limit = 3))

        assertEquals(listOf("abc", "def", "123,456,789"), (+"abc,def,123,456,789").split(',', limit = 3))
        assertEquals(listOf("abc", "def", "123,456,789"), (+"abc,def,123,456,789").split(",", limit = 3))

        assertEquals(listOf("abc", "def", "123,456,789,"), (+"abc,def,123,456,789,").split(',', limit = 3))
        assertEquals(listOf("abc", "def", "123,456,789,"), (+"abc,def,123,456,789,").split(",", limit = 3))

        assertEquals(listOf("abc,def,123,456,789,"), (+"abc,def,123,456,789,").split(',', limit = 1))
        assertEquals(listOf("abc,def,123,456,789,"), (+"abc,def,123,456,789,").split(",", limit = 1))

        assertEquals(listOf("abc", "def", "123", "456"), (+"abc<BR>def<br>123<bR>456").split("<BR>", ignoreCase = true))
        assertEquals(listOf("abc", "def<br>123<bR>456"), (+"abc<BR>def<br>123<bR>456").split("<BR>", ignoreCase = false))

        assertEquals(listOf("a", "b", "c"), (+"a*b*c").split("*"))
        assertEquals(listOf("", "bb", ""), (+"abba").split("a"))
        assertEquals(listOf("", "bb", ""), (+"abba").split('a'))
    }

    @Test fun splitToLines() = withOneCharSequenceArg { arg1 ->
        val string = arg1("first line\rsecond line\nthird line\r\nlast line")
        assertEquals(listOf("first line", "second line", "third line", "last line"), string.lines())


        val singleLine = arg1("single line")
        assertEquals(listOf(singleLine.toString()), singleLine.lines())
    }

    @Test fun splitIllegalLimit() = withOneCharSequenceArg("test string") { string ->
        assertFailsWith<IllegalArgumentException> { string.split(*arrayOf<String>(), limit = -1) }
        assertFailsWith<IllegalArgumentException> { string.split(*charArrayOf(), limit = -2) }
        assertFailsWith<IllegalArgumentException> { string.split("", limit = -3) }
        assertFailsWith<IllegalArgumentException> { string.split('3', limit = -4) }
        assertFailsWith<IllegalArgumentException> { string.split("1", limit = -5) }
        assertFailsWith<IllegalArgumentException> { string.split('4', '1', limit = -6) }
        assertFailsWith<IllegalArgumentException> { string.split("5", "9", limit = -7) }
    }

    @Test fun indexOfAnyChar() = withOneCharSequenceArg("abracadabra") { string ->
        val chars = charArrayOf('d', 'b')
        assertEquals(1, string.indexOfAny(chars))
        assertEquals(6, string.indexOfAny(chars, startIndex = 2))
        assertEquals(-1, string.indexOfAny(chars, startIndex = 9))

        assertEquals(8, string.lastIndexOfAny(chars))
        assertEquals(6, string.lastIndexOfAny(chars, startIndex = 7))
        assertEquals(-1, string.lastIndexOfAny(chars, startIndex = 0))

        assertEquals(-1, string.indexOfAny(charArrayOf()))
    }

    @Test fun indexOfAnyCharIgnoreCase() = withOneCharSequenceArg("abraCadabra") { string ->
        val chars = charArrayOf('B', 'c')
        assertEquals(1, string.indexOfAny(chars, ignoreCase = true))
        assertEquals(4, string.indexOfAny(chars, startIndex = 2, ignoreCase = true))
        assertEquals(-1, string.indexOfAny(chars, startIndex = 9, ignoreCase = true))

        assertEquals(8, string.lastIndexOfAny(chars, ignoreCase = true))
        assertEquals(4, string.lastIndexOfAny(chars, startIndex = 7, ignoreCase = true))
        assertEquals(-1, string.lastIndexOfAny(chars, startIndex = 0, ignoreCase = true))
    }

    @Test fun indexOfAnyString() = withOneCharSequenceArg("abracadabra") { string ->
        val substrings = listOf("rac", "ra")
        assertEquals(2, string.indexOfAny(substrings))
        assertEquals(9, string.indexOfAny(substrings, startIndex = 3))
        assertEquals(2, string.indexOfAny(substrings.reversed()))
        assertEquals(-1, string.indexOfAny(substrings, 10))

        assertEquals(9, string.lastIndexOfAny(substrings))
        assertEquals(2, string.lastIndexOfAny(substrings, startIndex = 8))
        assertEquals(2, string.lastIndexOfAny(substrings.reversed(), startIndex = 8))
        assertEquals(-1, string.lastIndexOfAny(substrings, 1))

        assertEquals(0, string.indexOfAny(listOf("dab", "")), "empty strings are not ignored")
        assertEquals(-1, string.indexOfAny(listOf()))
    }

    @Test fun indexOfAnyStringIgnoreCase() = withOneCharSequenceArg("aBraCadaBrA") { string ->
        val substrings = listOf("rAc", "Ra")

        assertEquals(2, string.indexOfAny(substrings, ignoreCase = true))
        assertEquals(9, string.indexOfAny(substrings, startIndex = 3, ignoreCase = true))
        assertEquals(-1, string.indexOfAny(substrings, startIndex = 10, ignoreCase = true))

        assertEquals(9, string.lastIndexOfAny(substrings, ignoreCase = true))
        assertEquals(2, string.lastIndexOfAny(substrings, startIndex = 8, ignoreCase = true))
        assertEquals(-1, string.lastIndexOfAny(substrings, startIndex = 1, ignoreCase = true))
    }

    @Test fun findAnyOfStrings() = withOneCharSequenceArg("abracadabra") { string ->
        val substrings = listOf("rac", "ra")
        assertEquals(2 to "rac", string.findAnyOf(substrings))
        assertEquals(9 to "ra", string.findAnyOf(substrings, startIndex = 3))
        assertEquals(2 to "ra", string.findAnyOf(substrings.reversed()))
        assertEquals(null, string.findAnyOf(substrings, 10))

        assertEquals(9 to "ra", string.findLastAnyOf(substrings))
        assertEquals(2 to "rac", string.findLastAnyOf(substrings, startIndex = 8))
        assertEquals(2 to "ra", string.findLastAnyOf(substrings.reversed(), startIndex = 8))
        assertEquals(null, string.findLastAnyOf(substrings, 1))

        assertEquals(0 to "", string.findAnyOf(listOf("dab", "")), "empty strings are not ignored")
        assertEquals(null, string.findAnyOf(listOf()))
    }

    @Test fun findAnyOfStringsIgnoreCase() = withOneCharSequenceArg("aBraCadaBrA") { string ->
        val substrings = listOf("rAc", "Ra")

        assertEquals(2 to substrings[0], string.findAnyOf(substrings, ignoreCase = true))
        assertEquals(9 to substrings[1], string.findAnyOf(substrings, startIndex = 3, ignoreCase = true))
        assertEquals(null, string.findAnyOf(substrings, startIndex = 10, ignoreCase = true))

        assertEquals(9 to substrings[1], string.findLastAnyOf(substrings, ignoreCase = true))
        assertEquals(2 to substrings[0], string.findLastAnyOf(substrings, startIndex = 8, ignoreCase = true))
        assertEquals(null, string.findLastAnyOf(substrings, startIndex = 1, ignoreCase = true))
    }

    @Test fun indexOfChar() = withOneCharSequenceArg("bcedef") { string ->
        assertEquals(-1, string.indexOf('a'))
        assertEquals(2, string.indexOf('e'))
        assertEquals(2, string.indexOf('e', 2))
        assertEquals(4, string.indexOf('e', 3))
        assertEquals(4, string.lastIndexOf('e'))
        assertEquals(2, string.lastIndexOf('e', 3))

        for (startIndex in -1..string.length+1) {
            assertEquals(string.indexOfAny(charArrayOf('e'), startIndex), string.indexOf('e', startIndex))
            assertEquals(string.lastIndexOfAny(charArrayOf('e'), startIndex), string.lastIndexOf('e', startIndex))
        }

    }

    @Test fun indexOfCharIgnoreCase() = withOneCharSequenceArg("bCEdef") { string ->
        assertEquals(-1, string.indexOf('a', ignoreCase = true))
        assertEquals(2, string.indexOf('E', ignoreCase = true))
        assertEquals(2, string.indexOf('e', 2, ignoreCase = true))
        assertEquals(4, string.indexOf('E', 3, ignoreCase = true))
        assertEquals(4, string.lastIndexOf('E', ignoreCase = true))
        assertEquals(2, string.lastIndexOf('e', 3, ignoreCase = true))


        for (startIndex in -1..string.length + 1) {
            assertEquals(
                string.indexOfAny(charArrayOf('e'), startIndex, ignoreCase = true),
                string.indexOf('E', startIndex, ignoreCase = true)
            )
            assertEquals(
                string.lastIndexOfAny(charArrayOf('E'), startIndex, ignoreCase = true),
                string.lastIndexOf('e', startIndex, ignoreCase = true)
            )
        }
    }

    @Test fun indexOfString() = withOneCharSequenceArg("bceded") { string ->
        for (index in string.indices)
            assertEquals(index, string.indexOf("", index))
        assertEquals(1, string.indexOf("ced"))
        assertEquals(4, string.indexOf("ed", 3))
        assertEquals(-1, string.indexOf("abcdefgh"))
    }

    @Test fun indexOfStringIgnoreCase() = withOneCharSequenceArg("bceded") { string ->
        for (index in string.indices)
            assertEquals(index, string.indexOf("", index, ignoreCase = true))
        assertEquals(1, string.indexOf("cEd", ignoreCase = true))
        assertEquals(4, string.indexOf("Ed", 3, ignoreCase = true))
        assertEquals(-1, string.indexOf("abcdefgh", ignoreCase = true))
    }


    @Test fun contains() = withTwoCharSequenceArgs { arg1, arg2 ->
        operator fun String.contains(other: String): Boolean = arg1(this).contains(arg2(other))
        operator fun String.contains(other: Char): Boolean = arg1(this).contains(other)

        assertTrue("pl" in "sample")
        assertFalse("PL" in "sample")
        assertTrue(arg1("sömple").contains(arg2("Ö"), ignoreCase = true))

        assertTrue("" in "sample")
        assertTrue("" in "")

        assertTrue('ö' in "sömple")
        assertFalse('Ö' in "sömple")
        assertTrue(arg1("sömple").contains('Ö', ignoreCase = true))
    }

    @Test fun equalsIgnoreCase() {
        assertFalse("sample".equals("Sample", ignoreCase = false))
        assertTrue("sample".equals("Sample", ignoreCase = true))
        assertFalse("sample".equals(null, ignoreCase = false))
        assertFalse("sample".equals(null, ignoreCase = true))
        assertTrue(null.equals(null, ignoreCase = true))
        assertTrue(null.equals(null, ignoreCase = false))
    }

    @Test fun compareToIgnoreCase() {

        fun assertCompareResult(expectedResult: Int, v1: String, v2: String, ignoreCase: Boolean) {
            val result = v1.compareTo(v2, ignoreCase = ignoreCase).sign
            assertEquals(expectedResult, result, "Comparing '$v1' with '$v2', ignoreCase = $ignoreCase")
            if (expectedResult == 0)
                assertTrue(v1.equals(v2, ignoreCase = ignoreCase))
            if (!ignoreCase)
                assertEquals(v1.compareTo(v2).sign, result)
        }

        fun assertCompareResult(expectedResult: Int, expectedResultIgnoreCase: Int, v1: String, v2: String) {
            assertCompareResult(expectedResult, v1, v2, false)
            assertCompareResult(expectedResultIgnoreCase, v1, v2, true)
        }

        val (EQ, LT, GT) = listOf(0, -1, 1)

        assertCompareResult(EQ, EQ, "ABC", "ABC")
        assertCompareResult(LT, EQ, "ABC", "ABc")
        assertCompareResult(GT, EQ, "ABc", "ABC")

        assertCompareResult(LT, LT, "ABC", "ABx")
        assertCompareResult(LT, GT, "ABX", "ABc")

        assertCompareResult(LT, LT, "[", "aa")
        assertCompareResult(GT, LT, "[", "AA")
        assertCompareResult(EQ, EQ, "", "")
        assertCompareResult(LT, LT, "", "A")
        assertCompareResult(GT, GT, "A", "")

        run {
            val a32 = "A".repeat(32)
            assertCompareResult(LT, EQ, a32 + "B", a32 + "b")
            assertCompareResult(LT, GT, a32 + "BB", a32 + "b")
            assertCompareResult(LT, GT, a32 + "C", a32 + "b")

        }

        val equalIgnoringCase = listOf("ABC", "ABc", "aBC", "AbC", "abc")
        for (item1 in equalIgnoringCase) {
            for (item2 in equalIgnoringCase) {
                assertCompareResult(EQ, item1, item2, ignoreCase = true)
            }
        }
    }


    @Test fun orderIgnoringCase() {
        val list = listOf("Beast", "Ast", "asterisk", "[]")
        assertEquals(listOf("Ast", "Beast", "[]", "asterisk"), list.sorted())
        assertEquals(listOf("[]", "Ast", "asterisk", "Beast"), list.sortedWith(String.CASE_INSENSITIVE_ORDER))
    }

    @Test fun replace() {
        val input = "abbAb"
        assertEquals("abb${'$'}b", input.replace('A', '$'))
        assertEquals("/bb/b", input.replace('A', '/', ignoreCase = true))

        assertEquals("${'$'}bAb", input.replace("ab", "$"))
        assertEquals("/b/", input.replace("ab", "/", ignoreCase = true))

        assertEquals("-a-b-b-A-b-", input.replace("", "-"))
    }

    @Test fun replaceFirst() {
        val input = "AbbabA"
        assertEquals("Abb${'$'}bA", input.replaceFirst('a', '$'))
        assertEquals("${'$'}bbabA", input.replaceFirst('a', '$', ignoreCase = true))
        // doesn't pass in Rhino JS
        // assertEquals("schrodinger", "schrÖdinger".replaceFirst('ö', 'o', ignoreCase = true))

        assertEquals("Abba${'$'}", input.replaceFirst("bA", "$"))
        assertEquals("Ab${'$'}bA", input.replaceFirst("bA", "$", ignoreCase = true))

        assertEquals("-test", "test".replaceFirst("", "-"))
    }

    @Test fun count() = withOneCharSequenceArg("hello there\tfoo\nbar") { text ->
        val whitespaceCount = text.count { it.isWhitespace() }
        assertEquals(3, whitespaceCount)
    }

    @Test fun testSplitByChar() = withOneCharSequenceArg("ab\n[|^$&\\]^cd") { s ->
        s.split('b').let { list ->
            assertEquals(2, list.size)
            assertEquals("a", list[0])
            assertEquals("\n[|^$&\\]^cd", list[1])
        }
        s.split('^').let { list ->
            assertEquals(3, list.size)
            assertEquals("cd", list[2])
        }
        s.split('.').let { list ->
            assertEquals(1, list.size)
            assertEquals(s.toString(), list[0])
        }
    }

    @Test fun forEach() = withOneCharSequenceArg("abcd1234") { data ->
        var count = 0
        val sb = StringBuilder()
        data.forEach {
            count++
            sb.append(it)
        }
        assertEquals(data.length, count)
        assertEquals(data.toString(), sb.toString())
    }

    @Test
    fun onEach() = withOneCharSequenceArg("abcd") { data ->
        val result = StringBuilder()
        val newData = data.onEach { result.append(it + 1) }
        assertEquals("bcde", result.toString())
        assertTrue(data === newData)

        // static types test
        assertStaticTypeIs<String>("x".onEach { })
        assertStaticTypeIs<StringBuilder>(result.onEach { })
    }


    @Test fun filter() {
        assertEquals("acdca", ("abcdcba").filter { !it.equals('b') })
        assertEquals("1234", ("a1b2c3d4").filter { it.isAsciiDigit() })
    }

    @Test fun filterCharSequence() = withOneCharSequenceArg { arg1 ->
        assertContentEquals("acdca", arg1("abcdcba").filter { !it.equals('b') })
        assertContentEquals("1234", arg1("a1b2c3d4").filter { it.isAsciiDigit() })
    }

    @Test fun filterNot() {
        assertEquals("acdca", ("abcdcba").filterNot { it.equals('b') })
        assertEquals("abcd", ("a1b2c3d4").filterNot { it.isAsciiDigit() })
    }

    @Test fun filterNotCharSequence() = withOneCharSequenceArg { arg1 ->
        assertContentEquals("acdca", arg1("abcdcba").filterNot { it.equals('b') })
        assertContentEquals("abcd", arg1("a1b2c3d4").filterNot { it.isAsciiDigit() })
    }

    @Test fun filterIndexed() {
        val data = "abedcf"
        assertEquals("abdf", data.filterIndexed { index, c -> c == 'a' + index })
    }

    @Test fun filterIndexedCharSequence() = withOneCharSequenceArg("abedcf") { data ->
        assertContentEquals("abdf", data.filterIndexed { index, c -> c == 'a' + index })
    }

    @Test fun all() = withOneCharSequenceArg("AbCd") { data ->
        assertTrue {
            data.all { it.isAsciiLetter() }
        }
        assertFalse {
            data.all { it.isAsciiUpperCase() }
        }
    }

    @Test fun any() = withOneCharSequenceArg("a1bc") { data ->
        assertTrue {
            data.any() { it.isAsciiDigit() }
        }
        assertFalse {
            data.any() { it.isAsciiUpperCase() }
        }
    }

    @Test fun find() = withOneCharSequenceArg("a1b2c3") { data ->
        assertEquals('1', data.first { it.isAsciiDigit() })
        assertNull(data.firstOrNull { it.isAsciiUpperCase() })
    }

    @Test fun findNot() = withOneCharSequenceArg("1a2b3c") { data ->
        assertEquals('a', data.filterNot { it.isAsciiDigit() }.firstOrNull())
        assertNull(data.filterNot { it.isAsciiLetter() || it.isAsciiDigit() }.firstOrNull())
    }

    @Test fun random() = withOneCharSequenceArg { data ->
        data("abcdefg").let { charSeq ->
            val tosses = List(10) { charSeq.random() }
            assertTrue(tosses.distinct().size > 1, "Should be some distinct elements in $tosses")

            val seed = Random.nextInt()
            val random1 = Random(seed)
            val random2 = Random(seed)

            val tosses1 = List(10) { charSeq.random(random1) }
            val tosses2 = List(10) { charSeq.random(random2) }

            assertEquals(tosses1, tosses2)
        }

        data("x").let { singletonCharSeq ->
            val tosses = List(10) { singletonCharSeq.random() }
            assertEquals(singletonCharSeq.toList(), tosses.distinct())
        }

        assertFailsWith<NoSuchElementException> { data("").random() }
    }

    @Test fun partition() {
        val data = "a1b2c3"
        val pair = data.partition { it.isAsciiDigit() }
        assertEquals("123", pair.first, "pair.first")
        assertEquals("abc", pair.second, "pair.second")
    }

    @Test fun partitionCharSequence() = withOneCharSequenceArg("a1b2c3") { data ->
        val pair = data.partition { it.isAsciiDigit() }
        assertContentEquals("123", pair.first, "pair.first")
        assertContentEquals("abc", pair.second, "pair.second")
    }

    @Test fun zipWithNext() = withOneCharSequenceArg { arg1 ->
        assertEquals(listOf("ab", "bc"), arg1("abc").zipWithNext { a: Char, b: Char -> a.toString() + b })
        assertTrue(arg1("").zipWithNext { a: Char, b: Char -> a.toString() + b }.isEmpty())
        assertTrue(arg1("a").zipWithNext { a: Char, b: Char -> a.toString() + b }.isEmpty())
    }

    @Test fun zipWithNextPairs() = withOneCharSequenceArg { arg1 ->
        assertEquals(listOf('a' to 'b', 'b' to 'c'), arg1("abc").zipWithNext())
        assertTrue(arg1("").zipWithNext().isEmpty())
        assertTrue(arg1("a").zipWithNext().isEmpty())
    }


    @Test
    fun chunked() = withOneCharSequenceArg { arg1 ->
        val size = 7
        val data = arg1("abcdefg")
        val result = data.chunked(4)
        assertEquals(listOf("abcd", "efg"), result)

        val result2 = data.chunked(3) { it.reversed().toString() }
        assertEquals(listOf("cba", "fed", "g"), result2)

        data.toString().let { expectedSingleChunk ->
            assertEquals(expectedSingleChunk, data.chunked(size).single())
            assertEquals(expectedSingleChunk, data.chunked(size + 3).single())
            assertEquals(expectedSingleChunk, data.chunked(Int.MAX_VALUE).single())
        }

        arg1("ab").let {
            assertEquals(it.toString(), it.chunked(Int.MAX_VALUE).single())
        }

        assertTrue(arg1("").chunked(3).isEmpty())

        for (illegalValue in listOf(Int.MIN_VALUE, -1, 0)) {
            assertFailsWith<IllegalArgumentException>("size $illegalValue") { data.chunked(illegalValue) }
        }

        for (chunkSize in 1..size + 1) {
            compare(data.chunked(chunkSize).iterator(), data.chunkedSequence(chunkSize).iterator()) { iteratorBehavior() }
        }
    }


    @Test
    fun windowed() = withOneCharSequenceArg { arg1 ->
        val size = 7
        val data = arg1("abcdefg")
        val result = data.windowed(4, 2)
        assertEquals(listOf("abcd", "cdef"), result)

        val resultPartial = data.windowed(4, 2, partialWindows = true)
        assertEquals(listOf("abcd", "cdef", "efg", "g"), resultPartial)

        val result2 = data.windowed(2, 3) { it.reversed().toString() }
        assertEquals(listOf("ba", "ed"), result2)
        val result2partial = data.windowed(2, 3, partialWindows = true) { it.reversed().toString() }
        assertEquals(listOf("ba", "ed", "g"), result2partial)

        assertEquals(data.chunked(2), data.windowed(2, 2, partialWindows = true))

        assertEquals(data.take(2), data.windowed(2, size).single())
        assertEquals(data.take(3), data.windowed(3, size + 3).single())


        assertEquals(data.toString(), data.windowed(size, 1).single())
        assertTrue(data.windowed(size + 1, 1).isEmpty())

        val result3partial = data.windowed(size, 1, partialWindows = true)
        result3partial.forEachIndexed { index, window ->
            assertEquals(size - index, window.length, "size of window#$index")
        }

        assertTrue(arg1("").windowed(3, 2).isEmpty())

        for (illegalValue in listOf(Int.MIN_VALUE, -1, 0)) {
            assertFailsWith<IllegalArgumentException>("size $illegalValue") { data.windowed(illegalValue, 1) }
            assertFailsWith<IllegalArgumentException>("step $illegalValue") { data.windowed(1, illegalValue) }
        }

        for (window in 1..size + 1) {
            for (step in 1..size + 1) {
                compare(data.windowed(window, step).iterator(), data.windowedSequence(window, step).iterator()) { iteratorBehavior() }
                compare(data.windowed(window, step, partialWindows = true).iterator(),
                        data.windowedSequence(window, step, partialWindows = true).iterator()) { iteratorBehavior() }
            }
        }

        // index overflow tests
        for (partialWindows in listOf(true, false)) {

            val windowed1 = data.windowed(5, Int.MAX_VALUE, partialWindows)
            assertEquals("abcde", windowed1.single())
            val windowed2 = data.windowed(Int.MAX_VALUE, 5, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toString(), data.substring(5, 7)) else listOf(), windowed2)
            val windowed3 = data.windowed(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toString()) else listOf(), windowed3)

            val windowedTransform1 = data.windowed(5, Int.MAX_VALUE, partialWindows) { it }
            assertEquals("abcde", windowedTransform1.single())
            val windowedTransform2 = data.windowed(Int.MAX_VALUE, 5, partialWindows) { it }
            assertEquals(if (partialWindows) listOf(data.toString(), data.substring(5, 7)) else listOf(), windowedTransform2)
            val windowedTransform3 = data.windowed(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows) { it }
            assertEquals(if (partialWindows) listOf(data.toString()) else listOf(), windowedTransform3)

            val windowedSequence1 = data.windowedSequence(5, Int.MAX_VALUE, partialWindows)
            assertEquals("abcde", windowedSequence1.single())
            val windowedSequence2 = data.windowedSequence(Int.MAX_VALUE, 5, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toString(), data.substring(5, 7)) else listOf(), windowedSequence2.toList())
            val windowedSequence3 = data.windowedSequence(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toString()) else listOf(), windowedSequence3.toList())

            val windowedSequenceTransform1 = data.windowedSequence(5, Int.MAX_VALUE, partialWindows) { it }
            assertEquals("abcde", windowedSequenceTransform1.single())
            val windowedSequenceTransform2 = data.windowedSequence(Int.MAX_VALUE, 5, partialWindows) { it }
            assertEquals(if (partialWindows) listOf(data.toString(), data.substring(5, 7)) else listOf(), windowedSequenceTransform2.toList())
            val windowedSequenceTransform3 = data.windowedSequence(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows) { it }
            assertEquals(if (partialWindows) listOf(data.toString()) else listOf(), windowedSequenceTransform3.toList())
        }
    }

    @Test fun map() = withOneCharSequenceArg { arg1 ->
        assertEquals(listOf('a', 'b', 'c'), arg1("abc").map { it })

        assertEquals(listOf(true, false, true), arg1("AbC").map { it.isAsciiUpperCase() })

        assertEquals(listOf<Boolean>(), arg1("").map { it.isAsciiUpperCase() })

        assertEquals(listOf(97, 98, 99), arg1("abc").map { it.toInt() })
    }

    @Test fun mapTo() = withOneCharSequenceArg { arg1 ->
        val result1 = arrayListOf<Char>()
        val return1 = arg1("abc").mapTo(result1, { it })
        assertEquals(result1, return1)
        assertEquals(arrayListOf('a', 'b', 'c'), result1)

        val result2 = arrayListOf<Boolean>()
        val return2 = arg1("AbC").mapTo(result2, { it.isAsciiUpperCase() })
        assertEquals(result2, return2)
        assertEquals(arrayListOf(true, false, true), result2)

        val result3 = arrayListOf<Boolean>()
        val return3 = arg1("").mapTo(result3, { it.isAsciiUpperCase() })
        assertEquals(result3, return3)
        assertEquals(arrayListOf<Boolean>(), result3)

        val result4 = arrayListOf<Int>()
        val return4 = arg1("abc").mapTo(result4, { it.toInt() })
        assertEquals(result4, return4)
        assertEquals(arrayListOf(97, 98, 99), result4)
    }

    @Test fun flatMap() = withOneCharSequenceArg("abcd") { data ->
        val result = data.flatMap { ('a'..it) + ' ' }
        assertEquals("a ab abc abcd ".toList(), result)
    }

    @Test fun fold() = withOneCharSequenceArg { arg1 ->
        // calculate number of digits in the string
        val data = arg1("a1b2c3def")
        val result = data.fold(0, { digits, c -> if (c.isAsciiDigit()) digits + 1 else digits })
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, arg1("ABCD").fold(true, { r, c -> r && c.isAsciiUpperCase() }))

        //get string back
        assertEquals(data.toString(), data.fold("", { s, c -> s + c }))
    }

    @Test fun foldRight() = withOneCharSequenceArg { arg1 ->
        // calculate number of digits in the string
        val data = arg1("a1b2c3def")
        val result = data.foldRight(0, { c, digits -> if (c.isAsciiDigit()) digits + 1 else digits })
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, arg1("ABCD").foldRight(true, { c, r -> r && c.isAsciiUpperCase() }))

        //get string back
        assertEquals(data.toString(), data.foldRight("", { s, c -> "" + s + c }))
    }

    @Test fun reduceIndexed() = withOneCharSequenceArg { arg1 ->
        // get the 3rd character
        assertEquals('c', arg1("bacfd").reduceIndexed { index, v, c -> if (index == 2) c else v })

        expect('c') {
            "ab".reduceIndexed { index, acc, e ->
                assertEquals(1, index)
                assertEquals('a', acc)
                assertEquals('b', e)
                e + (e - acc)
            }
        }

        assertFailsWith<UnsupportedOperationException> {
            arg1("").reduceIndexed { _, _, _ -> '\n' }
        }
    }

    @Test fun reduceRightIndexed() = withOneCharSequenceArg { arg1 ->
        // get the 3rd character
        assertEquals('c', arg1("bacfd").reduceRightIndexed { index, c, v -> if (index == 2) c else v })

        expect('c') {
            "ab".reduceRightIndexed { index, e, acc ->
                assertEquals(0, index)
                assertEquals('b', acc)
                assertEquals('a', e)
                acc + (acc - e)
            }
        }

        assertFailsWith<UnsupportedOperationException> {
            arg1("").reduceRightIndexed { _, _, _ -> '\n' }
        }
    }

    @Test fun reduce() = withOneCharSequenceArg { arg1 ->
        // get the smallest character(by char value)
        assertEquals('a', arg1("bacfd").reduce { v, c -> if (v > c) c else v })

        assertFailsWith<UnsupportedOperationException> {
            arg1("").reduce { _, _ -> '\n' }
        }
    }

    @Test fun reduceRight() = withOneCharSequenceArg { arg1 ->
        // get the smallest character(by char value)
        assertEquals('a', arg1("bacfd").reduceRight { c, v -> if (v > c) c else v })

        assertFailsWith<UnsupportedOperationException> {
            arg1("").reduceRight { _, _ -> '\n' }
        }
    }

    @Test fun reduceOrNull() = withOneCharSequenceArg { arg1 ->
        // get the smallest character(by char value)
        assertEquals('a', arg1("bacfd").reduceOrNull { v, c -> if (v > c) c else v })

        expect(null, { arg1("").reduceOrNull { _, _ -> '\n' } })
    }

    @Test fun groupBy() = withOneCharSequenceArg("abAbaABcD") { data ->
        // group characters by their case
        val result = data.groupBy { it.isAsciiUpperCase() }
        assertEquals(2, result.size)
        assertEquals(listOf('a', 'b', 'b', 'a', 'c'), result[false])
        assertEquals(listOf('A', 'A', 'B', 'D'), result[true])
    }

    @Test fun associateWith() = withOneCharSequenceArg("abc") { data ->
        val result = data.associateWith { it + 1 }
        assertEquals(mapOf('a' to 'b', 'b' to 'c', 'c' to 'd'), result)

        val mutableResult = data.drop(1).associateWithTo(result.toMutableMap()) { it - 1 }
        assertEquals(mapOf('a' to 'b', 'b' to 'a', 'c' to 'b'), mutableResult)
    }

    @Test fun joinToString() {
        val data = "abcd".toList()
        val result = data.joinToString("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring".toList()
        val result2 = data2.joinToString("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)

        val data3 = "a1/b".toList()
        val result3 = data3.joinToString() { it.toUpperCase().toString() }
        assertEquals("A, 1, /, B", result3)
    }

    @Test fun joinTo() {
        val data = "kotlin".toList()
        val sb = StringBuilder()
        data.joinTo(sb, "^", "<", ">")
        assertEquals("<k^o^t^l^i^n>", sb.toString())
    }


    @Test fun dropWhile() {
        val data = "ab1cd2"
        assertEquals("1cd2", data.dropWhile { it.isAsciiLetter() })
        assertEquals("", data.dropWhile { true })
        assertEquals("ab1cd2", data.dropWhile { false })
    }

    @Test fun dropWhileCharSequence() = withOneCharSequenceArg("ab1cd2") { data ->
        assertContentEquals("1cd2", data.dropWhile { it.isAsciiLetter() })
        assertContentEquals("", data.dropWhile { true })
        assertContentEquals("ab1cd2", data.dropWhile { false })
    }


    @Test fun drop() {
        val data = "abcd1234"
        assertEquals("d1234", data.drop(3))
        assertFails {
            data.drop(-2)
        }
        assertEquals("", data.drop(data.length + 5))
    }

    @Test fun dropCharSequence() = withOneCharSequenceArg("abcd1234") { data ->
        assertContentEquals("d1234", data.drop(3))
        assertFails {
            data.drop(-2)
        }
        assertContentEquals("", data.drop(data.length + 5))
    }

    @Test fun takeWhile() {
        val data = "ab1cd2"
        assertEquals("ab", data.takeWhile { it.isAsciiLetter() })
        assertEquals("", data.takeWhile { false })
        assertEquals("ab1cd2", data.takeWhile { true })
    }

    @Test fun takeWhileCharSequence() = withOneCharSequenceArg("ab1cd2") { data ->
        assertContentEquals("ab", data.takeWhile { it.isAsciiLetter() })
        assertContentEquals("", data.takeWhile { false })
        assertContentEquals("ab1cd2", data.takeWhile { true })
    }

    @Test fun take() {
        val data = "abcd1234"
        assertEquals("abc", data.take(3))
        assertFails {
            data.take(-7)
        }
        assertEquals(data, data.take(data.length + 42))
    }

    @Test fun takeCharSequence() = withOneCharSequenceArg("abcd1234") { data ->
        assertEquals("abc", data.take(3))
        assertFails {
            data.take(-7)
        }
        assertContentEquals(data.toString(), data.take(data.length + 42))
    }


    @Test fun testReplaceAllClosure() = withOneCharSequenceArg("test123zzz") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("test[123]zzz", result)
    }

    @Test fun testReplaceAllClosureAtStart() = withOneCharSequenceArg("123zzz") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("[123]zzz", result)
    }

    @Test fun testReplaceAllClosureAtEnd() = withOneCharSequenceArg("test123") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("test[123]", result)
    }

    @Test fun testReplaceAllClosureEmpty() = withOneCharSequenceArg("") { s ->
        val result = s.replace("\\d+".toRegex()) { _ ->
            "x"
        }
        assertEquals("", result)

    }

    @Test fun trimMargin() {
        // WARNING
        // DO NOT REFORMAT AS TESTS MAY FAIL DUE TO INDENTATION CHANGE

        assertEquals("ABC\n123\n456", """ABC
                                      |123
                                      |456""".trimMargin())

        assertEquals("ABC\n  123\n  456", """ABC
                                      |123
                                      |456""".replaceIndentByMargin(newIndent = "  "))

        assertEquals("ABC \n123\n456", """ABC${" "}
                                      |123
                                      |456""".trimMargin())

        assertEquals(" ABC\n123\n456", """ ABC
                                        >>123
                                        ${"\t"}>>456""".trimMargin(">>"))

        assertEquals("", "".trimMargin())

        assertEquals("", """
                            """.trimMargin())

        assertEquals("", """
                            |""".trimMargin())

        assertEquals("", """
                            |
                            """.trimMargin())

        assertEquals("    a", """
            |    a
        """.trimMargin())

        assertEquals("    a", """
            |    a""".trimMargin())

        assertEquals("    a", """ |    a
        """.trimMargin())

        assertEquals("    a", """ |    a""".trimMargin())

        assertEquals("\u0000|ABC", "${"\u0000"}|ABC".trimMargin())
    }

    @Test fun trimIndent() {
        // WARNING
        // DO NOT REFORMAT AS TESTS MAY FAIL DUE TO INDENTATION CHANGE

        assertEquals("123", """
        123
        """.trimIndent())

        assertEquals("123\n   456", """
        123
           456
           """.trimIndent())

        assertEquals("   123\n456", """
           123
        456
        """.trimIndent())

        assertEquals("     123\n  456", """
           123
        456
        """.replaceIndent(newIndent = "  "))

        assertEquals("   123\n456", """
           123
        456""".trimIndent())

        assertEquals("    ", """
${"    "}
        """.trimIndent())

        val deindented = """
                                                            ,.
                      ,.                     _       oo.   `88P
                     ]88b              ,o.  d88.    ]88b     '
                      888   _          Y888o888     d88P     _     _
                      888 ,888          `Y88888o_  ,888    d88b   d88._____
                      888,888P ,oooooo.   ;888888b.]88P    888'   d888888888p
                      888888P d88888888.  J88b'YPP ]88b   ,888    d888P'''888.
                      8888P' ]88P   `888  d88[     d88P   ]88b    888'    Y88b
                      8888p  ]88b    888  888      d88[    888    888.    `888
                     ,88888b  888[   888  888.     d88[    888.   Y88b     Y88[
                     d88PY88b `888L,d88P  Y88b     Y88b    ]88b   `888     888'
                     888  Y88b  Y88888P    888.     888.    888.   Y88b   `88P
                    d88P   888   `'P'      Y888.    `888.   `88P   `Y8P     '
                    Y8P'    '               `YP      Y8P'     '

                    ____       dXp   _    _        _________
                  ddXXXXXp     XXP  ,XX  dXb      Yo.XXXXXX      ,oooooo.
                  X'L_oXXP     XX'   XX[ dXb      dXb            YPPPPXXX'
                  XYXXXXX     ]XX    dXb dXb      dX8Xooooo         dXXP
                  XXb`YYXXo.   YXXo_ dXP dXP      YXb''''''       ,XXP'
                  `XX   `YYXb   `YXXXXP  XX[      ]XX            ,XX'
                   YXb     YXb     `''   XXXXooL  `XX._____      `XXXXXXXXooooo.
                   `XP      '             ''''''   YPXXXXXX'       ''''''`''YPPP
        """.trimIndent()

        assertEquals(23, deindented.lines().size)
        val indents = deindented.lines().map { "^\\s*".toRegex().find(it)!!.value.length }
        assertEquals(0, indents.min())
        assertEquals(42, indents.max())
        assertEquals(1, deindented.lines().count { it.isEmpty() })
    }

    @Test fun testIndent() {
        assertEquals("  ABC\n  123", "ABC\n123".prependIndent("  "))
        assertEquals("  ABC\n  \n  123", "ABC\n\n123".prependIndent("  "))
        assertEquals("  ABC\n  \n  123", "ABC\n \n123".prependIndent("  "))
        assertEquals("  ABC\n   \n  123", "ABC\n   \n123".prependIndent("  "))
        assertEquals("  ", "".prependIndent("  "))
    }

    @Test
    fun elementAt() {
        expect('a') { "a c".elementAt(0) }
        expect(' ') { "a c".elementAt(1) }
        expect('c') { "a c".elementAt(2) }

        assertFailsWith<IndexOutOfBoundsException> { "".elementAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { "a c".elementAt(-1) }
    }
}
