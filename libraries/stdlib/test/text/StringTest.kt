package test.text

import kotlin.test.*
import org.junit.Test as test

// could not be local inside isEmptyAndBlank, because non-toplevel declarations is not yet supported for JS
class IsEmptyCase(val value: String?, val isNull: Boolean = false, val isEmpty: Boolean = false, val isBlank: Boolean = false)

class StringTest {

    test fun isEmptyAndBlank() {

        val cases = listOf(
            IsEmptyCase(null,              isNull = true),
            IsEmptyCase("",                isEmpty = true, isBlank = true),
            IsEmptyCase("  \r\n\t\u00A0",  isBlank = true),
            IsEmptyCase(" Some ")
        )

        for (case in cases) {
            assertEquals(case.isNull || case.isEmpty, case.value.isNullOrEmpty(), "failed for case '${case.value}'")
            assertEquals(case.isNull || case.isBlank, case.value.isNullOrBlank(), "failed for case '${case.value}'")
            if (case.value != null)
            {
                assertEquals(case.isEmpty, case.value.isEmpty(), "failed for case '${case.value}'")
                assertEquals(case.isBlank, case.value.isBlank(), "failed for case '${case.value}'")
            }

        }
    }

    test fun startsWithString() {
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

    test fun endsWithString() {
        assertTrue("abcd".endsWith("d"))
        assertTrue("abcd".endsWith("abcd"))
        assertFalse("abcd".endsWith("b"))
        assertFalse("strö".endsWith("RÖ", ignoreCase = false))
        assertTrue("strö".endsWith("RÖ", ignoreCase = true))
        assertFalse("".endsWith("a"))
        assertTrue("some".endsWith(""))
        assertTrue("".endsWith(""))
    }

    test fun startsWithChar() {
        assertTrue("abcd".startsWith('a'))
        assertFalse("abcd".startsWith('b'))
        assertFalse("abcd".startsWith('A', ignoreCase = false))
        assertTrue("abcd".startsWith('A', ignoreCase = true))
        assertFalse("".startsWith('a'))
    }

    test fun endsWithChar() {
        assertTrue("abcd".endsWith('d'))
        assertFalse("abcd".endsWith('b'))
        assertFalse("strö".endsWith('Ö', ignoreCase = false))
        assertTrue("strö".endsWith('Ö', ignoreCase = true))
        assertFalse("".endsWith('a'))
    }

    test fun capitalize() {
        assertEquals("A", "A".capitalize())
        assertEquals("A", "a".capitalize())
        assertEquals("Abcd", "abcd".capitalize())
        assertEquals("Abcd", "Abcd".capitalize())
    }

    test fun decapitalize() {
        assertEquals("a", "A".decapitalize())
        assertEquals("a", "a".decapitalize())
        assertEquals("abcd", "abcd".decapitalize())
        assertEquals("abcd", "Abcd".decapitalize())
        assertEquals("uRL", "URL".decapitalize())
    }

    test fun slice() {
        val iter = listOf(4, 3, 0, 1)
        // abcde
        // 01234
        assertEquals("bcd", "abcde".substring(1..3))
        assertEquals("dcb", "abcde".slice(3 downTo 1))
        assertEquals("edab", "abcde".slice(iter))
    }

    test fun reverse() {
        assertEquals("dcba", "abcd".reverse())
        assertEquals("4321", "1234".reverse())
        assertEquals("", "".reverse())
    }

    test fun indices() {
        assertEquals(0..4, "abcde".indices)
        assertEquals(0..0, "a".indices)
        assertTrue("".indices.isEmpty())
    }

    test fun replaceRange() {
        val s = "sample text"
        assertEquals("sa??e text", s.replaceRange(2, 5, "??"))
        assertEquals("sa?? text", s.replaceRange(2..5, "??"))
        fails {
            s.replaceRange(5..2, "??")
        }
        fails {
            s.replaceRange(5, 2, "??")
        }

        // symmetry with indices
        assertEquals("??", s.replaceRange(s.indices, "??"))
    }

    test fun removeRange() {
        val s = "sample text"
        assertEquals("sae text", s.removeRange(2, 5))
        assertEquals("sa text", s.removeRange(2..5))

        assertEquals(s, s.removeRange(2,2))

        // symmetry with indices
        assertEquals("", s.removeRange(s.indices))

        // symmetry with replaceRange
        assertEquals(s.replaceRange(2, 5, ""), s.removeRange(2, 5))
        assertEquals(s.replaceRange(2..5, ""), s.removeRange(2..5))
    }

    test fun substringDelimited() {
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

    test fun replaceDelimited() {
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

    test fun stringIterator() {
        var sum = 0
        for(c in "239")
            sum += (c.toInt() - '0'.toInt())
        assertTrue(sum == 14)
    }

    test fun trimStart() {
        assertEquals("", "".trimStart())
        assertEquals("a", "a".trimStart())
        assertEquals("a", " a".trimStart())
        assertEquals("a", "  a".trimStart())
        assertEquals("a  ", "  a  ".trimStart())
        assertEquals("a b", "  a b".trimStart())
        assertEquals("a b ", "  a b ".trimStart())
        assertEquals("a", " \u00A0 a".trimStart())

        assertEquals("a", "\ta".trimStart())
        assertEquals("a", "\t\ta".trimStart())
        assertEquals("a", "\ra".trimStart())
        assertEquals("a", "\na".trimStart())

        assertEquals("a=", "-=-=a=".trimStart('-','='))
        assertEquals("123a", "ab123a".trimStart { it < '0' || it > '9' }) // TODO: Use !it.isDigit when available in JS
    }

    test fun trimEnd() {
        assertEquals("", "".trimEnd())
        assertEquals("a", "a".trimEnd())
        assertEquals("a", "a ".trimEnd())
        assertEquals("a", "a  ".trimEnd())
        assertEquals("  a", "  a  ".trimEnd())
        assertEquals("a b", "a b  ".trimEnd())
        assertEquals(" a b", " a b  ".trimEnd())
        assertEquals("a", "a \u00A0 ".trimEnd())

        assertEquals("a", "a\t".trimEnd())
        assertEquals("a", "a\t\t".trimEnd())
        assertEquals("a", "a\r".trimEnd())
        assertEquals("a", "a\n".trimEnd())

        assertEquals("=a", "=a=-=-".trimEnd('-','='))
        assertEquals("ab123", "ab123a".trimEnd { it < '0' || it > '9' }) // TODO: Use !it.isDigit when available in JS
    }

    test fun trimStartAndEnd() {
        val examples = array(
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

        for (example in examples) {
            assertEquals(example.trim(), example.trimEnd().trimStart())
            assertEquals(example.trim(), example.trimStart().trimEnd())
        }

        val examplesForPredicate = array(
                "123",
                "-=123=-"
        )

        val trimChars = charArray('-','=')
        val trimPredicate = { (it: Char) -> it < '0' || it > '9' } // TODO: Use !it.isDigit when available in JS
        for (example in examplesForPredicate) {
            assertEquals(example.trimStart(*trimChars).trimEnd(*trimChars), example.trim(*trimChars))
            assertEquals(example.trimStart(trimPredicate).trimEnd(trimPredicate), example.trim(trimPredicate))
        }
    }

    test fun padStart() {
        assertEquals("s", "s".padStart(0))
        assertEquals("s", "s".padStart(1))
        assertEquals("  ", "".padStart(2))
        assertEquals("--s", "s".padStart(3, '-'))
        fails {
            "s".padStart(-1)
        }
    }

    test fun padEnd() {
        assertEquals("s", "s".padEnd(0))
        assertEquals("s", "s".padEnd(1))
        assertEquals("  ", "".padEnd(2))
        assertEquals("s--", "s".padEnd(3, '-'))
        fails {
            "s".padEnd(-1)
        }
    }

    test fun removePrefix() {
        assertEquals("fix", "prefix".removePrefix("pre"), "Removes prefix")
        assertEquals("prefix", "preprefix".removePrefix("pre"), "Removes prefix once")
        assertEquals("sample", "sample".removePrefix("value"))
        assertEquals("sample", "sample".removePrefix(""))
    }

    test fun removeSuffix() {
        assertEquals("suf", "suffix".removeSuffix("fix"), "Removes suffix")
        assertEquals("suffix", "suffixfix".removeSuffix("fix"), "Removes suffix once")
        assertEquals("sample", "sample".removeSuffix("value"))
        assertEquals("sample", "sample".removeSuffix(""))
    }

    test fun removeSurrounding() {
        assertEquals("value", "<value>".removeSurrounding("<", ">"))
        assertEquals("<value>", "<<value>>".removeSurrounding("<", ">"), "Removes surrounding once")
        assertEquals("<value", "<value".removeSurrounding("<", ">"), "Only removes surrounding when both prefix and suffix present")
        assertEquals("value>", "value>".removeSurrounding("<", ">"), "Only removes surrounding when both prefix and suffix present")
        assertEquals("value", "value".removeSurrounding("<", ">"))
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
        assertTrue(s.rangesDelimitedBy("-", startIndex = s.length()).single().isEmpty())
    }
    */


    test fun split() {
        assertEquals(listOf(""), "".splitBy(";"))
        assertEquals(listOf("test"), "test".split(*charArray()), "empty list of delimiters, none matched -> entire string returned")
        assertEquals(listOf("test"), "test".splitBy(*array<String>()), "empty list of delimiters, none matched -> entire string returned")

        assertEquals(listOf("abc", "def", "123;456"), "abc;def,123;456".split(';', ',', limit = 3))
        assertEquals(listOf("abc", "def", "123", "456"), "abc<BR>def<br>123<bR>456".splitBy("<BR>", ignoreCase = true))

        assertEquals(listOf("abc", "def", "123", "456"), "abc=-def==123=456".splitBy("==", "=-", "="))
    }

    test fun splitToLines() {
        val string = "first line\rsecond line\nthird line\r\nlast line"
        assertEquals(listOf("first line", "second line", "third line", "last line"), string.lines())


        val singleLine = "single line"
        assertEquals(listOf(singleLine), singleLine.lines())
    }


    test fun indexOfAnyChar() {
        val string = "abracadabra"
        val chars = charArray('d','b')
        assertEquals(1, string.indexOfAny(chars))
        assertEquals(6, string.indexOfAny(chars, startIndex = 2))
        assertEquals(-1, string.indexOfAny(chars, startIndex = 9))

        assertEquals(8, string.lastIndexOfAny(chars))
        assertEquals(6, string.lastIndexOfAny(chars, startIndex = 7))
        assertEquals(-1, string.lastIndexOfAny(chars, startIndex = 0))

        assertEquals(-1, string.indexOfAny(charArray()))
    }

    test fun indexOfAnyCharIgnoreCase() {
        val string = "abraCadabra"
        val chars = charArray('B','c')
        assertEquals(1, string.indexOfAny(chars, ignoreCase = true))
        assertEquals(4, string.indexOfAny(chars, startIndex = 2, ignoreCase = true))
        assertEquals(-1, string.indexOfAny(chars, startIndex = 9, ignoreCase = true))

        assertEquals(8, string.lastIndexOfAny(chars, ignoreCase = true))
        assertEquals(4, string.lastIndexOfAny(chars, startIndex = 7, ignoreCase = true))
        assertEquals(-1, string.lastIndexOfAny(chars, startIndex = 0, ignoreCase = true))
    }

    test fun indexOfAnyString() {
        val string = "abracadabra"
        val substrings = listOf("rac", "ra")
        assertEquals(2, string.indexOfAny(substrings))
        assertEquals(9, string.indexOfAny(substrings, startIndex = 3))
        assertEquals(2, string.indexOfAny(substrings.reverse()))
        assertEquals(-1, string.indexOfAny(substrings, 10))

        assertEquals(9, string.lastIndexOfAny(substrings))
        assertEquals(2, string.lastIndexOfAny(substrings, startIndex = 8))
        assertEquals(2, string.lastIndexOfAny(substrings.reverse(), startIndex = 8))
        assertEquals(-1, string.lastIndexOfAny(substrings, 1))

        assertEquals(0, string.indexOfAny(listOf("dab", "")), "empty strings are not ignored")
        assertEquals(-1, string.indexOfAny(listOf()))
    }

    test fun indexOfAnyStringIgnoreCase() {
        val string = "aBraCadaBrA"
        val substrings = listOf("rAc", "Ra")

        assertEquals(2, string.indexOfAny(substrings, ignoreCase = true))
        assertEquals(9, string.indexOfAny(substrings, startIndex = 3, ignoreCase = true))
        assertEquals(-1, string.indexOfAny(substrings, startIndex = 10, ignoreCase = true))

        assertEquals(9, string.lastIndexOfAny(substrings, ignoreCase = true))
        assertEquals(2, string.lastIndexOfAny(substrings, startIndex = 8, ignoreCase = true))
        assertEquals(-1, string.lastIndexOfAny(substrings, startIndex = 1, ignoreCase = true))
    }

    test fun findAnyOfStrings() {
        val string = "abracadabra"
        val substrings = listOf("rac", "ra")
        assertEquals(2 to "rac", string.findAnyOf(substrings))
        assertEquals(9 to "ra", string.findAnyOf(substrings, startIndex = 3))
        assertEquals(2 to "ra", string.findAnyOf(substrings.reverse()))
        assertEquals(null, string.findAnyOf(substrings, 10))

        assertEquals(9 to "ra", string.findLastAnyOf(substrings))
        assertEquals(2 to "rac", string.findLastAnyOf(substrings, startIndex = 8))
        assertEquals(2 to "ra", string.findLastAnyOf(substrings.reverse(), startIndex = 8))
        assertEquals(null, string.findLastAnyOf(substrings, 1))

        assertEquals(0 to "", string.findAnyOf(listOf("dab", "")), "empty strings are not ignored")
        assertEquals(null, string.findAnyOf(listOf()))
    }

    test fun findAnyOfStringsIgnoreCase() {
        val string = "aBraCadaBrA"
        val substrings = listOf("rAc", "Ra")

        assertEquals(2 to substrings[0], string.findAnyOf(substrings, ignoreCase = true))
        assertEquals(9 to substrings[1], string.findAnyOf(substrings, startIndex = 3, ignoreCase = true))
        assertEquals(null, string.findAnyOf(substrings, startIndex = 10, ignoreCase = true))

        assertEquals(9 to substrings[1], string.findLastAnyOf(substrings, ignoreCase = true))
        assertEquals(2 to substrings[0], string.findLastAnyOf(substrings, startIndex = 8, ignoreCase = true))
        assertEquals(null, string.findLastAnyOf(substrings, startIndex = 1, ignoreCase = true))
    }

    test fun indexOfChar() {
        val string = "bcedef"
        assertEquals(-1, string.indexOf('a'))
        assertEquals(2, string.indexOf('e'))
        assertEquals(2, string.indexOf('e', 2))
        assertEquals(4, string.indexOf('e', 3))
        assertEquals(4, string.lastIndexOf('e'))
        assertEquals(2, string.lastIndexOf('e', 3))

        for (startIndex in -1..string.length()+1) {
            assertEquals(string.indexOfAny(charArray('e'), startIndex), string.indexOf('e', startIndex))
            assertEquals(string.lastIndexOfAny(charArray('e'), startIndex), string.lastIndexOf('e', startIndex))
        }

    }

    test fun indexOfCharIgnoreCase() {
        val string = "bCEdef"
        assertEquals(-1, string.indexOf('a', ignoreCase = true))
        assertEquals(2, string.indexOf('E', ignoreCase = true))
        assertEquals(2, string.indexOf('e', 2, ignoreCase = true))
        assertEquals(4, string.indexOf('E', 3, ignoreCase = true))
        assertEquals(4, string.lastIndexOf('E', ignoreCase = true))
        assertEquals(2, string.lastIndexOf('e', 3, ignoreCase = true))


        for (startIndex in -1..string.length()+1){
            assertEquals(string.indexOfAny(charArray('e'), startIndex, ignoreCase = true), string.indexOf('E', startIndex, ignoreCase = true))
            assertEquals(string.lastIndexOfAny(charArray('E'), startIndex, ignoreCase = true), string.lastIndexOf('e', startIndex, ignoreCase = true))
        }
    }

    test fun indexOfString() {
        val string = "bceded"
        for (index in string.indices)
            assertEquals(index, string.indexOf("", index))
        assertEquals(1, string.indexOf("ced"))
        assertEquals(4, string.indexOf("ed", 3))
        assertEquals(-1, string.indexOf("abcdefgh"))
    }

    test fun indexOfStringIgnoreCase() {
        val string = "bceded"
        for (index in string.indices)
            assertEquals(index, string.indexOf("", index, ignoreCase = true))
        assertEquals(1, string.indexOf("cEd", ignoreCase = true))
        assertEquals(4, string.indexOf("Ed", 3, ignoreCase = true))
        assertEquals(-1, string.indexOf("abcdefgh", ignoreCase = true))
    }


    test fun contains() {
        assertTrue("sample".contains("pl"))
        assertFalse("sample".contains("PL"))
        assertTrue("sömple".contains("Ö", ignoreCase = true))

        assertTrue("sample".contains(""))
        assertTrue("".contains(""))
    }

    test fun equalsIgnoreCase() {
        assertFalse("sample".equals("Sample", ignoreCase = false))
        assertTrue("sample".equals("Sample", ignoreCase = true))
    }
}
