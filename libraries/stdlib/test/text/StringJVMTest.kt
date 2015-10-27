package test.text

import java.util.Locale
import kotlin.test.*

import org.junit.Test as test

class StringJVMTest {

    @test fun orEmpty() {
        val s: String? = "hey"
        val ns: String? = null

        assertEquals("hey", s.orEmpty())
        assertEquals("", ns.orEmpty())
    }

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

    @test fun count() = withOneCharSequenceArg("hello there\tfoo\nbar") { text ->
        val whitespaceCount = text.count { it.isWhitespace() }
        assertEquals(3, whitespaceCount)
    }

    @test fun testSplitByChar() = withOneCharSequenceArg("ab\n[|^$&\\]^cd") { s ->
        var list = s.split('b');
        assertEquals(2, list.size())
        assertEquals("a", list[0])
        assertEquals("\n[|^$&\\]^cd", list[1])
        list = s.split('^')
        assertEquals(3, list.size())
        assertEquals("cd", list[2])
        list = s.split('.')
        assertEquals(1, list.size())
        assertEquals(s.toString(), list[0])
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

    @test fun filter() = withOneCharSequenceArg { arg1 ->
        assertEquals("acdca", arg1("abcdcba").filter { !it.equals('b') })
        assertEquals("1234", arg1("a1b2c3d4").filter { it.isDigit() })
    }

    @test fun filterNot() = withOneCharSequenceArg { arg1 ->
        assertEquals("acdca", arg1("abcdcba").filterNot { it.equals('b') })
        assertEquals("abcd", arg1("a1b2c3d4").filterNot { it.isDigit() })
    }

    @test fun forEach() = withOneCharSequenceArg("abcd1234") { data ->
        var count = 0
        val sb = StringBuilder()
        data.forEach {
            count++
            sb.append(it)
        }
        assertEquals(data.length, count)
        assertEquals(data.toString(), sb.toString())
    }

    @test fun all() = withOneCharSequenceArg("AbCd") { data ->
        assertTrue {
            data.all { it.isJavaIdentifierPart() }
        }
        assertFalse {
            data.all { it.isUpperCase() }
        }
    }

    @test fun any() = withOneCharSequenceArg("a1bc") { data ->
        assertTrue {
            data.any() { it.isDigit() }
        }
        assertFalse {
            data.any() { it.isUpperCase() }
        }
    }

    @test fun joinTo() {
        val data = "kotlin".toList()
        val sb = StringBuilder()
        data.joinTo(sb, "^", "<", ">")
        assertEquals("<k^o^t^l^i^n>", sb.toString())
    }

    @test fun find() = withOneCharSequenceArg("a1b2c3") { data ->
        assertEquals('1', data.first { it.isDigit() })
        assertNull(data.firstOrNull { it.isUpperCase() })
    }

    @test fun findNot() = withOneCharSequenceArg("1a2b3c") { data ->
        assertEquals('a', data.filterNot { it.isDigit() }.firstOrNull())
        assertNull(data.filterNot { it.isJavaIdentifierPart() }.firstOrNull())
    }

    @test fun partition() = withOneCharSequenceArg("a1b2c3") { data ->
        val pair = data.partition { it.isDigit() }
        assertEquals("123", pair.first, "pair.first")
        assertEquals("abc", pair.second, "pair.second")
    }

    @test fun map() = withOneCharSequenceArg { arg1 ->
        assertEquals(listOf('a', 'b', 'c'), arg1("abc").map { it })

        assertEquals(listOf(true, false, true), arg1("AbC").map { it.isUpperCase() })

        assertEquals(listOf<Boolean>(), arg1("").map { it.isUpperCase() })

        assertEquals(listOf(97, 98, 99), arg1("abc").map { it.toInt() })
    }

    @test fun mapTo() = withOneCharSequenceArg { arg1 ->
        val result1 = arrayListOf<Char>()
        val return1 = arg1("abc").mapTo(result1, { it })
        assertEquals(result1, return1)
        assertEquals(arrayListOf('a', 'b', 'c'), result1)

        val result2 = arrayListOf<Boolean>()
        val return2 = arg1("AbC").mapTo(result2, { it.isUpperCase() })
        assertEquals(result2, return2)
        assertEquals(arrayListOf(true, false, true), result2)

        val result3 = arrayListOf<Boolean>()
        val return3 = arg1("").mapTo(result3, { it.isUpperCase() })
        assertEquals(result3, return3)
        assertEquals(arrayListOf<Boolean>(), result3)

        val result4 = arrayListOf<Int>()
        val return4 = arg1("abc").mapTo(result4, { it.toInt() })
        assertEquals(result4, return4)
        assertEquals(arrayListOf(97, 98, 99), result4)
    }

    @test fun flatMap() = withOneCharSequenceArg("abcd") { data ->
        val result = data.flatMap { ('a'..it) + ' ' }
        assertEquals("a ab abc abcd ".toCharList(), result)
    }

    @test fun fold() = withOneCharSequenceArg { arg1 ->
        // calculate number of digits in the string
        val data = arg1("a1b2c3def")
        val result = data.fold(0, { digits, c -> if(c.isDigit()) digits + 1 else digits } )
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, arg1("ABCD").fold(true, { r, c -> r && c.isUpperCase() }))

        //get string back
        assertEquals(data.toString(), data.fold("", { s, c -> s + c }))
    }

    @test fun foldRight() = withOneCharSequenceArg { arg1 ->
        // calculate number of digits in the string
        val data = arg1("a1b2c3def")
        val result = data.foldRight(0, { c, digits -> if(c.isDigit()) digits + 1 else digits })
        assertEquals(3, result)

        //simulate all method
        assertEquals(true, arg1("ABCD").foldRight(true, { c, r -> r && c.isUpperCase() }))

        //get string back
        assertEquals(data.toString(), data.foldRight("", { s, c -> "" + s + c }))
    }

    @test fun reduce() = withOneCharSequenceArg { arg1 ->
        // get the smallest character(by char value)
        assertEquals('a', arg1("bacfd").reduce { v, c -> if (v > c) c else v })

        assertFailsWith(UnsupportedOperationException::class) {
            arg1("").reduce { a, b -> '\n' }
        }
    }

    @test fun reduceRight() = withOneCharSequenceArg { arg1 ->
        // get the smallest character(by char value)
        assertEquals('a', arg1("bacfd").reduceRight { c, v -> if (v > c) c else v })

        assertFailsWith(UnsupportedOperationException::class) {
            arg1("").reduceRight { a, b -> '\n' }
        }
    }

    @test fun groupBy() = withOneCharSequenceArg("abAbaABcD") { data ->
        // group characters by their case
        val result = data.groupBy { it.isLowerCase() }
        assertEquals(2, result.size)
        assertEquals(listOf('a','b','b','a','c'), result[true])
        assertEquals(listOf('A','A','B','D'), result[false])
    }

    @test fun joinToString() {
        val data = "abcd".toList()
        val result = data.joinToString("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring".toList()
        val result2 = data2.joinToString("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)

        val data3 = "a1/b".toList()
        val result3 = data3.joinToString() { it.toUpperCase().toString() }
        assertEquals("A, 1, /, B", result3)
    }

    @test fun join() {
        val data = "abcd".map { it.toString() }
        val result = data.joinToString("_", "(", ")")
        assertEquals("(a_b_c_d)", result)

        val data2 = "verylongstring".map { it.toString() }
        val result2 = data2.joinToString("-", "[", "]", 11, "oops")
        assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)
    }



    @test fun dropWhile() {
        val data = "ab1cd2"
        assertEquals("1cd2", data.dropWhile { it.isJavaIdentifierStart() })
        assertEquals("", data.dropWhile { true })
        assertEquals("ab1cd2", data.dropWhile { false })
    }

    @test fun dropWhileCharSequence() = withOneCharSequenceArg("ab1cd2") { data ->
        assertContentEquals("1cd2", data.dropWhile { it.isJavaIdentifierStart() })
        assertContentEquals("", data.dropWhile { true })
        assertContentEquals("ab1cd2", data.dropWhile { false })
    }


    @test fun drop() {
        val data = "abcd1234"
        assertEquals("d1234", data.drop(3))
        assertFails {
            data.drop(-2)
        }
        assertEquals("", data.drop(data.length() + 5))
    }

    @test fun dropCharSequence() = withOneCharSequenceArg("abcd1234") { data ->
        assertContentEquals("d1234", data.drop(3))
        assertFails {
            data.drop(-2)
        }
        assertContentEquals("", data.drop(data.length() + 5))
    }

    @test fun takeWhile() {
        val data = "ab1cd2"
        assertEquals("ab", data.takeWhile { it.isJavaIdentifierStart() })
        assertEquals("", data.takeWhile { false })
        assertEquals("ab1cd2", data.takeWhile { true })
    }

    @test fun takeWhileCharSequence() = withOneCharSequenceArg("ab1cd2") { data ->
        assertContentEquals("ab", data.takeWhile { it.isJavaIdentifierStart() })
        assertContentEquals("", data.takeWhile { false })
        assertContentEquals("ab1cd2", data.takeWhile { true })
    }

    @test fun take() {
        val data = "abcd1234"
        assertEquals("abc", data.take(3))
        assertFails {
            data.take(-7)
        }
        assertEquals(data, data.take(data.length() + 42))
    }

    @test fun takeCharSequence() = withOneCharSequenceArg("abcd1234") { data ->
        assertEquals("abc", data.take(3))
        assertFails {
            data.take(-7)
        }
        assertContentEquals(data.toString(), data.take(data.length + 42))
    }

    @test fun formatter() {
        assertEquals("12", "%d%d".format(1, 2))

        assertEquals("1,234,567.890", "%,.3f".format(Locale.ENGLISH, 1234567.890))
        assertEquals("1.234.567,890", "%,.3f".format(Locale.GERMAN, 1234567.890))
        assertEquals("1 234 567,890", "%,.3f".format(Locale("fr"), 1234567.890))
    }

    @test fun toByteArrayEncodings() {
        val s = "hello"
        val defaultCharset = java.nio.charset.Charset.defaultCharset()!!
        assertEquals(String(s.toByteArray()), String(s.toByteArray(defaultCharset)))
        assertEquals(String(s.toByteArray()), String(s.toByteArray(defaultCharset.name())))
    }

    @test fun testReplaceAllClosure() = withOneCharSequenceArg("test123zzz") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("test[123]zzz", result)
    }

    @test fun testReplaceAllClosureAtStart() = withOneCharSequenceArg("123zzz") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("[123]zzz", result)
    }

    @test fun testReplaceAllClosureAtEnd() = withOneCharSequenceArg("test123") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "[" + mr.value + "]"
        }
        assertEquals("test[123]", result)
    }

    @test fun testReplaceAllClosureEmpty() = withOneCharSequenceArg("") { s ->
        val result = s.replace("\\d+".toRegex()) { mr ->
            "x"
        }
        assertEquals("", result)

    }

    @test fun slice() = withOneCharSequenceArg { arg1 ->
        val iter = listOf(4, 3, 0, 1)

        val data = arg1("ABCDabcd")
        // ABCDabcd
        // 01234567
        assertEquals("BCDabc", data.slice(1..6).toString())
        assertEquals("baD", data.slice(5 downTo 3).toString())
        assertEquals("aDAB", data.slice(iter).toString())

        assertFails {
            arg1("abc").slice(listOf(1,4))
        }
        assertFails {
            data.slice(listOf(10))
        }
    }


    @test fun orderIgnoringCase() {
        val list = listOf("Beast", "Ast", "asterisk")
        assertEquals(listOf("Ast", "Beast", "asterisk"), list.sorted())
        assertEquals(listOf("Ast", "asterisk", "Beast"), list.sortedWith(String.CASE_INSENSITIVE_ORDER))
    }
}
