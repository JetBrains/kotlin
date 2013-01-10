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

    test fun filter() {
        assertEquals("acdca", "abcdcba".filter { !it.equals('b') })
        assertEquals("1234", "a1b2c3d4".filter { it.isDigit() })
    }

    test fun reverse() {
        assertEquals("dcba", "abcd".reverse())
        assertEquals("4321", "1234".reverse())
        assertEquals("", "".reverse())
    }

    test fun forEach() {
        val data = "abcd1234"
        var count = 0
        data.forEach{ count++ }
        assertEquals(data.length(), count)
    }

    test fun all() {
        val data = "AbCd"
        assertTrue {
            data.all { it.isJavaLetter() }
        }
        assertNot {
            data.all { it.isUpperCase() }
        }
    }

    test fun any() {
      val data = "a1bc"
      assertTrue {
          data.any() { it.isDigit() }
      }
      assertNot {
          data.any() { it.isUpperCase() }
      }
    }
}
