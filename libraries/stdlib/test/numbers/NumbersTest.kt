package numbers

import org.junit.Test as test
import kotlin.test.*

class NumbersTest {


    @test fun intMinMaxValues() {
        assertTrue(Int.MIN_VALUE < 0)
        assertTrue(Int.MAX_VALUE > 0)

        // overflow behavior
        // doesn't hold for JS Number
        // expect(Int.MIN_VALUE) { Int.MAX_VALUE + 1 }
        // expect(Int.MAX_VALUE) { Int.MIN_VALUE - 1 }
    }

    @test fun longMinMaxValues() {
        assertTrue(Long.MIN_VALUE < 0)
        assertTrue(Long.MAX_VALUE > 0)
        // overflow behavior
        expect(Long.MIN_VALUE) { Long.MAX_VALUE + 1 }
        expect(Long.MAX_VALUE) { Long.MIN_VALUE - 1 }
    }

    @test fun shortMinMaxValues() {
        assertTrue(Short.MIN_VALUE < 0)
        assertTrue(Short.MAX_VALUE > 0)
        // overflow behavior
        expect(Short.MIN_VALUE) { (Short.MAX_VALUE + 1).toShort() }
        expect(Short.MAX_VALUE) { (Short.MIN_VALUE - 1).toShort() }
    }

    @test fun byteMinMaxValues() {
        assertTrue(Byte.MIN_VALUE < 0)
        assertTrue(Byte.MAX_VALUE > 0)
        // overflow behavior
        expect(Byte.MIN_VALUE) { (Byte.MAX_VALUE + 1).toByte() }
        expect(Byte.MAX_VALUE) { (Byte.MIN_VALUE - 1).toByte() }
    }

    @test fun doubleMinMaxValues() {
        assertTrue(Double.MIN_VALUE > 0)
        assertTrue(Double.MAX_VALUE > 0)
        // overflow behavior
        expect(Double.POSITIVE_INFINITY) { Double.MAX_VALUE * 2 }
        expect(Double.NEGATIVE_INFINITY) {-Double.MAX_VALUE * 2 }
        expect(0.0) { Double.MIN_VALUE / 2 }
    }

    @test fun floatMinMaxValues() {
        assertTrue(Float.MIN_VALUE > 0)
        assertTrue(Float.MAX_VALUE > 0)
        // overflow behavior
        expect(Float.POSITIVE_INFINITY) { Float.MAX_VALUE * 2 }
        expect(Float.NEGATIVE_INFINITY) { -Float.MAX_VALUE * 2 }
        expect(0.0F) { Float.MIN_VALUE / 2.0F }
    }
    
    @test fun doubleProperties() {
        for (value in listOf(1.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE))
            doTestNumber(value)
        for (value in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            doTestNumber(value, isInfinite = true)
        doTestNumber(Double.NaN, isNaN = true)
    }

    @test fun floatProperties() {
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