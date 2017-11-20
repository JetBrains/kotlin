@file:kotlin.jvm.JvmVersion
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

    @Test fun orderIgnoringCase() {
        val list = listOf("Beast", "Ast", "asterisk")
        assertEquals(listOf("Ast", "Beast", "asterisk"), list.sorted())
        assertEquals(listOf("Ast", "asterisk", "Beast"), list.sortedWith(String.CASE_INSENSITIVE_ORDER))
    }

    @Test fun charsets() {
        assertEquals("UTF-32", Charsets.UTF_32.name())
        assertEquals("UTF-32LE", Charsets.UTF_32LE.name())
        assertEquals("UTF-32BE", Charsets.UTF_32BE.name())
    }
}
