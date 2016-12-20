package test.numbers

import org.junit.Test
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

}