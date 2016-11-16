@file:kotlin.jvm.JvmVersion
package test.numbers

import java.math.BigDecimal
import org.junit.Test
import kotlin.test.*

class NumbersJVMTest {

    @Test fun intMinMaxValues() {
        assertEquals(java.lang.Integer.MIN_VALUE, Int.MIN_VALUE)
        assertEquals(java.lang.Integer.MAX_VALUE, Int.MAX_VALUE)
    }

    @Test fun longMinMaxValues() {
        assertEquals(java.lang.Long.MIN_VALUE, Long.MIN_VALUE)
        assertEquals(java.lang.Long.MAX_VALUE, Long.MAX_VALUE)
    }

    @Test fun shortMinMaxValues() {
        assertEquals(java.lang.Short.MIN_VALUE, Short.MIN_VALUE)
        assertEquals(java.lang.Short.MAX_VALUE, Short.MAX_VALUE)
    }

    @Test fun byteMinMaxValues() {
        assertEquals(java.lang.Byte.MIN_VALUE, Byte.MIN_VALUE)
        assertEquals(java.lang.Byte.MAX_VALUE, Byte.MAX_VALUE)
    }

    @Test fun doubleMinMaxValues() {
        assertEquals(java.lang.Double.MIN_VALUE, Double.MIN_VALUE)
        assertEquals(java.lang.Double.MAX_VALUE, Double.MAX_VALUE)
        assertEquals(java.lang.Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        assertEquals(java.lang.Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
    }

    @Test fun floatMinMaxValues() {
        assertEquals(java.lang.Float.MIN_VALUE, Float.MIN_VALUE)
        assertEquals(java.lang.Float.MAX_VALUE, Float.MAX_VALUE)
        assertEquals(java.lang.Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertEquals(java.lang.Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    }

    @Test fun bigDecimalDivRounding() {
        val (d1, d2, d3, d4, d5) = (1..5).map { BigDecimal(it.toString()) }
        val d7 = BigDecimal("7")

        assertEquals(d1, d2 / d3)
        assertEquals(d2, d3 / d2)
        assertEquals(d2, d5 / d2)
        assertEquals(d4, d7 / d2)
        assertEquals(d1, d7 / d5)
    }
}