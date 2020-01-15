/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.ranges

import kotlin.test.*

public class RangeTest {
    @Test fun intRange() {
        val range = -5..9
        assertFalse(-1000 in range)
        assertFalse(-6 in range)

        assertTrue(-5 in range)
        assertTrue(-4 in range)
        assertTrue(0 in range)
        assertTrue(3 in range)
        assertTrue(8 in range)
        assertTrue(9 in range)

        assertFalse(10 in range)
        assertFalse(9000 in range)

        assertFalse(range.isEmpty())

        assertTrue(9 in (range as ClosedRange<Int>))
        assertFalse((range as ClosedRange<Int>).isEmpty())

        assertTrue(1.toShort() in range)
        assertTrue(1.toByte() in range)
        assertTrue(1.toLong() in range)
        run @Suppress("DEPRECATION") {
            assertTrue(1.toFloat() in range)
            assertTrue(1.toDouble() in range)
        }

        assertFalse(Long.MAX_VALUE in range)

        assertFalse(null in range)
        assertTrue(1 as Int? in range)
        assertFalse(10 as Int? in range)

        val openRange = 1 until 10
        assertTrue(9 in openRange)
        assertFalse(10 in openRange)

        assertTrue((1 until Int.MIN_VALUE).isEmpty())
    }

    @Test fun byteRange() {
        val range = (-5).toByte()..9.toByte()
        assertFalse((-100).toByte() in range)
        assertFalse((-6).toByte() in range)

        assertTrue((-5).toByte() in range)
        assertTrue((-4).toByte() in range)
        assertTrue(0.toByte() in range)
        assertTrue(3.toByte() in range)
        assertTrue(8.toByte() in range)
        assertTrue(9.toByte() in range)

        assertFalse(10.toByte() in range)
        assertFalse(111.toByte() in range)

        assertFalse(range.isEmpty())


        assertTrue(1.toShort() in range)
        assertTrue(1.toInt() in range)
        assertTrue(1.toLong() in range)
        run @Suppress("DEPRECATION") {
            assertTrue(1.toFloat() in range)
            assertTrue(1.toDouble() in range)
        }

        assertFalse(Long.MAX_VALUE in range)

        // assertTrue(1.toByte() as Byte? in range) // expected not to compile

        val openRange = 1.toByte() until 10.toByte()
        assertTrue(9.toByte() in openRange)
        assertFalse(10.toByte() in openRange)

        // byte arguments now construct IntRange so no overflow here
        assertTrue((0.toByte() until Byte.MIN_VALUE).isEmpty())
        assertTrue((0.toByte() until Int.MIN_VALUE).isEmpty())
    }

    @Test fun shortRange() {
        val range = (-5).toShort()..9.toShort()
        assertFalse((-1000).toShort() in range)
        assertFalse((-6).toShort() in range)

        assertTrue((-5).toShort() in range)
        assertTrue((-4).toShort() in range)
        assertTrue(0.toShort() in range)
        assertTrue(3.toShort() in range)
        assertTrue(8.toShort() in range)
        assertTrue(9.toShort() in range)

        assertFalse(10.toShort() in range)
        assertFalse(239.toShort() in range)

        assertFalse(range.isEmpty())

        assertTrue(1.toByte() in range)
        assertTrue(1.toInt() in range)
        assertTrue(1.toLong() in range)
        run @Suppress("DEPRECATION") {
            assertTrue(1.toFloat() in range)
            assertTrue(1.toDouble() in range)
        }

        assertFalse(Long.MAX_VALUE in range)

        // assertTrue(1.toShort() as Short? in range) // expected not to compile

        val openRange = 1.toShort() until 10.toShort()
        assertTrue(9.toShort() in openRange)
        assertFalse(10.toShort() in openRange)

        assertTrue((0.toShort() until Short.MIN_VALUE).isEmpty())
        assertTrue((0.toShort() until Int.MIN_VALUE).isEmpty())
    }

    @Test fun longRange() {
        val range = -5L..9L
        assertFalse(-10000000L in range)
        assertFalse(-6L in range)

        assertTrue(-5L in range)
        assertTrue(-4L in range)
        assertTrue(0L in range)
        assertTrue(3L in range)
        assertTrue(8L in range)
        assertTrue(9L in range)

        assertFalse(10L in range)
        assertFalse(10000000L in range)

        assertFalse(range.isEmpty())

        assertTrue(9 in (range as ClosedRange<Long>))
        assertFalse((range as ClosedRange<Long>).isEmpty())

        assertTrue(1.toByte() in range)
        assertTrue(1.toShort() in range)
        assertTrue(1.toInt() in range)
        run @Suppress("DEPRECATION") {
            assertTrue(1.toFloat() in range)
            assertTrue(1.toDouble() in range)

            assertFalse(Double.MAX_VALUE in range)
        }

        assertFalse(null in range)
        assertTrue(1L as Long? in range)
        assertFalse(10L as Long? in range)

        val openRange = 1L until 10L
        assertTrue(9L in openRange)
        assertFalse(10L in openRange)

        assertTrue((0 until Long.MIN_VALUE).isEmpty())
        assertTrue((0L until Long.MIN_VALUE).isEmpty())

    }

    @Test fun charRange() {
        val range = 'c'..'w'
        assertFalse('0' in range)
        assertFalse('b' in range)

        assertTrue('c' in range)
        assertTrue('d' in range)
        assertTrue('h' in range)
        assertTrue('m' in range)
        assertTrue('v' in range)
        assertTrue('w' in range)

        assertFalse('z' in range)
        assertFalse('\u1000' in range)

        assertFalse(range.isEmpty())

        assertTrue('v' in (range as ClosedRange<Char>))
        assertFalse((range as ClosedRange<Char>).isEmpty())

        assertFalse(null in range)
        assertTrue('p' as Char? in range)
        assertFalse('z' as Char? in range)

        val openRange = 'A' until 'Z'
        assertTrue('Y' in openRange)
        assertFalse('Z' in openRange)

        assertTrue(('A' until Char.MIN_VALUE).isEmpty())
    }

    @Test fun doubleRange() {
        val range = -1.0..3.14159265358979
        assertFalse(-1e200 in range)
        assertFalse(-100.0 in range)
        assertFalse(-1.00000000001 in range)

        assertTrue(-1.0 in range)
        assertTrue(-0.99999999999 in range)
        assertTrue(0.0 in range)
        assertTrue(1.5 in range)
        assertTrue(3.1415 in range)
        assertTrue(3.14159265358979 in range)

        assertFalse(3.15 in range)
        assertFalse(10.0 in range)
        assertFalse(1e200 in range)

        assertFalse(range.isEmpty())

        run @Suppress("DEPRECATION") {
            assertTrue(1.toByte() in range)
            assertTrue(1.toShort() in range)
            assertTrue(1.toInt() in range)
            assertTrue(1.toLong() in range)
        }
        assertTrue(1.toFloat() in range)

        val zeroRange = 0.0..-0.0
        assertFalse(zeroRange.isEmpty())
        assertTrue(-0.0 in zeroRange)
        assertTrue(-0.0F in zeroRange)
        val normalZeroRange = -0.0..0.0
        assertEquals(zeroRange, normalZeroRange)
        assertEquals(zeroRange.hashCode(), normalZeroRange.hashCode())

        val nanRange = 0.0..Double.NaN
        assertFalse(1.0 in nanRange)
        assertFalse(Double.NaN in nanRange)
        assertFalse(Float.NaN in nanRange)
        assertTrue(nanRange.isEmpty())

        val halfInfRange = 0.0..Double.POSITIVE_INFINITY
        assertTrue(Double.POSITIVE_INFINITY in halfInfRange)
        assertFalse(Double.NEGATIVE_INFINITY in halfInfRange)
        assertFalse(Double.NaN in halfInfRange)
        assertTrue(Float.POSITIVE_INFINITY in halfInfRange)
    }

    @Test fun floatRange() {
        val range = -1.0f..3.14159f
        assertFalse(-1e30f in range)
        assertFalse(-100.0f in range)
        assertFalse(-1.00001f in range)

        assertTrue(-1.0f in range)
        assertTrue(-0.99999f in range)
        assertTrue(0.0f in range)
        assertTrue(1.5f in range)
        assertTrue(3.1415f in range)
        assertTrue(3.14159f in range)

        assertFalse(3.15f in range)
        assertFalse(10.0f in range)
        assertFalse(1e30f in range)

        assertFalse(range.isEmpty())

        run @Suppress("DEPRECATION") {
            assertTrue(1.toByte() in range)
            assertTrue(1.toShort() in range)
            assertTrue(1.toInt() in range)
            assertTrue(1.toLong() in range)
        }
        assertTrue(1.toDouble() in range)

        assertFalse(Double.MAX_VALUE in range)

        val zeroRange = 0.0F..-0.0F
        assertFalse(zeroRange.isEmpty())
        assertTrue(-0.0F in zeroRange)
        val normalZeroRange = -0.0F..0.0F
        assertEquals(zeroRange, normalZeroRange)
        assertEquals(zeroRange.hashCode(), normalZeroRange.hashCode())

        val nanRange = 0.0F..Float.NaN
        assertFalse(1.0F in nanRange)
        assertFalse(Float.NaN in nanRange)
        assertTrue(nanRange.isEmpty())

        val halfInfRange = 0.0F..Float.POSITIVE_INFINITY
        assertTrue(Float.POSITIVE_INFINITY in halfInfRange)
        assertFalse(Float.NEGATIVE_INFINITY in halfInfRange)
        assertFalse(Float.NaN in halfInfRange)
        assertTrue(Double.POSITIVE_INFINITY in halfInfRange)
        assertTrue(Double.MAX_VALUE in halfInfRange)
    }

    @Suppress("EmptyRange")
    @Test fun isEmpty() {
        assertTrue((2..1).isEmpty())
        assertTrue((2L..0L).isEmpty())
        assertTrue((1.toShort()..(-1).toShort()).isEmpty())
        assertTrue((0.toByte()..(-1).toByte()).isEmpty())
        assertTrue((0f..-3.14f).isEmpty())
        assertTrue((-2.72..-3.14).isEmpty())
        assertTrue(('z'..'x').isEmpty())

        assertTrue((1 downTo 2).isEmpty())
        assertTrue((0L downTo 2L).isEmpty())
        assertFalse((2 downTo 1).isEmpty())
        assertFalse((2L downTo 0L).isEmpty())
        assertTrue(('a' downTo 'z').isEmpty())
        assertTrue(('z'..'a' step 2).isEmpty())

        assertTrue(("range".."progression").isEmpty())
    }

    @Suppress("ReplaceAssertBooleanWithAssertEquality", "EmptyRange")
    @Test fun emptyEquals() {
        assertTrue(IntRange.EMPTY == IntRange.EMPTY)
        assertEquals(IntRange.EMPTY, IntRange.EMPTY)
        assertEquals(0L..42L, 0L..42L)
        assertEquals(0L..4200000042000000L, 0L..4200000042000000L)
        assertEquals(3 downTo 0, 3 downTo 0)

        assertEquals(2..1, 1..0)
        assertEquals(2L..1L, 1L..0L)
        assertEquals(2.toShort()..1.toShort(), 1.toShort()..0.toShort())
        assertEquals(2.toByte()..1.toByte(), 1.toByte()..0.toByte())
        assertEquals(0f..-3.14f, 3.14f..0f)
        assertEquals(-2.0..-3.0, 3.0..2.0)
        assertEquals('b'..'a', 'c'..'b')

        assertTrue(1 downTo 2 == 2 downTo 3)
        assertTrue(-1L downTo 0L == -2L downTo -1L)
        assertEquals('j'..'a' step 4, 'u'..'q' step 2)

        assertFalse(0..1 == IntRange.EMPTY)

        assertEquals("range".."progression", "hashcode".."equals")
        assertFalse(("aa".."bb") == ("aaa".."bbb"))
    }

    @Suppress("EmptyRange")
    @Test fun emptyHashCode() {
        assertEquals((0..42).hashCode(), (0..42).hashCode())
        assertEquals((1.23..4.56).hashCode(), (1.23..4.56).hashCode())

        assertEquals((0..-1).hashCode(), IntRange.EMPTY.hashCode())
        assertEquals((2L..1L).hashCode(), (1L..0L).hashCode())
        assertEquals((0.toShort()..(-1).toShort()).hashCode(), (42.toShort()..0.toShort()).hashCode())
        assertEquals((0.toByte()..(-1).toByte()).hashCode(), (42.toByte()..0.toByte()).hashCode())
        assertEquals((0f..-3.14f).hashCode(), (2.39f..1.41f).hashCode())
        assertEquals((0.0..-10.0).hashCode(), (10.0..0.0).hashCode())
        assertEquals(('z'..'x').hashCode(), ('l'..'k').hashCode())

        assertEquals((1 downTo 2).hashCode(), (2 downTo 3).hashCode())
        assertEquals((1L downTo 2L).hashCode(), (2L downTo 3L).hashCode())
        assertEquals(('a' downTo 'b').hashCode(), ('c' downTo 'd').hashCode())

        assertEquals(("range".."progression").hashCode(), ("hashcode".."equals").hashCode())
    }

    @Test fun comparableRange() {
        val range = "island".."isle"
        assertFalse("apple" in range)
        assertFalse("icicle" in range)

        assertTrue("island" in range)
        assertTrue("isle" in range)
        assertTrue("islandic" in range)

        assertFalse("item" in range)
        assertFalse("trail" in range)

        assertFalse(range.isEmpty())
    }

    private fun assertFailsWithIllegalArgument(f: () -> Unit) = assertFailsWith<IllegalArgumentException> { f() }

    @Test fun illegalProgressionCreation() {
        // create Progression explicitly with increment = 0
        assertFailsWithIllegalArgument { IntProgression.fromClosedRange(0, 5, 0) }
        assertFailsWithIllegalArgument { LongProgression.fromClosedRange(0, 5, 0) }
        assertFailsWithIllegalArgument { CharProgression.fromClosedRange('a', 'z', 0) }


        assertFailsWithIllegalArgument { 0..5 step 0 }
        assertFailsWithIllegalArgument { 0.toByte()..5.toByte() step 0 }
        assertFailsWithIllegalArgument { 0.toShort()..5.toShort() step 0 }
        assertFailsWithIllegalArgument { 0L..5L step 0L }
        assertFailsWithIllegalArgument { 'a'..'z' step 0 }

        assertFailsWithIllegalArgument { 0 downTo -5 step 0 }
        assertFailsWithIllegalArgument { 0.toByte() downTo (-5).toByte() step 0 }
        assertFailsWithIllegalArgument { 0.toShort() downTo (-5).toShort() step 0 }
        assertFailsWithIllegalArgument { 0L downTo -5L step 0L }
        assertFailsWithIllegalArgument { 'z' downTo 'a' step 0 }

        assertFailsWithIllegalArgument { 0..5 step -2 }
        assertFailsWithIllegalArgument { 0.toByte()..5.toByte() step -2 }
        assertFailsWithIllegalArgument { 0.toShort()..5.toShort() step -2 }
        assertFailsWithIllegalArgument { 0L..5L step -2L }
        assertFailsWithIllegalArgument { 'a'..'z' step -2 }


        assertFailsWithIllegalArgument { 0 downTo -5 step -2 }
        assertFailsWithIllegalArgument { 0.toByte() downTo (-5).toByte() step -2 }
        assertFailsWithIllegalArgument { 0.toShort() downTo (-5).toShort() step -2 }
        assertFailsWithIllegalArgument { 0L downTo -5L step -2L }
        assertFailsWithIllegalArgument { 'z' downTo 'a' step -2 }
    }

    @Test fun stepSizeIsTooLow() {
        assertFailsWithIllegalArgument { CharProgression.fromClosedRange('a', 'b', Int.MIN_VALUE) }
        assertFailsWithIllegalArgument { IntProgression.fromClosedRange(0, 1, Int.MIN_VALUE) }
        assertFailsWithIllegalArgument { LongProgression.fromClosedRange(0, 1, Long.MIN_VALUE) }
    }

    @Test fun randomInEmptyRange() {
        assertFailsWith<NoSuchElementException> { IntRange.EMPTY.random() }
        assertFailsWith<NoSuchElementException> { LongRange.EMPTY.random() }
        assertFailsWith<NoSuchElementException> { CharRange.EMPTY.random() }
    }

    @Test fun randomOrNullInEmptyRange() {
        assertNull(IntRange.EMPTY.randomOrNull())
        assertNull(LongRange.EMPTY.randomOrNull())
        assertNull(CharRange.EMPTY.randomOrNull())
    }
}
