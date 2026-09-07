/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.text.unicode.*
import kotlin.test.*

@OptIn(ExperimentalCodePointApi::class)
public class CodePointRangeTest {
    @Test
    fun codePointRange() {
        val range = 'a'.toCodePoint()..0x1F600.toCodePoint()
        assertFalse(range.isEmpty())

        assertEquals('a'.toCodePoint(), range.first)
        assertEquals(0x1F600.toCodePoint(), range.last)
        assertEquals(0x1F600.toCodePoint(), range.endInclusive)
        @Suppress("DEPRECATION")
        assertEquals(0x1F601.toCodePoint(), range.endExclusive)

        assertTrue('a'.toCodePoint() in range)
        assertTrue('z'.toCodePoint() in range)
        assertTrue(0x1F600.toCodePoint() in range)
        assertFalse('0'.toCodePoint() in range)
        assertFalse(0x1F601.toCodePoint() in range)

        assertTrue('a'.toCodePoint() in (range as ClosedRange<CodePoint>))
        assertFalse((range as ClosedRange<*>).isEmpty())

        assertTrue('a' in range)
        assertFalse('0' in range)

        assertFalse(null in range)
        assertTrue('a'.toCodePoint() as CodePoint? in range)
        assertFalse(0x1F601.toCodePoint() as CodePoint? in range)

        val closedRange = Char.MIN_LOW_SURROGATE.toCodePoint()..Char.MAX_LOW_SURROGATE.toCodePoint()
        val openRange = Char.MIN_LOW_SURROGATE.toCodePoint()..<(Char.MAX_LOW_SURROGATE.toCodePoint() + 1)
        assertTrue(Char.MAX_LOW_SURROGATE in closedRange)
        assertTrue(Char.MAX_LOW_SURROGATE in openRange)
        assertFalse(Char.MAX_LOW_SURROGATE + 1 in openRange)
        assertEquals(closedRange, openRange)

        assertTrue(('a'.toCodePoint() until CodePoint.MIN_VALUE).isEmpty())
    }


    @Test
    @Suppress("DEPRECATION")
    fun openRangeEndExclusive() {
        assertNotEquals(CodePoint.MIN_VALUE, ('a'.toCodePoint()..<CodePoint.MIN_VALUE).endExclusive)
    }

    @Test
    @Suppress("DEPRECATION")
    fun openRangeEndExclusiveThrows() {
        assertFailsWith<IllegalStateException> { ('a'.toCodePoint()..CodePoint.MAX_VALUE).endExclusive }
    }

    @Suppress("EmptyRange")
    @Test
    fun isEmpty() {
        assertTrue(('a'.toCodePoint() downTo 'b'.toCodePoint()).isEmpty())
        assertFalse(('b'.toCodePoint() downTo 'a'.toCodePoint()).isEmpty())
    }


    private fun <T> assertAllEqual(vararg elements: T) {
        for (i in 0..<elements.size) {
            assertEquals(elements[i], elements[i])
            for (j in i + 1..<elements.size) {
                assertEquals(elements[i], elements[j])
                assertEquals(elements[j], elements[i])
                assertEquals(elements[i].hashCode(), elements[j].hashCode())
            }
        }
    }

    @Suppress("EmptyRange")
    @Test fun emptyEqualsHashCode() {
        assertAllEqual(
            CodePointRange.EMPTY,
            'b'.toCodePoint()..'a'.toCodePoint(),
            '0'.toCodePoint()..<'0'.toCodePoint()
        )
        assertAllEqual(
            'b'.toCodePoint()..'a'.toCodePoint() step 1,
            'b'.toCodePoint()..'a'.toCodePoint() step 5,
            'b'.toCodePoint() downTo 'z'.toCodePoint() step 2,
        )
        assertNotEquals('a'.toCodePoint()..'z'.toCodePoint(), CodePointRange.EMPTY)
    }


    @Test
    fun nonEmptyEquals() {
        assertAllEqual('a'.toCodePoint()..'d'.toCodePoint(), 'a'.toCodePoint()..<'e'.toCodePoint()) //, 'a'.toCodePoint()..'d'.toCodePoint() step 1)
        assertAllEqual('d'.toCodePoint() downTo 'a'.toCodePoint(), 'd'.toCodePoint() downTo 'a'.toCodePoint() step 1)
    }

    @Test
    fun nonEmptyRangeHashCode() {
        fun <N : Comparable<N>, R> checkIterableRangeHashCode(range: R) where R : ClosedRange<N>, R : Iterable<N> {
            assertEquals(31 * range.start.hashCode() + range.endInclusive.hashCode(), range.hashCode())
        }
        checkIterableRangeHashCode('a'.toCodePoint()..'z'.toCodePoint())
    }

    @Test
    fun nonEmptyProgressionHashCode() {
        fun checkHashCode(progression: Any, first: Any, last: Any, step: Any) {
            assertEquals(31 * 31 * first.hashCode() + 31 * last.hashCode() + step.hashCode(), progression.hashCode())
        }
        ('a'.toCodePoint()..0x1F600.toCodePoint() step 1).let { checkHashCode(it, it.first, it.last, it.step) }
        ('a'.toCodePoint()..0x1F600.toCodePoint() step 2).let { checkHashCode(it, it.first, it.last, it.step) }
        (0x1F600.toCodePoint() downTo 'a'.toCodePoint()).let { checkHashCode(it, it.first, it.last, it.step) }
    }

    @IgnorableReturnValue
    private fun <T> assertFailsWithIllegalArgument(f: () -> T) = assertFailsWith<IllegalArgumentException> { f() }

    @Test
    fun illegalProgressionCreation() {
        // create Progression explicitly with increment = 0
        assertFailsWithIllegalArgument { CodePointProgression.fromClosedRange('a'.toCodePoint(), 'b'.toCodePoint(), 0) }

        assertFailsWithIllegalArgument { 'a'.toCodePoint()..'b'.toCodePoint() step 0 }
        assertFailsWithIllegalArgument { 'b'.toCodePoint() downTo 'a'.toCodePoint() step 0 }

        assertFailsWithIllegalArgument { 'a'.toCodePoint()..'b'.toCodePoint() step -2 }
        assertFailsWithIllegalArgument { 'b'.toCodePoint() downTo 'a'.toCodePoint() step -2 }
    }

    @Test
    fun stepSizeIsTooLow() {
        assertFailsWithIllegalArgument { CodePointProgression.fromClosedRange('a'.toCodePoint(), 'b'.toCodePoint(), Int.MIN_VALUE) }
    }

    @Test
    fun randomInEmptyRange() {
        assertFailsWith<NoSuchElementException> { CodePointRange.EMPTY.random() }
        assertNull(CodePointRange.EMPTY.randomOrNull())
    }

    @Test fun firstInEmptyRange() {
        assertFailsWith<NoSuchElementException> { CodePointRange.EMPTY.first() }
        assertNull(CodePointRange.EMPTY.firstOrNull())
    }

    @Test fun lastInEmptyRange() {
        assertFailsWith<NoSuchElementException> { CodePointRange.EMPTY.last() }
        assertFailsWith<NoSuchElementException> { CodePointProgression.fromClosedRange('a'.toCodePoint(), 'd'.toCodePoint(), -2).last() }
        assertNull(CodePointRange.EMPTY.lastOrNull())
        assertNull(CodePointProgression.fromClosedRange('a'.toCodePoint(), 'd'.toCodePoint(), -2).lastOrNull())
    }
}
