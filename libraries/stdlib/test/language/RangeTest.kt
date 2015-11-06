@file: Suppress("DEPRECATION_ERROR")
package language

import kotlin.test.*
import org.junit.Test as test

public class RangeTest {
    @test fun intRange() {
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

        assertTrue(1.toShort() in range)
        assertTrue(1.toByte() in range)
        assertTrue(1.toLong() in range)
        assertTrue(1.toFloat() in range)
        assertTrue(1.toDouble() in range)

        assertFalse(Long.MAX_VALUE in range)

        val openRange = 1 until 10
        assertTrue(9 in openRange)
        assertFalse(10 in openRange)

        assertTrue(assertFails { 1 until Int.MIN_VALUE } is IllegalArgumentException)
    }

    @test fun byteRange() {
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
        assertTrue(1.toFloat() in range)
        assertTrue(1.toDouble() in range)

        assertFalse(Long.MAX_VALUE in range)

        val openRange = 1.toByte() until 10.toByte()
        assertTrue(9.toByte() in openRange)
        assertFalse(10.toByte() in openRange)

        // byte arguments now construct IntRange so no overflow here
        // assertTrue(assertFails { 0.toByte() until Byte.MIN_VALUE } is IllegalArgumentException)

    }

    @test fun shortRange() {
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
        assertTrue(1.toFloat() in range)
        assertTrue(1.toDouble() in range)

        assertFalse(Long.MAX_VALUE in range)

        val openRange = 1.toShort() until 10.toShort()
        assertTrue(9.toShort() in openRange)
        assertFalse(10.toShort() in openRange)

        // short arguments now construct IntRange so no overflow here
        // assertTrue(assertFails { 0.toShort() until Short.MIN_VALUE } is IllegalArgumentException)
    }

    @test fun longRange() {
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

        assertTrue(1.toByte() in range)
        assertTrue(1.toShort() in range)
        assertTrue(1.toInt() in range)
        assertTrue(1.toFloat() in range)
        assertTrue(1.toDouble() in range)

        assertFalse(Double.MAX_VALUE in range)

        val openRange = 1L until 10L
        assertTrue(9L in openRange)
        assertFalse(10L in openRange)

        assertTrue(assertFails { 0L until Long.MIN_VALUE } is IllegalArgumentException)

    }

    @test fun charRange() {
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

        val openRange = 'A' until 'Z'
        assertTrue('Y' in openRange)
        assertFalse('Z' in openRange)

        assertTrue(assertFails { 'A' until '\u0000' } is IllegalArgumentException)
    }

    @test fun doubleRange() {
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

        assertTrue(1.toByte() in range)
        assertTrue(1.toShort() in range)
        assertTrue(1.toInt() in range)
        assertTrue(1.toLong() in range)
        assertTrue(1.toFloat() in range)
    }

    @test fun floatRange() {
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

        assertTrue(1.toByte() in range)
        assertTrue(1.toShort() in range)
        assertTrue(1.toInt() in range)
        assertTrue(1.toLong() in range)
        assertTrue(1.toDouble() in range)

        assertFalse(Double.MAX_VALUE in range)
    }

    @test fun isEmpty() {
        assertTrue((2..1).isEmpty())
        assertTrue((2L..0L).isEmpty())
        assertTrue((1.toShort()..-1.toShort()).isEmpty())
        assertTrue((0.toByte()..-1.toByte()).isEmpty())
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

    @test fun emptyEquals() {
        assertTrue(IntRange.EMPTY == IntRange.EMPTY)
        assertEquals(IntRange.EMPTY, IntRange.EMPTY)
        assertEquals(0L..42L, 0L..42L)
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

    @test fun emptyHashCode() {
        assertEquals((0..42).hashCode(), (0..42).hashCode())
        assertEquals((1.23..4.56).hashCode(), (1.23..4.56).hashCode())

        assertEquals((0..-1).hashCode(), IntRange.EMPTY.hashCode())
        assertEquals((2L..1L).hashCode(), (1L..0L).hashCode())
        assertEquals((0.toShort()..-1.toShort()).hashCode(), (42.toShort()..0.toShort()).hashCode())
        assertEquals((0.toByte()..-1.toByte()).hashCode(), (42.toByte()..0.toByte()).hashCode())
        assertEquals((0f..-3.14f).hashCode(), (2.39f..1.41f).hashCode())
        assertEquals((0.0..-10.0).hashCode(), (10.0..0.0).hashCode())
        assertEquals(('z'..'x').hashCode(), ('l'..'k').hashCode())

        assertEquals((1 downTo 2).hashCode(), (2 downTo 3).hashCode())
        assertEquals((1L downTo 2L).hashCode(), (2L downTo 3L).hashCode())
        assertEquals(('a' downTo 'b').hashCode(), ('c' downTo 'd').hashCode())

        assertEquals(("range".."progression").hashCode(), ("hashcode".."equals").hashCode())
    }

    @test fun comparableRange() {
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
}