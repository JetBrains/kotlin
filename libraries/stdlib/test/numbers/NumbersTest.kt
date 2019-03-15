/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*

object NumbersTestConstants {
    public const val byteMinSucc: Byte = (Byte.MIN_VALUE + 1).toByte()
    public const val byteMaxPred: Byte = (Byte.MAX_VALUE - 1).toByte()

    public const val shortMinSucc: Short = (Short.MIN_VALUE + 1).toShort()
    public const val shortMaxPred: Short = (Short.MAX_VALUE - 1).toShort()

    public const val intMinSucc: Int = Int.MIN_VALUE + 1
    public const val intMaxPred: Int = Int.MAX_VALUE - 1

    public const val longMinSucc: Long = Long.MIN_VALUE + 1L
    public const val longMaxPred: Long = Long.MAX_VALUE - 1L
}

class NumbersTest {

    var one: Int = 1
    var oneS: Short = 1
    var oneB: Byte = 1

    @Test fun intMinMaxValues() {
        assertTrue(Int.MIN_VALUE < 0)
        assertTrue(Int.MAX_VALUE > 0)

        assertEquals(NumbersTestConstants.intMinSucc, Int.MIN_VALUE + one)
        assertEquals(NumbersTestConstants.intMaxPred, Int.MAX_VALUE - one)

        // overflow behavior
        expect(Int.MIN_VALUE) { Int.MAX_VALUE + one }
        expect(Int.MAX_VALUE) { Int.MIN_VALUE - one }
    }

    @Test fun longMinMaxValues() {
        assertTrue(Long.MIN_VALUE < 0)
        assertTrue(Long.MAX_VALUE > 0)

        assertEquals(NumbersTestConstants.longMinSucc, Long.MIN_VALUE + one)
        assertEquals(NumbersTestConstants.longMaxPred, Long.MAX_VALUE - one)

        // overflow behavior
        expect(Long.MIN_VALUE) { Long.MAX_VALUE + one }
        expect(Long.MAX_VALUE) { Long.MIN_VALUE - one }
    }

    @Test fun shortMinMaxValues() {
        assertTrue(Short.MIN_VALUE < 0)
        assertTrue(Short.MAX_VALUE > 0)

        assertEquals(NumbersTestConstants.shortMinSucc, Short.MIN_VALUE.inc())
        assertEquals(NumbersTestConstants.shortMaxPred, Short.MAX_VALUE.dec())

        // overflow behavior
        expect(Short.MIN_VALUE) { (Short.MAX_VALUE + oneS).toShort() }
        expect(Short.MAX_VALUE) { (Short.MIN_VALUE - oneS).toShort() }
    }

    @Test fun byteMinMaxValues() {
        assertTrue(Byte.MIN_VALUE < 0)
        assertTrue(Byte.MAX_VALUE > 0)

        assertEquals(NumbersTestConstants.byteMinSucc, Byte.MIN_VALUE.inc())
        assertEquals(NumbersTestConstants.byteMaxPred, Byte.MAX_VALUE.dec())

        // overflow behavior
        expect(Byte.MIN_VALUE) { (Byte.MAX_VALUE + oneB).toByte() }
        expect(Byte.MAX_VALUE) { (Byte.MIN_VALUE - oneB).toByte() }
    }

    @Test fun doubleMinMaxValues() {
        assertTrue(Double.MIN_VALUE > 0)
        assertTrue(Double.MAX_VALUE > 0)

        // overflow behavior
        expect(Double.POSITIVE_INFINITY) { Double.MAX_VALUE * 2 }
        expect(Double.NEGATIVE_INFINITY) {-Double.MAX_VALUE * 2 }
        expect(0.0) { Double.MIN_VALUE / 2 }
    }

    @Test fun floatMinMaxValues() {
        assertTrue(Float.MIN_VALUE > 0)
        assertTrue(Float.MAX_VALUE > 0)

        // overflow behavior
        expect(Float.POSITIVE_INFINITY) { Float.MAX_VALUE * 2 }
        expect(Float.NEGATIVE_INFINITY) { -Float.MAX_VALUE * 2 }
        expect(0.0F) { Float.MIN_VALUE / 2.0F }
    }

    @Test fun charMinMaxValues() {
        assertTrue(Char.MIN_VALUE.toInt() == 0)
        assertTrue(Char.MAX_VALUE.toInt() > 0)

        // overflow behavior
        expect(Char.MIN_VALUE) { Char.MAX_VALUE + one }
        expect(Char.MAX_VALUE) { Char.MIN_VALUE - one }
    }
    
    @Test fun doubleProperties() {
        for (value in listOf(1.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE))
            doTestNumber(value)
        for (value in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            doTestNumber(value, isInfinite = true)
        doTestNumber(Double.NaN, isNaN = true)
    }

    @Test fun floatProperties() {
        for (value in listOf(1.0F, 0.0F, Float.MAX_VALUE, Float.MIN_VALUE))
            doTestNumber(value)
        for (value in listOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY))
            doTestNumber(value, isInfinite = true)
        doTestNumber(Float.NaN, isNaN = true)
    }


    private fun doTestNumber(value: Double, isNaN: Boolean = false, isInfinite: Boolean = false) {
        assertEquals(isNaN, value.isNaN(), "Expected $value to have isNaN: $isNaN")
        assertEquals(isInfinite, value.isInfinite(), "Expected $value to have isInfinite: $isInfinite")
        assertEquals(!isNaN && !isInfinite, value.isFinite())
    }

    private fun doTestNumber(value: Float, isNaN: Boolean = false, isInfinite: Boolean = false) {
        assertEquals(isNaN, value.isNaN(), "Expected $value to have isNaN: $isNaN")
        assertEquals(isInfinite, value.isInfinite(), "Expected $value to have isInfinite: $isInfinite")
        assertEquals(!isNaN && !isInfinite, value.isFinite())
    }

    @Test fun doubleToBits() {
        assertEquals(0x400921fb54442d18L, kotlin.math.PI.toBits())
        assertEquals(0x400921fb54442d18L, kotlin.math.PI.toRawBits())
        assertEquals(kotlin.math.PI, Double.fromBits(0x400921fb54442d18L))

        for (value in listOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -Double.MIN_VALUE, -0.0, 0.0, Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1.0, Double.MIN_VALUE)) {
            assertEquals(value, Double.fromBits(value.toBits()))
            assertEquals(value, Double.fromBits(value.toRawBits()))
        }
        assertTrue(Double.NaN.toBits().let(Double.Companion::fromBits).isNaN())
        assertTrue(Double.NaN.toRawBits().let { Double.fromBits(it) }.isNaN())

        assertEquals(0x7FF00000L shl 32, Double.POSITIVE_INFINITY.toBits())
        assertEquals(0xFFF00000L shl 32, Double.NEGATIVE_INFINITY.toBits())

        assertEquals(0x7FF80000_00000000L, Double.NaN.toBits())
        assertEquals(0x7FF80000_00000000L, Double.NaN.toRawBits())

        val bitsNaN = Double.NaN.toBits()
        for (bitsDenormNaN in listOf(0xFFF80000L shl 32, bitsNaN or 1)) {
            assertTrue(Double.fromBits(bitsDenormNaN).isNaN(), "expected $bitsDenormNaN represent NaN")
            assertEquals(bitsNaN, Double.fromBits(bitsDenormNaN).toBits())
        }
    }

    @Test fun floatToBits() {
        val PI_F = kotlin.math.PI.toFloat()
        assertEquals(0x40490fdb, PI_F.toBits())
        assertAlmostEquals(PI_F, Float.fromBits(0x40490fdb)) // PI_F is actually Double in JS
        // -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MIN_VALUE, Float.MIN_VALUE: overflow or underflow
        for (value in listOf(Float.NEGATIVE_INFINITY, -1.0F, -0.0F, 0.0F, Float.POSITIVE_INFINITY, 1.0F)) {
            assertEquals(value, Float.fromBits(value.toBits()))
            assertEquals(value, Float.fromBits(value.toRawBits()))
        }

        assertTrue(Float.NaN.toBits().let(Float.Companion::fromBits).isNaN())
        assertTrue(Float.NaN.toRawBits().let { Float.fromBits(it) }.isNaN())

        assertEquals(0xbf800000.toInt(), (-1.0F).toBits())
        assertEquals(0x7fc00000, Float.NaN.toBits())
        assertEquals(0x7fc00000, Float.NaN.toRawBits())

        val bitsNaN = Float.NaN.toBits()
        for (bitsDenormNaN in listOf(0xFFFC0000.toInt(), bitsNaN or 1)) {
            assertTrue(Float.fromBits(bitsDenormNaN).isNaN(), "expected $bitsDenormNaN represent NaN")
            assertEquals(bitsNaN, Float.fromBits(bitsDenormNaN).toBits())
        }
    }

    @Test fun sizeInBitsAndBytes() {
        fun testSizes(companion: Any, sizeBytes: Int, sizeBits: Int, expectedSizeBytes: Int) {
            assertEquals(expectedSizeBytes, sizeBytes, companion.toString())
            assertEquals(expectedSizeBytes * 8, sizeBits, companion.toString())
        }

        testSizes(Char, Char.SIZE_BYTES, Char.SIZE_BITS, 2)

        testSizes(Byte, Byte.SIZE_BYTES, Byte.SIZE_BITS, 1)
        testSizes(Short, Short.SIZE_BYTES, Short.SIZE_BITS, 2)
        testSizes(Int, Int.SIZE_BYTES, Int.SIZE_BITS, 4)
        testSizes(Long, Long.SIZE_BYTES, Long.SIZE_BITS, 8)

        testSizes(UByte, UByte.SIZE_BYTES, UByte.SIZE_BITS, 1)
        testSizes(UShort, UShort.SIZE_BYTES, UShort.SIZE_BITS, 2)
        testSizes(UInt, UInt.SIZE_BYTES, UInt.SIZE_BITS, 4)
        testSizes(ULong, ULong.SIZE_BYTES, ULong.SIZE_BITS, 8)
   }

}