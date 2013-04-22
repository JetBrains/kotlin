package test.string

import kotlin.test.*

import org.junit.Test as test

class StringJVMTest {
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
        val result = data.flatMap { listOf(it) }
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

    test fun formatter() {
        assertEquals("12", "%d%d".format(1, 2))
    }

    test fun trimLeading() {
        assertEquals("", "".trimLeading())
        assertEquals("a", "a".trimLeading())
        assertEquals("a", " a".trimLeading())
        assertEquals("a", "  a".trimLeading())
        assertEquals("a  ", "  a  ".trimLeading())
        assertEquals("a b", "  a b".trimLeading())
        assertEquals("a b ", "  a b ".trimLeading())

        assertEquals("a", "\ta".trimLeading())
        assertEquals("a", "\t\ta".trimLeading())
        assertEquals("a", "\ra".trimLeading())
        assertEquals("a", "\na".trimLeading())
    }

    test fun trimTrailing() {
        assertEquals("", "".trimTrailing())
        assertEquals("a", "a".trimTrailing())
        assertEquals("a", "a ".trimTrailing())
        assertEquals("a", "a  ".trimTrailing())
        assertEquals("  a", "  a  ".trimTrailing())
        assertEquals("a b", "a b  ".trimTrailing())
        assertEquals(" a b", " a b  ".trimTrailing())

        assertEquals("a", "a\t".trimTrailing())
        assertEquals("a", "a\t\t".trimTrailing())
        assertEquals("a", "a\r".trimTrailing())
        assertEquals("a", "a\n".trimTrailing())
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
                "\na\n"
        )

        for (example in examples) {
            assertEquals(example.trim(), example.trimTrailing().trimLeading())
            assertEquals(example.trim(), example.trimLeading().trimTrailing())
        }
    }

    test fun toByteArrayEncodings() {
        val s = "hello"
        val defaultCharset = java.nio.charset.Charset.defaultCharset()!!
        assertEquals(s.toByteArray().toString(), s.toByteArray(defaultCharset).toString())
        assertEquals(s.toByteArray().toString(), s.toByteArray(defaultCharset.name()).toString())
    }
}
