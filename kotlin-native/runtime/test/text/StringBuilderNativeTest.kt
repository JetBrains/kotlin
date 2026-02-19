/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

// Native-specific part of stdlib/test/text/StringBuilderTest.kt
class StringBuilderNativeTest {
    @Test
    fun insertCharSequence() {
        StringBuilder("my insert CharSequence test").let { sb ->
            sb.insert(0, null as CharSequence?)
            assertEquals("nullmy insert CharSequence test", sb.toString())
            sb.insert(2, null as CharSequence?)
            assertEquals("nunullllmy insert CharSequence test", sb.toString())
            sb.insert(sb.length, null as CharSequence?)
            assertEquals("nunullllmy insert CharSequence testnull", sb.toString())
            sb.insert(2, "12" as CharSequence)
            assertEquals("nu12nullllmy insert CharSequence testnull", sb.toString())
            sb.insert(sb.length, "12" as CharSequence)
            assertEquals("nu12nullllmy insert CharSequence testnull12", sb.toString())
        }
    }

    @Test
    fun insertString() {
        StringBuilder("my insert String test").let { sb ->
            sb.insertRange(0, "1234", 0, 0)
            assertEquals("my insert String test", sb.toString())
            sb.insertRange(0, "1234", 0, 1)
            assertEquals("1my insert String test", sb.toString())
            sb.insertRange(0, "1234", 1, 3)
            assertEquals("231my insert String test", sb.toString())
            sb.insertRange(2, "1234", 0, 0)
            assertEquals("231my insert String test", sb.toString())
            sb.insertRange(2, "1234", 0, 1)
            assertEquals("2311my insert String test", sb.toString())
            sb.insertRange(2, "1234", 1, 3)
            assertEquals("232311my insert String test", sb.toString())
            sb.insertRange(sb.length, "1234", 0, 0)
            assertEquals("232311my insert String test", sb.toString())
            sb.insertRange(sb.length, "1234", 0, 1)
            assertEquals("232311my insert String test1", sb.toString())
            sb.insertRange(sb.length, "1234", 1, 3)
            assertEquals("232311my insert String test123", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, "_") }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, "_") }
            assertFails { sb.insertRange(0, "null", -1, 0) }
            assertFails { sb.insertRange(0, "null", 0, 5) }
            assertFails { sb.insertRange(0, "null", 2, 1) }
        }
    }

    @Test
    fun insertByte() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.toByte())
            assertEquals("42", sb.toString())
            sb.insert(0, 13.toByte())
            assertEquals("1342", sb.toString())
            sb.insert(3, -1.toByte())
            assertEquals("134-12", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.toByte()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.toByte()) }
        }
    }

    @Test
    fun insertShort() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.toShort())
            assertEquals("42", sb.toString())
            sb.insert(0, 13.toShort())
            assertEquals("1342", sb.toString())
            sb.insert(3, -1.toShort())
            assertEquals("134-12", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.toShort()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.toShort()) }
        }
    }

    @Test
    fun insertInt() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.toInt())
            assertEquals("42", sb.toString())
            sb.insert(0, 13.toInt())
            assertEquals("1342", sb.toString())
            sb.insert(3, -1.toInt())
            assertEquals("134-12", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.toInt()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.toInt()) }
        }
    }

    @Test
    fun insertLong() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.toLong())
            assertEquals("42", sb.toString())
            sb.insert(0, 13.toLong())
            assertEquals("1342", sb.toString())
            sb.insert(3, -1.toLong())
            assertEquals("134-12", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.toLong()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.toLong()) }
        }
    }

    @Test
    fun insertFloat() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.2.toFloat())
            assertEquals("42.2", sb.toString())
            sb.insert(0, 13.1.toFloat())
            assertEquals("13.142.2", sb.toString())
            sb.insert(3, -1.toFloat())
            assertEquals("13.-1.0142.2", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.2.toFloat()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.2.toFloat()) }
        }
    }

    @Test
    fun insertDouble() {
        StringBuilder().let { sb ->
            sb.insert(0, 42.2.toDouble())
            assertEquals("42.2", sb.toString())
            sb.insert(0, 13.1.toDouble())
            assertEquals("13.142.2", sb.toString())
            sb.insert(3, -1.toDouble())
            assertEquals("13.-1.0142.2", sb.toString())

            assertFailsWith<IndexOutOfBoundsException> { sb.insert(-1, 42.2.toDouble()) }
            assertFailsWith<IndexOutOfBoundsException> { sb.insert(sb.length + 1, 42.2.toDouble()) }
        }
    }

    @Test
    fun testReverse() {
        val sb = StringBuilder("123456")
        assertTrue(sb === sb.reverse())
        assertEquals("654321", sb.toString())

        sb.setLength(1)
        assertEquals("6", sb.toString())

        sb.setLength(0)
        assertEquals("", sb.toString())
    }

    @Test
    fun testDoubleReverse() {
        fun assertReversed(original: String, reversed: String, reversedBack: String = original) {
            assertEquals(reversed, StringBuilder(original).reverse().toString())
            assertEquals(reversedBack, StringBuilder(reversed).reverse().toString())
        }

        assertReversed("a", "a")
        assertReversed("ab", "ba")
        assertReversed("abcdef", "fedcba")
        assertReversed("abcdefg", "gfedcba")
        assertReversed("\ud800\udc00", "\ud800\udc00")
        assertReversed("\udc00\ud800", "\ud800\udc00", "\ud800\udc00")
        assertReversed("a\ud800\udc00", "\ud800\udc00a")
        assertReversed("ab\ud800\udc00", "\ud800\udc00ba")
        assertReversed("abc\ud800\udc00", "\ud800\udc00cba")
        assertReversed("\ud800\udc00\udc01\ud801\ud802\udc02", "\ud802\udc02\ud801\udc01\ud800\udc00", "\ud800\udc00\ud801\udc01\ud802\udc02")
        assertReversed("\ud800\udc00\ud801\udc01\ud802\udc02", "\ud802\udc02\ud801\udc01\ud800\udc00")
        assertReversed("\ud800\udc00\udc01\ud801a", "a\ud801\udc01\ud800\udc00", "\ud800\udc00\ud801\udc01a")
        assertReversed("a\ud800\udc00\ud801\udc01", "\ud801\udc01\ud800\udc00a")
        assertReversed("\ud800\udc00\udc01\ud801ab", "ba\ud801\udc01\ud800\udc00", "\ud800\udc00\ud801\udc01ab")
        assertReversed("ab\ud800\udc00\ud801\udc01", "\ud801\udc01\ud800\udc00ba")
        assertReversed("\ud800\udc00\ud801\udc01", "\ud801\udc01\ud800\udc00")
        assertReversed("a\ud800\udc00z\ud801\udc01", "\ud801\udc01z\ud800\udc00a")
        assertReversed("a\ud800\udc00bz\ud801\udc01", "\ud801\udc01zb\ud800\udc00a")
        assertReversed("abc\ud802\udc02\ud801\udc01\ud800\udc00", "\ud800\udc00\ud801\udc01\ud802\udc02cba")
        assertReversed("abcd\ud802\udc02\ud801\udc01\ud800\udc00", "\ud800\udc00\ud801\udc01\ud802\udc02dcba")
    }

    @Test
    fun appendLong() {
        val times = 100
        val expected = (12345678L until (12345678L + times)).fold("") { res, idx -> res + idx }

        val sb = StringBuilder()
        repeat(times) { sb.append(12345678L + it) }
        assertEquals(expected, sb.toString())

        val cornerCase = listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)
        val expectedCornerCase = cornerCase.fold("") { res, e -> res + e }
        for (v in cornerCase) sb.append(v)
        assertEquals(expected + expectedCornerCase, sb.toString())
    }

    @Test
    fun appendNullCharSequence() {
        val sb = StringBuilder()
        sb.append(null as CharSequence?)
        assertEquals("null", sb.toString())
    }

    @Test
    fun appendLine() {
        val sb = StringBuilder()
        sb.appendLine("abc").appendLine(42).appendLine(0.1)
        assertEquals("abc\n42\n0.1\n", sb.toString())
    }
}