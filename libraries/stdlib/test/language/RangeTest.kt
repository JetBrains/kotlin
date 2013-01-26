package language

import java.lang.Double as jDouble
import java.lang.Float as jFloat
import kotlin.test.*
import org.junit.Test as test

public class RangeTest {
    test fun intRange() {
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
    }

    test fun byteRange() {
        val range = -5.toByte()..9.toByte()
        assertFalse(-100.toByte() in range)
        assertFalse(-6.toByte() in range)

        assertTrue(-5.toByte() in range)
        assertTrue(-4.toByte() in range)
        assertTrue(0.toByte() in range)
        assertTrue(3.toByte() in range)
        assertTrue(8.toByte() in range)
        assertTrue(9.toByte() in range)

        assertFalse(10.toByte() in range)
        assertFalse(111.toByte() in range)
    }

    test fun shortRange() {
        val range = -5.toShort()..9.toShort()
        assertFalse(-1000.toShort() in range)
        assertFalse(-6.toShort() in range)

        assertTrue(-5.toShort() in range)
        assertTrue(-4.toShort() in range)
        assertTrue(0.toShort() in range)
        assertTrue(3.toShort() in range)
        assertTrue(8.toShort() in range)
        assertTrue(9.toShort() in range)

        assertFalse(10.toShort() in range)
        assertFalse(239.toShort() in range)
    }

    test fun longRange() {
        val range = -5.toLong()..9.toLong()
        assertFalse(-10000000.toLong() in range)
        assertFalse(-6.toLong() in range)

        assertTrue(-5.toLong() in range)
        assertTrue(-4.toLong() in range)
        assertTrue(0.toLong() in range)
        assertTrue(3.toLong() in range)
        assertTrue(8.toLong() in range)
        assertTrue(9.toLong() in range)

        assertFalse(10.toLong() in range)
        assertFalse(10000000.toLong() in range)
    }

    test fun charRange() {
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
    }

    test fun doubleRange() {
        val range = -1.0..3.14159265358979
        assertFalse(jDouble.NEGATIVE_INFINITY in range)
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
        assertFalse(jDouble.POSITIVE_INFINITY in range)

        assertFalse(jDouble.NaN in range)
    }

    test fun floatRange() {
        val range = -1.0.toFloat()..3.14159.toFloat()
        assertFalse(jFloat.NEGATIVE_INFINITY in range)
        assertFalse(-1e30.toFloat() in range)
        assertFalse(-100.0.toFloat() in range)
        assertFalse(-1.00001.toFloat() in range)

        assertTrue(-1.0.toFloat() in range)
        assertTrue(-0.99999.toFloat() in range)
        assertTrue(0.0.toFloat() in range)
        assertTrue(1.5.toFloat() in range)
        assertTrue(3.1415.toFloat() in range)
        assertTrue(3.14159.toFloat() in range)

        assertFalse(3.15.toFloat() in range)
        assertFalse(10.0.toFloat() in range)
        assertFalse(1e30.toFloat() in range)
        assertFalse(jFloat.POSITIVE_INFINITY in range)

        assertFalse(jFloat.NaN in range)
    }

    test fun comparableRange() {
        val range = "island".."isle"
        assertFalse("apple" in range)
        assertFalse("icicle" in range)

        assertTrue("island" in range)
        assertTrue("isle" in range)
        assertTrue("islandic" in range)

        assertFalse("item" in range)
        assertFalse("trail" in range)
    }

    test fun illegalSequenceCreation() {
        // create sequence explicitly with increment = 0
        failsWith<IllegalArgumentException> { IntSequence(0, 5, 0) }
        failsWith<IllegalArgumentException> { ByteSequence(0, 5, 0) }
        failsWith<IllegalArgumentException> { ShortSequence(0, 5, 0) }
        failsWith<IllegalArgumentException> { LongSequence(0, 5, 0) }
        failsWith<IllegalArgumentException> { CharacterSequence('a', 'z', 0) }
        failsWith<IllegalArgumentException> { DoubleSequence(0.0, 5.0, 0.0) }
        failsWith<IllegalArgumentException> { FloatSequence(0.0.toFloat(), 5.0.toFloat(), 0.0.toFloat()) }

        failsWith<IllegalArgumentException> { 0..5 step 0 }
        failsWith<IllegalArgumentException> { 0.toByte()..5.toByte() step 0 }
        failsWith<IllegalArgumentException> { 0.toShort()..5.toShort() step 0  }
        failsWith<IllegalArgumentException> { 0.toLong()..5.toLong() step 0.toLong() }
        failsWith<IllegalArgumentException> { 'a'..'z' step 0 }
        failsWith<IllegalArgumentException> { 0.0..5.0 step 0.0 }
        failsWith<IllegalArgumentException> { 0.0.toFloat()..5.0.toFloat() step 0.0.toFloat() }

        failsWith<IllegalArgumentException> { 0 downTo -5 step 0 }
        failsWith<IllegalArgumentException> { 0.toByte() downTo -5.toByte() step 0 }
        failsWith<IllegalArgumentException> { 0.toShort() downTo -5.toShort() step 0  }
        failsWith<IllegalArgumentException> { 0.toLong() downTo -5.toLong() step 0.toLong() }
        failsWith<IllegalArgumentException> { 'z' downTo 'a' step 0 }
        failsWith<IllegalArgumentException> { 0.0 downTo -5.0 step 0.0 }
        failsWith<IllegalArgumentException> { 0.0.toFloat() downTo -5.0.toFloat() step 0.0.toFloat() }

        failsWith<IllegalArgumentException> { 0..5 step -2 }
        failsWith<IllegalArgumentException> { 0.toByte()..5.toByte() step -2 }
        failsWith<IllegalArgumentException> { 0.toShort()..5.toShort() step -2  }
        failsWith<IllegalArgumentException> { 0.toLong()..5.toLong() step -2.toLong() }
        failsWith<IllegalArgumentException> { 'a'..'z' step -2 }
        failsWith<IllegalArgumentException> { 0.0..5.0 step -0.5 }
        failsWith<IllegalArgumentException> { 0.0.toFloat()..5.0.toFloat() step -0.5.toFloat() }

        failsWith<IllegalArgumentException> { 0 downTo -5 step -2 }
        failsWith<IllegalArgumentException> { 0.toByte() downTo -5.toByte() step -2 }
        failsWith<IllegalArgumentException> { 0.toShort() downTo -5.toShort() step -2  }
        failsWith<IllegalArgumentException> { 0.toLong() downTo -5.toLong() step -2.toLong() }
        failsWith<IllegalArgumentException> { 'z' downTo 'a' step -2 }
        failsWith<IllegalArgumentException> { 0.0 downTo -5.0 step -0.5 }
        failsWith<IllegalArgumentException> { 0.0.toFloat() downTo -5.0.toFloat() step -0.5.toFloat() }
    }
}