package numbers

import org.junit.Test as test
import kotlin.test.*

class NumbersJVMTest {

    @test fun intMinMaxValues() {
        assertEquals(java.lang.Integer.MIN_VALUE, Int.MIN_VALUE)
        assertEquals(java.lang.Integer.MAX_VALUE, Int.MAX_VALUE)
    }

    @test fun longMinMaxValues() {
        assertEquals(java.lang.Long.MIN_VALUE, Long.MIN_VALUE)
        assertEquals(java.lang.Long.MAX_VALUE, Long.MAX_VALUE)
    }

    @test fun shortMinMaxValues() {
        assertEquals(java.lang.Short.MIN_VALUE, Short.MIN_VALUE)
        assertEquals(java.lang.Short.MAX_VALUE, Short.MAX_VALUE)
    }

    @test fun byteMinMaxValues() {
        assertEquals(java.lang.Byte.MIN_VALUE, Byte.MIN_VALUE)
        assertEquals(java.lang.Byte.MAX_VALUE, Byte.MAX_VALUE)
    }

    @test fun doubleMinMaxValues() {
        assertEquals(java.lang.Double.MIN_VALUE, Double.MIN_VALUE)
        assertEquals(java.lang.Double.MAX_VALUE, Double.MAX_VALUE)
        assertEquals(java.lang.Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        assertEquals(java.lang.Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
    }

    @test fun floatMinMaxValues() {
        assertEquals(java.lang.Float.MIN_VALUE, Float.MIN_VALUE)
        assertEquals(java.lang.Float.MAX_VALUE, Float.MAX_VALUE)
        assertEquals(java.lang.Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertEquals(java.lang.Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    }
}