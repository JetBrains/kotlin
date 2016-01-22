package test.text

import test.collections.assertArrayNotSameButEquals
import java.util.Locale
import kotlin.test.*

import org.junit.Test as test

class StringJVMTest {

    @test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
    }

    @test fun toByte() {
        assertEquals(77.toByte(), "77".toByte())
        assertFails { "255".toByte() }
    }

    @test fun toShort() {
        assertEquals(77.toShort(), "77".toShort())
    }

    @test fun toInt() {
        assertEquals(77, "77".toInt())
    }

    @test fun toLong() {
        assertEquals(77.toLong(), "77".toLong())
    }

    @test fun testSplitByPattern() = withOneCharSequenceArg("ab1cd2def3") { s ->
        val isDigit = "\\d".toRegex()
        assertEquals(listOf("ab", "cd", "def", ""), s.split(isDigit))
        assertEquals(listOf("ab", "cd", "def3"), s.split(isDigit, 3))

        // deprecation replacement equivalence
        assertEquals("\\d".toPattern().split(s).toList(), s.split("\\d".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().toList())

        assertFails {
            s.split(isDigit, -1)
        }
    }

    @test fun repeat() = withOneCharSequenceArg { arg1 ->
        fun String.repeat(n: Int): String = arg1(this).repeat(n)

        assertFails { "foo".repeat(-1) }
        assertEquals("", "foo".repeat(0))
        assertEquals("foo", "foo".repeat(1))
        assertEquals("foofoo", "foo".repeat(2))
        assertEquals("foofoofoo", "foo".repeat(3))
    }


    @test fun sliceCharSequenceFails() = withOneCharSequenceArg { arg1 ->
        assertFails {
            arg1("abc").slice(1..4)
        }
        assertFails {
            arg1("ABCDabcd").slice(listOf(10))
        }
    }

    @test fun formatter() {
        assertEquals("12", "%d%d".format(1, 2))

        assertEquals("1,234,567.890", "%,.3f".format(Locale.ENGLISH, 1234567.890))
        assertEquals("1.234.567,890", "%,.3f".format(Locale.GERMAN, 1234567.890))
        assertEquals("1 234 567,890", "%,.3f".format(Locale("fr"), 1234567.890))
    }

    @test fun toByteArrayEncodings() {
        val s = "hello®"
        assertEquals(String(s.toByteArray()), String(s.toByteArray(Charsets.UTF_8)))
    }

    @test fun toCharArray() {
        val s = "hello"
        val chars = s.toCharArray()
        assertArrayNotSameButEquals(charArrayOf('h', 'e', 'l', 'l', 'o'), chars)

        val buffer = CharArray(4)
        s.toCharArray(buffer, 2, 1, 3)
        assertArrayNotSameButEquals(charArrayOf('\u0000', '\u0000', 'e', 'l'), buffer)
    }

    @test fun orderIgnoringCase() {
        val list = listOf("Beast", "Ast", "asterisk")
        assertEquals(listOf("Ast", "Beast", "asterisk"), list.sorted())
        assertEquals(listOf("Ast", "asterisk", "Beast"), list.sortedWith(String.CASE_INSENSITIVE_ORDER))
    }
}
