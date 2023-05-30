/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.TestPlatform
import test.testOnlyOn
import test.testExceptOn
import kotlin.random.Random
import kotlin.test.*
import kotlin.text.*

class StringBuilderTest {

    @Test fun stringBuild() {
        val s = buildString {
            append("a")
            append(true)
        }
        assertEquals("atrue", s)
    }

    @Test fun appendMany() {
        assertEquals("a1", StringBuilder().append("a", "1").toString())
        assertEquals("a1", StringBuilder().append("a", 1).toString())
        assertEquals("a1", StringBuilder().append("a", StringBuilder().append("1")).toString())
    }

    @Test fun append() {
        // this test is needed for JS implementation
        assertEquals("em", buildString {
            appendRange("element", 2, 4)
        })
    }

    // KT-52336
    @Test
    @Suppress("DEPRECATION_ERROR")
    fun deprecatedAppend() {
        val chars = charArrayOf('a', 'b', 'c', 'd')
        val sb = StringBuilder()
        testOnlyOn(TestPlatform.Jvm) {
            sb.append(chars, 1, 2) // Should fail after KT-15220 gets fixed
            assertEquals("bc", sb.toString())
        }
        testExceptOn(TestPlatform.Jvm) {
            assertFailsWith<NotImplementedError> {
                sb.append(chars, 1, 2)
            }
        }
    }

    @Test fun asCharSequence() {
        val original = "Some test string"
        val sb = StringBuilder(original)
        val result = sb.toString()
        val cs = sb as CharSequence

        assertEquals(result.length, cs.length)
        assertEquals(result.length, sb.length)
        for (index in result.indices) {
            assertEquals(result[index], sb[index])
            assertEquals(result[index], cs[index])
        }
        assertEquals(result.substring(2, 6), cs.subSequence(2, 6).toString())
    }

    @Test fun constructors() {
        StringBuilder().let { sb ->
            assertEquals(0, sb.length)
            assertEquals("", sb.toString())
        }

        StringBuilder(16).let { sb ->
            assertEquals(0, sb.length)
            assertEquals("", sb.toString())
        }

        StringBuilder("content").let { sb ->
            assertEquals(7, sb.length)
            assertEquals("content", sb.toString())
        }

        StringBuilder(StringBuilder("content")).let { sb ->
            assertEquals(7, sb.length)
            assertEquals("content", sb.toString())
        }
    }

    @Test fun clear() {
        val sb = StringBuilder()
        sb.append("test")
        val s = sb.toString()
        sb.clear()
        assertTrue(sb.isEmpty())
        assertEquals("test", s)
    }

    @Test fun get() {
        val sb = StringBuilder()
        sb.append("test")
        assertEquals('t', sb[0])
        assertEquals('e', sb[1])
        assertEquals('s', sb[2])
        assertEquals('t', sb[3])

        assertFailsWith<IndexOutOfBoundsException> { assertEquals('t', sb[-1]) }
        assertFailsWith<IndexOutOfBoundsException> { assertEquals('t', sb[4]) }
    }

    @Test
    fun reverse() {
        StringBuilder("my reverse test").let { sb ->
            sb.reverse()
            assertEquals("tset esrever ym", sb.toString())

            sb.append('\uD800')
            sb.append('\uDC00')

            sb.insert(10, '\uDC01')
            sb.insert(11, '\uD801')
            sb.insert(0, "\uD802\uDC02")

            sb.reverse()
            assertEquals("\uD800\uDC00my re\uD801\uDC01verse test\uD802\uDC02", sb.toString())
        }
    }

    @Test
    fun appendChar() {
        val times = 100
        val expected = "a".repeat(times)

        val sb = StringBuilder()
        repeat(times) { sb.append('a') }
        assertEquals(expected, sb.toString())

        sb.append('\uD800')
        sb.append('\uDC00')
        assertEquals(expected + "\uD800\uDC00", sb.toString())
    }

    @Test
    fun appendInt() {
        val times = 100
        val expected = (0 until times).fold("") { res, idx -> res + idx }

        val sb = StringBuilder()
        repeat(times) { sb.append(it) }
        assertEquals(expected, sb.toString())

        val cornerCase = listOf(0, -1, Int.MIN_VALUE, Int.MAX_VALUE)
        val expectedCornerCase = cornerCase.fold("") { res, e -> res + e }
        for (int in cornerCase) sb.append(int)
        assertEquals(expected + expectedCornerCase, sb.toString())
    }

    @Test
    fun appendBoolean() {
        StringBuilder().append(true).append(false).append(true).append(true).let { sb ->
            assertEquals("truefalsetruetrue", sb.toString())
        }
    }


    @Test
    fun appendString() {
        val times = 100
        val expected = "foo".repeat(times)

        StringBuilder().let { sb ->
            repeat(times) { sb.append("foo") }
            assertEquals(expected, sb.toString())

            sb.append(null as String?)
            assertEquals(expected + "null", sb.toString())
        }
    }

    @Test
    fun appendAny() {
        val myAny = object {
            override fun toString(): String = "It's My Any!"
        }

        StringBuilder().let { sb ->
            sb.append(null as Any?)
            sb.append(myAny)
            assertEquals("nullIt's My Any!", sb.toString())
        }
    }

    @Test
    fun appendCharArray() {
        StringBuilder().let { sb ->
            val times = 100
            val expected = "foo".repeat(times)

            repeat(times) { sb.append(charArrayOf('f', 'o', 'o')) }
            assertEquals(expected, sb.toString())
        }

        StringBuilder().let { sb ->
            val charArray = charArrayOf(
                'm', 'y', ' ', 'a', 'p', 'p', 'e', 'n', 'd', ' ', 'c', 'h', 'a', 'r', ' ', 'a', 'r', 'r', 'a', 'y', ' ', 't', 'e', 's', 't'
            )

            sb.appendRange(charArray, 0, charArray.size /*25*/)
            sb.appendRange(charArray, 0, 9)
            sb.appendRange(charArray, 15, 25)
            sb.appendRange(charArray, 9, 15)

            assertEquals("my append char array testmy appendarray test char ", sb.toString())

            assertFails { sb.appendRange(charArrayOf('_', '*', '#'), -1, 0) }
            assertFails { sb.appendRange(charArrayOf('_', '*', '#'), 0, 4) }
            assertFails { sb.appendRange(charArrayOf('_', '*', '#'), 2, 1) }
            assertFails { sb.appendRange(charArrayOf('_', '*', '#'), 2, -1) }
        }
    }

    @Test
    fun deleteChar() {
        StringBuilder("my delete test").let { sb ->
            sb.deleteAt(0)
            assertEquals("y delete test", sb.toString())
            sb.deleteAt(5)
            assertEquals("y delte test", sb.toString())
            sb.deleteAt(11)
            assertEquals("y delte tes", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.deleteAt(11) }
            assertFailsWith<IndexOutOfBoundsException> { sb.deleteAt(-1) }
        }
    }

    @Test
    fun deleteSubstring() {
        StringBuilder("my delete substring test").let { sb ->
            sb.deleteRange(0, 2)
            assertEquals(" delete substring test", sb.toString())
            sb.deleteRange(7, 17)
            assertEquals(" delete test", sb.toString())
            sb.deleteRange(8, 12)
            assertEquals(" delete ", sb.toString())
            sb.deleteRange(8, 12)
            assertEquals(" delete ", sb.toString())

            assertFails { sb.deleteRange(-1, 1) }
            assertFails { sb.deleteRange(0, -1) }
            assertFails { sb.deleteRange(2, 1) }
            assertFails { sb.deleteRange(sb.length + 1, sb.length + 2) }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun capacityTest() {
        testExceptOn(TestPlatform.Js) {
            assertEquals(100, StringBuilder(100).capacity()) // not implemented in JS
        }
        StringBuilder("string builder from string capacity test").let { sb ->
            assertTrue(sb.capacity() >= sb.length)
        }

        StringBuilder().let { sb ->
            assertTrue(sb.capacity() >= sb.length)
            repeat(Random.nextInt(17, 30)) { sb.append('c') }
            assertTrue(sb.capacity() >= sb.length)
            repeat(Random.nextInt(35, 62)) { sb.insert(0, "s") }
            sb.ensureCapacity(1)
            assertTrue(sb.capacity() >= sb.length)
            sb.ensureCapacity(-1) // negative argument is ignored
            assertTrue(sb.capacity() >= sb.length)
            sb.ensureCapacity(sb.length * 10)
            testExceptOn(TestPlatform.Js) {
                assertTrue(sb.capacity() >= sb.length * 10) // not implemented in JS
            }
        }
    }

    @Test
    fun overflow() = testExceptOn(TestPlatform.Js) {
        class CharSeq(override val length: Int) : CharSequence {
            override fun get(index: Int): Char =
                throw IllegalStateException("Not expected to be called")

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
                throw IllegalStateException("Not expected to be called")
        }

        val initialContent = "a".repeat(20)
        val bigCharSeq = CharSeq(Int.MAX_VALUE - initialContent.length + 1)
        assertFailsWith<Error> { // OutOfMemoryError
            StringBuilder(initialContent).append(bigCharSeq)
        }
        assertFailsWith<Error> { // OutOfMemoryError
            StringBuilder(initialContent).insert(5, bigCharSeq)
        }
    }

    @Test
    fun indexOf() {
        StringBuilder("my indexOf test").let { sb ->
            assertEquals(0, sb.indexOf(""))
            assertEquals(5, sb.indexOf("", 5))
            assertEquals(sb.length, sb.indexOf("", sb.length))
            assertEquals(sb.length, sb.indexOf("", 100))    // Java implementation, should be -1

            assertEquals(6, sb.indexOf("e"))
            assertEquals(12, sb.indexOf("e", 7))
            assertEquals(-1, sb.indexOf("e", 13))
            assertEquals(-1, sb.indexOf("e", 100))

            assertEquals(11, sb.indexOf("test"))
            assertEquals(11, sb.indexOf("test", 11))
            assertEquals(-1, sb.indexOf("test", 12))
        }
    }

    @Test
    fun lastIndexOf() {
        StringBuilder("my lastIndexOf test").let { sb ->
            assertEquals(sb.length, sb.lastIndexOf(""))
            assertEquals(sb.length, sb.lastIndexOf("", 100))
            assertEquals(5, sb.lastIndexOf("", 5))
            assertEquals(0, sb.lastIndexOf("", 0))
            assertEquals(-1, sb.lastIndexOf("", -100))

            assertEquals(16, sb.lastIndexOf("e"))
            assertEquals(10, sb.lastIndexOf("e", 15))
            assertEquals(-1, sb.lastIndexOf("e", 9))
            assertEquals(-1, sb.lastIndexOf("e", -100))

            assertEquals(15, sb.lastIndexOf("test"))
            assertEquals(15, sb.lastIndexOf("test", 15))
            assertEquals(-1, sb.lastIndexOf("test", 14))
        }
    }

    @Test
    fun insertBoolean() {
        StringBuilder().let { sb ->
            sb.insert(0, true)
            assertEquals("true", sb.toString())
            sb.insert(0, false)
            assertEquals("falsetrue", sb.toString())
            sb.insert(5, true)
            assertEquals("falsetruetrue", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, false) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, false) }
        }
    }

    @Test
    fun insertChar() {
        StringBuilder("my insert char test").let { sb ->
            sb.insert(0, '_')
            assertEquals("_my insert char test", sb.toString())
            sb.insert(10, 'T')
            assertEquals("_my insertT char test", sb.toString())
            sb.insert(21, '_')
            assertEquals("_my insertT char test_", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, '_') }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, '_') }
        }
    }

    @Test
    fun insertCharArray() {
        StringBuilder("my insert CharArray test").let { sb ->
            sb.insert(0, charArrayOf('_'))
            assertEquals("_my insert CharArray test", sb.toString())
            sb.insert(10, charArrayOf('T'))
            assertEquals("_my insertT CharArray test", sb.toString())
            sb.insertRange(26, charArrayOf('_', '*', '#'), 0, 1)
            assertEquals("_my insertT CharArray test_", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, charArrayOf('_')) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, charArrayOf('_')) }
            assertFails { sb.insertRange(0, charArrayOf('_', '*', '#'), -1, 0) }
            assertFails { sb.insertRange(0, charArrayOf('_', '*', '#'), 0, 4) }
            assertFails { sb.insertRange(0, charArrayOf('_', '*', '#'), 2, 1) }
            assertFails { sb.insertRange(0, charArrayOf('_', '*', '#'), 2, -1) }

            // Test insertion of large arrays that are likely to trigger increase of underlying array capacity
            sb.insert(0, CharArray(1000) { ',' })
            assertTrue(sb.toString().take(1000).all { it == ',' })
            assertEquals("_my insertT CharArray test_", sb.toString().drop(1000))

            sb.insertRange(0, CharArray(10000) { '@' }, 1000, 9000)
            assertTrue(sb.toString().take(8000).all { it == '@' })
        }
    }

    @Test
    fun insertCharSequence() {
        StringBuilder("my insert CharSequence test").let { sb ->
            sb.insert(0, "MMM" as CharSequence)
            assertEquals("MMMmy insert CharSequence test", sb.toString())
            sb.insert(12, StringBuilder("T"))
            assertEquals("MMMmy insertT CharSequence test", sb.toString())
            sb.insertRange(31, "_*#", 0, 1)
            assertEquals("MMMmy insertT CharSequence test_", sb.toString())
            sb.insertRange(0, "null" as CharSequence, 0, 2)
            assertEquals("nuMMMmy insertT CharSequence test_", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, "_" as CharSequence) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, StringBuilder("_")) }
            assertFails { sb.insertRange(0, "null" as CharSequence, -1, 0) }
            assertFails { sb.insertRange(0, "null" as CharSequence, 0, 5) }
            assertFails { sb.insertRange(0, "null" as CharSequence, 2, 1) }
        }
    }

    @Test
    fun insertAny() {
        val myAny = object {
            override fun toString(): String = "It's My Any!"
        }

        StringBuilder().let { sb ->
            sb.insert(0, null as Any?)
            sb.insert(2, myAny)
            assertEquals("nuIt's My Any!ll", sb.toString())
        }
    }

    @Test
    fun insertString() {
        StringBuilder("my insert string test").let { sb ->
            sb.insert(0, "_")
            assertEquals("_my insert string test", sb.toString())
            sb.insert(10, "TtT")
            assertEquals("_my insertTtT string test", sb.toString())
            sb.insert(25, "_!_")
            assertEquals("_my insertTtT string test_!_", sb.toString())
            sb.insert(13, null as String?)
            assertEquals("_my insertTtTnull string test_!_", sb.toString())
        }
    }

    @Test
    fun setLength() {
        StringBuilder("my setLength test").let { sb ->
            sb.setLength(17)
            assertEquals("my setLength test", sb.toString())
            sb.setLength(0)
            assertEquals("", sb.toString())
            sb.setLength(5)
            assertEquals("\u0000\u0000\u0000\u0000\u0000", sb.toString())

            assertFails { sb.setLength(-1) }
        }
    }

    @Test
    fun substring() {
        StringBuilder("my substring test").let { sb ->
            assertEquals("my ", sb.substring(0, 3))
            assertEquals("substring", sb.substring(3, 12))
            assertEquals("ing test", sb.substring(9))
            assertEquals("ing test", sb.substring(9, 17))

            assertFails { sb.substring(-1) }
            assertFails { sb.substring(0, -1) }
            assertFails { sb.substring(0, sb.length + 1) }
            assertFails { sb.substring(2, 1) }
        }
    }

    @Test
    fun trimToSize() {
        StringBuilder("my trimToSize test").let { sb ->
            assertEquals(18, sb.length)
//            assertTrue(sb.capacity() >= sb.length)
            sb.append('1')
            sb.trimToSize()
            assertEquals(19, sb.length)
//            assertTrue(sb.capacity() >= sb.length)
        }
    }

    @Test
    fun set() {
        StringBuilder("my set test").let { sb ->
            sb[0] = 'M'
            assertEquals("My set test", sb.toString())
            sb[2] = 'm'
            assertEquals("Mymset test", sb.toString())
            sb[10] = 'T'
            assertEquals("Mymset tesT", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb[-1] = '_' }
            assertFailsWith<IndexOutOfBoundsException> { sb[sb.length] = '_' }
        }
    }

    @Test
    fun setRange() {
        StringBuilder("my replace test").let { sb ->
            sb.setRange(0, 4, "R")
            assertEquals("Replace test", sb.toString())
            sb.setRange(7, 7, " empty string")
            assertEquals("Replace empty string test", sb.toString())
            sb.setRange(20, 25, "")
            assertEquals("Replace empty string", sb.toString())
            sb.setRange(20, 25, "")
            assertEquals("Replace empty string", sb.toString())

            assertFails { sb.setRange(-1, 0, "") }
            assertFails { sb.setRange(0, -1, "") }
            assertFails { sb.setRange(2, 1, "") }
            assertFails { sb.setRange(sb.length + 1, sb.length + 2, "") }
        }
    }

    @Test
    fun toCharArray() {
        StringBuilder("my toCharArray test").let { sb ->
            val chars = CharArray(10) { '_' }

            sb.toCharArray(chars, 8, 0, 2)
            assertEquals("________my", chars.concatToString())
            sb.toCharArray(chars, 3, 6, 11)
            assertEquals("___harArmy", chars.concatToString())
            sb.toCharArray(chars, 0, 16, 19)
            assertEquals("estharArmy", chars.concatToString())

            sb.setLength(5)

            assertFails { sb.toCharArray(chars, -1, 0, 1) }
            assertFails { sb.toCharArray(chars, chars.size, 0, 1) }
            assertFails { sb.toCharArray(chars, 0, -1, 0) }
            assertFails { sb.toCharArray(chars, 0, 0, -1) }
            assertFails { sb.toCharArray(chars, 0, 2, 1) }
            assertFails { sb.toCharArray(chars, 0, 0, sb.length + 1) }
        }
    }

    @Test
    fun appendLine() {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine('c')
        stringBuilder.appendLine("string")
        stringBuilder.appendLine(true)
        stringBuilder.appendLine(charArrayOf('a', 'r', 'r', 'a', 'y'))
        stringBuilder.appendLine()
        stringBuilder.appendLine("char sequence" as CharSequence)
        stringBuilder.appendLine(null as Any?)
        stringBuilder.appendLine("nonnull" as Any?)
        stringBuilder.appendLine(null as String?)
        stringBuilder.appendLine(null as CharSequence?)

        val expected =
            """
            c
            string
            true
            array
            
            char sequence
            null
            nonnull
            null
            null
            
            """.trimIndent()

        assertEquals(expected, stringBuilder.toString())
    }
}
