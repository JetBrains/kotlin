package test.text

import java.util.Locale
import kotlin.test.*

import org.junit.Test as test

class StringJVMTest {
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
        assertEquals(2, list.size())
        assertEquals("a", list[0])
        assertEquals("\n[|^$&\\]^cd", list[1])
        list = s.split('^')
        assertEquals(3, list.size())
        assertEquals("cd", list[2])
        list = s.split('.')
        assertEquals(1, list.size())
        assertEquals(s, list[0])
    }

    test fun testSplitByPattern() {
        val s = "ab1cd2def3"
        val isDigit = "\\d".toRegex()
        assertEquals(listOf("ab", "cd", "def", ""), s.split(isDigit))
        assertEquals(listOf("ab", "cd", "def3"), s.split(isDigit, 3))

        fails {
            s.split(isDigit, -1)
        }
    }

    test fun repeat() {
        fails{ "foo".repeat(-1) }
        assertEquals("", "foo".repeat(0))
        assertEquals("foo", "foo".repeat(1))
        assertEquals("foofoo", "foo".repeat(2))
        assertEquals("foofoofoo", "foo".repeat(3))
    }

    test fun filter() {
        assertEquals("acdca", "abcdcba".filter { !it.equals('b') })
        assertEquals("1234", "a1b2c3d4".filter { it.isDigit() })
    }

    test fun filterNot() {
        assertEquals("acdca", "abcdcba".filterNot { it.equals('b') })
        assertEquals("abcd", "a1b2c3d4".filterNot { it.isDigit() })
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

    test fun joinTo() {
        val data = "kotlin".toList()
        val sb = StringBuilder()
        data.joinTo(sb, "^", "<", ">")
        assertEquals("<k^o^t^l^i^n>", sb.toString())
    }

    test fun find() {
        val data = "a1b2c3"
        assertEquals('1', data.first { it.isDigit() })
        assertNull(data.firstOrNull { it.isUpperCase() })
    }

    test fun findNot() {
        val data = "1a2b3c"
        assertEquals('a', data.filterNot { it.isDigit() }.firstOrNull())
        assertNull(data.filterNot { it.isJavaLetterOrDigit() }.firstOrNull())
    }

    test fun partition() {
        val data = "a1b2c3"
        val pair = data.partition { it.isDigit() }
        assertEquals("123", pair.first, "pair.first")
        assertEquals("abc", pair.second, "pair.second")
    }

    test fun map() {
        assertEquals(arrayListOf('a', 'b', 'c'), "abc".map({ it }))

        assertEquals(arrayListOf(true, false, true), "AbC".map({ it.isUpperCase() }))

        assertEquals(arrayListOf<Boolean>(), "".map({ it.isUpperCase() }))

        assertEquals(arrayListOf(97, 98, 99), "abc".map({ it.toInt() }))
    }

    test fun mapTo() {
        val result1 = arrayListOf<Char>()
        val return1 = "abc".mapTo(result1, { it })
        assertEquals(result1, return1)
        assertEquals(arrayListOf('a', 'b', 'c'), result1)

        val result2 = arrayListOf<Boolean>()
        val return2 = "AbC".mapTo(result2, { it.isUpperCase() })
        assertEquals(result2, return2)
        assertEquals(arrayListOf(true, false, true), result2)

        val result3 = arrayListOf<Boolean>()
        val return3 = "".mapTo(result3, { it.isUpperCase() })
        assertEquals(result3, return3)
        assertEquals(arrayListOf<Boolean>(), result3)

        val result4 = arrayListOf<Int>()
        val return4 = "abc".mapTo(result4, { it.toInt() })
        assertEquals(result4, return4)
        assertEquals(arrayListOf(97, 98, 99), result4)
    }

    test fun flatMap() {
        val data = "abcd"
        val result = data.flatMap { listOf(it) }
        assertEquals(data.length(), result.count())
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

        failsWith(javaClass<UnsupportedOperationException>()) {
            "".reduce { a, b -> '\n' }
        }
    }

    test fun reduceRight() {
        // get the smallest character(by char value)
        assertEquals('a', "bacfd".reduceRight { c, v -> if (v > c) c else v })

        failsWith(javaClass<UnsupportedOperationException>()) {
            "".reduceRight { a, b -> '\n' }
        }
    }

    test fun groupBy() {
        // group characters by their case
        val data = "abAbaABcD"
        val result = data.groupBy { it.isLowerCase() }
        assertEquals(2, result.size())
        assertEquals(listOf('a','b','b','a','c'), result.get(true))
    }

    test fun joinToString() {
        val data = "abcd".toList()
        val result = data.joinToString("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring".toList()
        val result2 = data2.joinToString("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)
    }

    test fun join() {
        val data = "abcd".map { it.toString() }
        val result = data.join("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring".map { it.toString() }
        val result2 = data2.join("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)
    }

    test fun dropWhile() {
        val data = "ab1cd2"
        assertEquals("1cd2", data.dropWhile { it.isJavaLetter() })
        assertEquals("", data.dropWhile { true })
        assertEquals("ab1cd2", data.dropWhile { false })
    }

    test fun drop() {
        val data = "abcd1234"
        assertEquals("d1234", data.drop(3))
        fails {
            data.drop(-2)
        }
        assertEquals("", data.drop(data.length + 5))
    }

    test fun takeWhile() {
        val data = "ab1cd2"
        assertEquals("ab", data.takeWhile { it.isJavaLetter() })
        assertEquals("", data.takeWhile { false })
        assertEquals("ab1cd2", data.takeWhile { true })
    }

    test fun take() {
        val data = "abcd1234"
        assertEquals("abc", data.take(3))
        fails {
            data.take(-7)
        }
        assertEquals(data, data.take(data.length + 42))
    }

    test fun formatter() {
        assertEquals("12", "%d%d".format(1, 2))

        assertEquals("1,234,567.890", "%,.3f".format(Locale.ENGLISH, 1234567.890))
        assertEquals("1.234.567,890", "%,.3f".format(Locale.GERMAN, 1234567.890))
        assertEquals("1 234 567,890", "%,.3f".format(Locale("fr"), 1234567.890))
    }

    test fun toByteArrayEncodings() {
        val s = "hello"
        val defaultCharset = java.nio.charset.Charset.defaultCharset()!!
        assertEquals(String(s.toByteArray()), String(s.toByteArray(defaultCharset)))
        assertEquals(String(s.toByteArray()), String(s.toByteArray(defaultCharset.name())))
    }

    test fun testReplaceAllClosure() {
        val s = "test123zzz"
        val result = s.replaceAll("\\d+") { mr ->
            "[" + mr.group() + "]"
        }
        assertEquals("test[123]zzz", result)
    }

    test fun testReplaceAllClosureAtStart() {
        val s = "123zzz"
        val result = s.replaceAll("\\d+") { mr ->
            "[" + mr.group() + "]"
        }
        assertEquals("[123]zzz", result)
    }

    test fun testReplaceAllClosureAtEnd() {
        val s = "test123"
        val result = s.replaceAll("\\d+") { mr ->
            "[" + mr.group() + "]"
        }
        assertEquals("test[123]", result)
    }

    test fun testReplaceAllClosureEmpty() {
        val s = ""
        val result = s.replaceAll("\\d+") { mr ->
            "x"
        }
        assertEquals("", result)

    }

    test fun slice() {
        val iter = listOf(4, 3, 0, 1)

        val builder = StringBuilder()
        builder.append("ABCD")
        builder.append("abcd")
        // ABCDabcd
        // 01234567
        assertEquals("BCDabc", builder.slice(1..6))
        assertEquals("baD", builder.slice(5 downTo 3))
        assertEquals("aDAB", builder.slice(iter))

        fails {
            "abc".slice(listOf(1,4))
        }
        fails {
            builder.slice(listOf(10))
        }
    }
}
