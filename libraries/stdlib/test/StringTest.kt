package test

import java.util.Collections
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

    test fun filterNot() {
        assertEquals("acdca", "abcdcba".filterNot { it.equals('b') })
        assertEquals("abcd", "a1b2c3d4".filterNot { it.isDigit() })
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

    test fun appendString() {
        val data = "kotlin"
        val sb = StringBuilder()
        data.appendString(sb, "^", "<", ">")
        assertEquals("<k^o^t^l^i^n>", sb.toString())
    }

    test fun find() {
        val data = "a1b2c3"
        assertEquals('1', data.find { it.isDigit() })
        assertNull(data.find { it.isUpperCase() })
    }

    test fun findNot() {
        val data = "1a2b3c"
        assertEquals('a', data.findNot { it.isDigit() })
        assertNull(data.findNot { it.isJavaLetterOrDigit() })
    }

    test fun partition() {
        val data = "a1b2c3"
        val pair = data.partition { it.isDigit() }
        assertEquals("123", pair.first, "pair.first")
        assertEquals("abc", pair.second, "pair.second")
    }

    test fun flatMap() {
        val data = "abcd"
        val result = data.flatMap { Collections.singletonList(it) }
        assertEquals(data.size, result.count())
        assertEquals(data.toCharList(), result)
    }

    test fun fold() {
        // calculate number of digits in the string
        val data = "a1b2c3def"
        val result = data.fold(0, { digits, c -> if(c.isDigit()) digits + 1 else digits } )
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, "ABCD".fold(true, { r, c -> r && c.isUpperCase() }))

        //get string back
        assertEquals(data, data.fold("", { s, c -> s + c }))
    }

    test fun foldRight() {
        // calculate number of digits in the string
        val data = "a1b2c3def"
        val result = data.foldRight(0, { c, digits -> if(c.isDigit()) digits + 1 else digits })
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, "ABCD".foldRight(true, { c, r -> r && c.isUpperCase() }))

        //get string back
        assertEquals(data, data.foldRight("", { s, c -> "" + s + c }))
    }

    test fun reduce() {
        // get the smallest character(by char value)
        assertEquals('a', "bacfd".reduce { v, c -> if (v > c) c else v })

        failsWith<UnsupportedOperationException> {
            "".reduce { a, b -> '\n' }
        }
    }

    test fun reduceRight() {
        // get the smallest character(by char value)
        assertEquals('a', "bacfd".reduceRight { c, v -> if (v > c) c else v })

        failsWith<UnsupportedOperationException> {
            "".reduceRight { a, b -> '\n' }
        }
    }

    test fun groupBy() {
        // collect similar characters by their int code
        val data = "ababaaabcd"
        val result = data.groupBy { it.toInt() }
        assertEquals(4, result.size)
        assertEquals("bbb", result.get('b'.toInt()))
    }

    test fun makeString() {
        val data = "abcd"
        val result = data.makeString("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring"
        val result2 = data2.makeString("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)
    }

    test fun dropWhile() {
        val data = "ab1cd2"
        val result = data.dropWhile { it.isJavaLetter() }
        assertEquals("1cd2", result)
    }

    test fun drop() {
        val data = "abcd1234"
        assertEquals("d1234", data.drop(3))
        assertEquals(data, data.drop(-2))
        assertEquals("", data.drop(data.length + 5))
    }

    test fun takeWhile() {
        val data = "ab1cd2"
        val result = data.takeWhile { it.isJavaLetter() }
        assertEquals("ab", result)
    }

    test fun take() {
        val data = "abcd1234"
        assertEquals("abc", data.take(3))
        assertEquals("", data.take(-7))
        assertEquals(data, data.take(data.length + 42))
    }
}
