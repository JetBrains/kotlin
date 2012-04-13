package test.string

import kotlin.io.*
import kotlin.test.*

import org.junit.Test as test

class StringTest {
    test fun stringIterator() {
        var sum = 0
        for(c in "239")
            sum += (c.toInt() - '0'.toInt())
        assertTrue(sum == 14)
    }

    test fun stringBuilderIterator() {
        var sum = 0
        val sb = StringBuilder()
        for(c in "239")
            sb.append(c)

        println(sb)

        for(c in sb)
            sum += (c.toInt() - '0'.toInt())
        assertTrue(sum == 14)
    }

    test fun orEmpty() {
        val s: String? = "hey"
        val ns: String? = null

        assertEquals("hey", s.orEmpty())
        assertEquals("", ns.orEmpty())
    }

    test fun toShort() {
        assertEquals(77.toShort(), "77".toShort())
    }

    test fun toInt() {
        assertEquals(77, "77".toInt())
    }

    test fun toLong() {
        assertEquals(77.toLong(), "77".toLong())
    }

    test fun count() {
        val text = "hello there\tfoo\nbar"
        val whitespaceCount = text.count { it.isWhitespace() }
        assertEquals(3, whitespaceCount)
    }

    test fun testSplitByChar() {
        val s = "ab\n[|^$&\\]^cd"
        var list = s.split('b');
        assertEquals(2, list.size)
        assertEquals("a", list[0])
        assertEquals("\n[|^$&\\]^cd", list[1])
        list = s.split('^')
        assertEquals(3, list.size)
        assertEquals("cd", list[2])
        list = s.split('.')
        assertEquals(1, list.size)
        assertEquals(s, list[0])
    }

    fun testStartsWithChar() {
        assertTrue("abcd".startsWith('a'))
        assertFalse("abcd".startsWith('b'))
        assertFalse("".startsWith('a'))
    }

    fun testEndsWithChar() {
        assertTrue("abcd".endsWith('d'))
        assertFalse("abcd".endsWith('b'))
        assertFalse("".endsWith('a'))
    }

}
