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

    @Test fun doubleToBits() {
        assertEquals(0x400921fb54442d18L, kotlin.math.PI.toBits())
        assertEquals(kotlin.math.PI, Double.fromBits(0x400921fb54442d18L))

        for (value in listOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.0, -Double.MIN_VALUE, -0.0, 0.0, Double.POSITIVE_INFINITY, Double.MAX_VALUE, 1.0, Double.MIN_VALUE)) {
            assertEquals(value, Double.fromBits(value.toBits()))
            assertEquals(value, Double.fromBits(value.toRawBits()))
        }
        assertTrue(Double.NaN.toBits().let(Double.Companion::fromBits).isNaN())
        assertTrue(Double.NaN.toRawBits().let { Double.fromBits(it) }.isNaN())
    }

    @Test fun floatToBits() {
        val PI_F = kotlin.math.PI.toFloat()
        assertEquals(0x40490fdb, PI_F.toBits())
        assertEquals(PI_F, Float.fromBits(0x40490fdb))

        for (value in listOf(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -1.0F, -Float.MIN_VALUE, -0.0F, 0.0F, Float.POSITIVE_INFINITY, Float.MAX_VALUE, 1.0F, Float.MIN_VALUE)) {
            assertEquals(value, Float.fromBits(value.toBits()))
            assertEquals(value, Float.fromBits(value.toRawBits()))
        }
        assertTrue(Float.NaN.toBits().let(Float.Companion::fromBits).isNaN())
        assertTrue(Float.NaN.toRawBits().let { Float.fromBits(it) }.isNaN())
    }

}