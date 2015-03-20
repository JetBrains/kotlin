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

    test fun trimLeading() {
        assertEquals("", "".trimLeading())
        assertEquals("a", "a".trimLeading())
        assertEquals("a", " a".trimLeading())
        assertEquals("a", "  a".trimLeading())
        assertEquals("a  ", "  a  ".trimLeading())
        assertEquals("a b", "  a b".trimLeading())
        assertEquals("a b ", "  a b ".trimLeading())
        assertEquals("a", " \u00A0 a".trimLeading())

        assertEquals("a", "\ta".trimLeading())
        assertEquals("a", "\t\ta".trimLeading())
        assertEquals("a", "\ra".trimLeading())
        assertEquals("a", "\na".trimLeading())

        assertEquals("a=", "-=-=a=".trimLeading('-','='))
        assertEquals("123a", "ab123a".trimLeading { it < '0' || it > '9' }) // TODO: Use !it.isDigit when available in JS
    }

    test fun trimTrailing() {
        assertEquals("", "".trimTrailing())
        assertEquals("a", "a".trimTrailing())
        assertEquals("a", "a ".trimTrailing())
        assertEquals("a", "a  ".trimTrailing())
        assertEquals("  a", "  a  ".trimTrailing())
        assertEquals("a b", "a b  ".trimTrailing())
        assertEquals(" a b", " a b  ".trimTrailing())
        assertEquals("a", "a \u00A0 ".trimTrailing())

        assertEquals("a", "a\t".trimTrailing())
        assertEquals("a", "a\t\t".trimTrailing())
        assertEquals("a", "a\r".trimTrailing())
        assertEquals("a", "a\n".trimTrailing())

        assertEquals("=a", "=a=-=-".trimTrailing('-','='))
        assertEquals("ab123", "ab123a".trimTrailing { it < '0' || it > '9' }) // TODO: Use !it.isDigit when available in JS
    }

    test fun trimTrailingAndLeading() {
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
            assertEquals(example.trim(), example.trimTrailing().trimLeading())
            assertEquals(example.trim(), example.trimLeading().trimTrailing())
        }

        val examplesForPredicate = array(
                "123",
                "-=123=-"
        )

        val trimChars = charArray('-','=')
        val trimPredicate = { (it: Char) -> it < '0' || it > '9' } // TODO: Use !it.isDigit when available in JS
        for (example in examplesForPredicate) {
            assertEquals(example.trimLeading(*trimChars).trimTrailing(*trimChars), example.trim(*trimChars))
            assertEquals(example.trimLeading(trimPredicate).trimTrailing(trimPredicate), example.trim(trimPredicate))
        }
    }

    test fun padLeft() {
        assertEquals("s", "s".padLeft(0))
        assertEquals("s", "s".padLeft(1))
        assertEquals("  ", "".padLeft(2))
        assertEquals("--s", "s".padLeft(3, '-'))
        fails {
            "s".padLeft(-1)
        }
    }

    test fun padRight() {
        assertEquals("s", "s".padRight(0))
        assertEquals("s", "s".padRight(1))
        assertEquals("  ", "".padRight(2))
        assertEquals("s--", "s".padRight(3, '-'))
        fails {
            "s".padRight(-1)
        }
    }

}
