/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.text.unicode.*
import kotlin.test.*

@OptIn(ExperimentalUnicodeApi::class)
class CodePointIterationTest {

    @Test
    fun codePointAtBefore() {
        fun <T> testCodePointAtBefore(
            s: (String) -> T,
            size: T.() -> Int,
            codePointAt: T.(Int) -> CodePoint,
            codePointBefore: T.(Int) -> CodePoint,
        ) {
            val string = s("abc😀def")
            val unpairedSurrogates = s(charArrayOf('a', Char.MIN_HIGH_SURROGATE, 'b', Char.MIN_LOW_SURROGATE).concatToString())

            run {
                val c2 = string.codePointAt(2)
                val c3 = string.codePointAt(3)
                val c4 = string.codePointAt(4)
                assertEquals('c', c2.toSingleChar())
                assertEquals(0x1F600, c3.code)
                val [high, low] = c3.toSurrogatePair()
                assertEquals(low, c4.toSingleChar())

                assertFailsWith<IndexOutOfBoundsException> { string.codePointAt(-1) }
                assertFailsWith<IndexOutOfBoundsException> { string.codePointAt(string.size()) }

                assertEquals(Char.MIN_HIGH_SURROGATE, unpairedSurrogates.codePointAt(1).toSingleChar())
                assertEquals(Char.MIN_LOW_SURROGATE, unpairedSurrogates.codePointAt(3).toSingleChar())
            }

            run {
                val c2 = string.codePointBefore(3)
                val c3 = string.codePointBefore(4)
                val c4 = string.codePointBefore(5)
                assertEquals('c', c2.toSingleChar())
                assertEquals(0x1F600, c4.code)
                val [high, low] = c4.toSurrogatePair()
                assertEquals(high, c3.toSingleChar())
                assertEquals('f', string.codePointBefore(string.size()).toSingleChar())

                assertFailsWith<IndexOutOfBoundsException> { string.codePointBefore(0) }
                assertFailsWith<IndexOutOfBoundsException> { string.codePointBefore(string.size() + 1) }

                assertEquals(Char.MIN_HIGH_SURROGATE, unpairedSurrogates.codePointBefore(2).toSingleChar())
                assertEquals(Char.MIN_LOW_SURROGATE, unpairedSurrogates.codePointBefore(4).toSingleChar())
            }
        }

        testCodePointAtBefore({ it }, String::length, String::codePointAt, String::codePointBefore)
        testCodePointAtBefore({ it as CharSequence }, CharSequence::length, CharSequence::codePointAt, CharSequence::codePointBefore)
        testCodePointAtBefore({ StringBuilder(it) }, CharSequence::length, CharSequence::codePointAt, CharSequence::codePointBefore)
        testCodePointAtBefore({ it.toCharArray() }, CharArray::size, CharArray::codePointAt, CharArray::codePointBefore)

        run {
            val charArray = "abc😀".toCharArray()
            val [high, low] = charArray.codePointAt(3, 5).toSurrogatePair()
            assertEquals(high, charArray.codePointAt(3, 4).toSingleChar())
            assertEquals(low, charArray.codePointBefore(5, 4).toSingleChar())
            assertEquals(CodePoint.fromSurrogatePair(high, low), charArray.codePointBefore(5, 3))

            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointAt(3, 3) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointAt(3, charArray.size + 1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointAt(-1, -1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointBefore(1, 1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointBefore(1, -1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointBefore(charArray.size + 1, charArray.size) }
        }
    }

    @Test
    fun codePointCount() {
        val string = "abc😀def"
        assertEquals(7, string.codePointCount())
        assertEquals(4, string.codePointCount(startIndex = 3))
        assertEquals(4, string.codePointCount(startIndex = 4))
        assertEquals(4, string.codePointCount(endIndex = 4))
        assertEquals(4, string.codePointCount(endIndex = 5))
        assertFailsWith<IndexOutOfBoundsException> { string.codePointCount(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { string.codePointCount(endIndex = string.length + 1) }
        assertFailsWith<IndexOutOfBoundsException> { string.codePointCount(startIndex = 3, endIndex = 2) }

        val charSeq = StringBuilder(string)
        assertEquals(7, charSeq.codePointCount())
        assertEquals(4, charSeq.codePointCount(startIndex = 3))
        assertEquals(4, charSeq.codePointCount(startIndex = 4))
        assertEquals(4, charSeq.codePointCount(endIndex = 4))
        assertEquals(4, charSeq.codePointCount(endIndex = 5))
        assertFailsWith<IndexOutOfBoundsException> { charSeq.codePointCount(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { charSeq.codePointCount(endIndex = string.length + 1) }
        assertFailsWith<IndexOutOfBoundsException> { charSeq.codePointCount(startIndex = 3, endIndex = 2) }

        val charArray = string.toCharArray()
        assertEquals(7, charArray.codePointCount())
        assertEquals(4, charArray.codePointCount(startIndex = 3))
        assertEquals(4, charArray.codePointCount(startIndex = 4))
        assertEquals(4, charArray.codePointCount(endIndex = 4))
        assertEquals(4, charArray.codePointCount(endIndex = 5))
        assertFailsWith<IndexOutOfBoundsException> { charArray.codePointCount(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.codePointCount(endIndex = string.length + 1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.codePointCount(startIndex = 3, endIndex = 2) }
    }

    @Test
    fun codePointOffset() {
        val string = "abc😀def${Char.MIN_LOW_SURROGATE}-${Char.MAX_HIGH_SURROGATE}"
        assertEquals(5, string.offsetByCodePoints(0, 4))
        assertEquals(3, string.offsetByCodePoints(5, -1))
        assertEquals(3, string.offsetByCodePoints(4, -1))
        assertEquals(string.length - 1, string.offsetByCodePoints(string.length, -1))

        assertFailsWith<IndexOutOfBoundsException> { string.offsetByCodePoints(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { string.offsetByCodePoints(0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { string.offsetByCodePoints(string.length + 1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { string.offsetByCodePoints(string.length, 1) }

        val charSeq = StringBuilder(string)
        assertEquals(5, charSeq.offsetByCodePoints(0, 4))
        assertEquals(3, charSeq.offsetByCodePoints(5, -1))
        assertEquals(3, charSeq.offsetByCodePoints(4, -1))
        assertEquals(charSeq.length - 1, charSeq.offsetByCodePoints(charSeq.length, -1))

        assertFailsWith<IndexOutOfBoundsException> { charSeq.offsetByCodePoints(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { charSeq.offsetByCodePoints(0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { charSeq.offsetByCodePoints(charSeq.length + 1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { charSeq.offsetByCodePoints(charSeq.length, 1) }

        val charArray = string.toCharArray()
        assertEquals(5, charArray.offsetByCodePoints(0, 4))
        assertEquals(3, charArray.offsetByCodePoints(5, -1))
        assertEquals(3, charArray.offsetByCodePoints(4, -1))
        assertEquals(charArray.size - 1, charArray.offsetByCodePoints(charArray.size, -1))

        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(0, -1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(charArray.size + 1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(charArray.size, 1) }

        assertEquals(4, charArray.offsetByCodePoints(0, 4, endIndex = 4))
        assertEquals(4, charArray.offsetByCodePoints(5, -1, startIndex = 4, endIndex = 5))

        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(1, 0, startIndex = 2) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(1, -1, startIndex = 1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(6, 0, endIndex = 5) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(5, 1, endIndex = 5) }

        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(5, 1, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(5, 1, endIndex = charArray.size + 1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.offsetByCodePoints(4, 0, startIndex = 4, endIndex = 3) }
    }

    @Test
    fun forEachIteration() {
        val string = "abc😀def${Char.MIN_LOW_SURROGATE}-${Char.MAX_HIGH_SURROGATE}"

        fun <T> testIteration(s: (String) -> T, forEach: (T, (CodePoint)-> Unit) -> Unit) {
            val source = s(string)
            val result = StringBuilder()
            forEach(source) { c -> result.appendCodePoint(c) }
            assertEquals(string, result.toString())
        }

        testIteration({ it }, { s, action -> s.forEachCodePoint(action) })
        testIteration({ it as CharSequence }, { s, action -> s.forEachCodePoint(action) })
        testIteration({ it.toCharArray() }, { s, action -> s.forEachCodePoint(0, s.size, action = action) })
        testIteration({ "${Char.MIN_HIGH_SURROGATE}$string${Char.MIN_LOW_SURROGATE}".toCharArray() }, { s, action -> s.forEachCodePoint(1, s.size - 1, action) })

        val charArray = string.toCharArray()
        assertFailsWith<IndexOutOfBoundsException> { charArray.forEachCodePoint(startIndex = -1) {} }
        assertFailsWith<IndexOutOfBoundsException> { charArray.forEachCodePoint(endIndex = charArray.size + 1) {} }
        assertFailsWith<IndexOutOfBoundsException> { charArray.forEachCodePoint(startIndex = 1, endIndex = 0) {} }
    }

    @Test
    fun iteration() {
        fun String.codePointSequences(): List<CodePointSequence> = listOf(
            this.codePointSequence(),
            (this as CharSequence).codePointSequence(),
            StringBuilder(this).codePointSequence(),
            this.toCharArray().codePointSequence(),
            "${Char.MIN_HIGH_SURROGATE}$this${Char.MIN_LOW_SURROGATE}".toCharArray().codePointSequence(1, 1 + this.length)
        )

        val string = "abc😀def"
        for (seq in string.codePointSequences()) {
            assertEquals(listOf("a", "b", "c", "😀", "d", "e", "f"), seq.map { it.toString() }.toList())
        }

        val unpairedSurrogates = charArrayOf('a', Char.MIN_HIGH_SURROGATE, 'b', Char.MIN_LOW_SURROGATE).concatToString()
        for (seq in unpairedSurrogates.codePointSequences()) {
            assertEquals(unpairedSurrogates.toList(), seq.map { it.toSingleChar() }.toList())
        }

        val charArray = "abc".toCharArray()
        assertFailsWith<IndexOutOfBoundsException> { charArray.codePointSequence(startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { charArray.codePointSequence(endIndex = 4) }
        assertFailsWith<IllegalArgumentException> { charArray.codePointSequence(startIndex = 2, endIndex = 1) }
    }

    @Test
    fun indexedIteration() {
        val string = "abc😀def"
        fun <T> testIterator(s: (String) -> T, codePointIterator: T.(Int) -> CodePointIndexedIterator) {
            val s = s(string)
            val iterator = s.codePointIterator(3)
            assertTrue(iterator.hasNext())
            assertTrue(iterator.hasPrevious())
            assertEquals(2, iterator.previousIndex())
            assertEquals(3, iterator.nextIndex())
            assertEquals("😀".codePointAt(0), iterator.next())
            assertEquals(3, iterator.previousIndex())
            assertEquals(5, iterator.nextIndex())
            iterator.advanceByCodePoints(-1)
            assertEquals(3, iterator.nextIndex())
            assertEquals(2, iterator.previousIndex())
            assertEquals('c'.toCodePoint(), iterator.previous())

            assertEquals(string.substring(2).codePointSequence().toList(), iterator.asSequence().toList())
            assertFalse(iterator.hasNext())
            assertFailsWith<NoSuchElementException> { iterator.next() }
            assertEquals(string.length, iterator.nextIndex())

            assertTrue(iterator.hasPrevious())
            val reverseResult = buildList {
                while(iterator.hasPrevious()) add(iterator.previous())
            }
            assertFailsWith<NoSuchElementException> { iterator.previous() }
            assertEquals(-1, iterator.previousIndex())
            assertEquals(string.reversed().codePointSequence().toList(), reverseResult)

            assertFailsWith<IndexOutOfBoundsException> { iterator.advanceByCodePoints(-1) }
            assertFailsWith<IndexOutOfBoundsException> { iterator.advanceByCodePoints(string.codePointCount() + 1) }

            s.codePointIterator(4).let {
                assertEquals("😀".codePointAt(0).lowSurrogate(), it.next().toSingleChar())
            }
            s.codePointIterator(4).let {
                assertEquals("😀".codePointAt(0).highSurrogate(), it.previous().toSingleChar())
            }

            assertFailsWith<IndexOutOfBoundsException> { s.codePointIterator(-1) }
            assertFailsWith<IndexOutOfBoundsException> { s.codePointIterator(string.length + 1) }
        }
        testIterator({ it }, String::codePointIterator)
        testIterator({ StringBuilder(it) }, CharSequence::codePointIterator)
        testIterator({ it.toCharArray() }, CharArray::codePointIterator)

        run {
            val charArray = string.toCharArray()
            val c = "😀".codePointAt(0)
            assertEquals(c, charArray.codePointIterator(charArray.size, startIndex = 0, endIndex = charArray.size).apply { advanceByCodePoints(-4) }.next())
            assertEquals(c, charArray.codePointIterator(charArray.size, startIndex = 0, endIndex = charArray.size).apply { advanceByCodePoints(-3) }.previous())
            assertEquals(c.lowSurrogate(), charArray.codePointIterator(5, startIndex = 4).previous().toSingleChar())
            assertEquals(c.highSurrogate(), charArray.codePointIterator(3, endIndex = 4).next().toSingleChar())

            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointIterator(0, startIndex = 1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointIterator(2, endIndex = 1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointIterator(1, startIndex = -1) }
            assertFailsWith<IndexOutOfBoundsException> { charArray.codePointIterator(1, endIndex = charArray.size + 1) }
            assertFailsWith<IllegalArgumentException> { charArray.codePointIterator(1, startIndex = 1, endIndex = 0) }
        }
    }
}
