package numbers

import org.junit.Test as test
import kotlin.test.*

class NumbersTest {

    test fun doubleProperties() {
        for (value in listOf(1.0, 0.0))
            doTestNumber(value)
        for (value in listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            doTestNumber(value, isInfinite = true)
        doTestNumber(Double.NaN, isNaN = true)
    }

    test fun floatProperties() {
        for (value in listOf(1.0F, 0.0F))
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