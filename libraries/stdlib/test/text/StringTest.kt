package test.text

import kotlin.test.*
import org.junit.Test as test

class StringTest {

    test fun startsWith() {
        assertTrue("abcd".startsWith("ab"))
        assertTrue("abcd".startsWith("abcd"))
        assertTrue("abcd".startsWith("a"))
        assertFalse("abcd".startsWith("abcde"))
        assertFalse("abcd".startsWith("b"))
        assertFalse("".startsWith('a'))
    }

    test fun endsWith() {
        assertTrue("abcd".endsWith("d"))
        assertTrue("abcd".endsWith("abcd"))
        assertFalse("abcd".endsWith("b"))
        assertFalse("".endsWith('a'))
    }

    test fun testStartsWithChar() {
        assertTrue("abcd".startsWith('a'))
        assertFalse("abcd".startsWith('b'))
        assertFalse("".startsWith('a'))
    }

    test fun testEndsWithChar() {
        assertTrue("abcd".endsWith('d'))
        assertFalse("abcd".endsWith('b'))
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

}
