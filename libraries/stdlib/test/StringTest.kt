package test

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

    /*
    * tests for CharSequence.slice(), String.slice()
    * in /libraries/stdlib/src/kotlin/Strings.kt
    * */
    test fun slice() {
        val iter = listOf(4, 3, 0, 1)
        // abcde
        // 01234
        assertEquals("bcd" , "abcde".slice(1..3))
        assertEquals("dcb" , "abcde".slice(3 downTo 1))
        assertEquals("edab", "abcde".slice(iter))

        val builder = StringBuilder()
        buider.append("ABCD")
        buider.append("abcd")
        // ABCDabcd
        // 01234567
        assertEquals("BCDabc", builder.slice(1..6))
        assertEquals("baD"   , builder.slice(5 downTo 3))
        assertEquals("aDAB"  , builder.slice(iter))
    }
}
