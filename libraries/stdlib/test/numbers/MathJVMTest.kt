@file:kotlin.jvm.JvmVersion
package test.numbers

import kotlin.test.*
import org.junit.Test
import kotlin.math.*

class MathJVMTest {

    @Test fun IEEEremainder() {
        val data = arrayOf(  //  a    a IEEErem 2.5
                doubleArrayOf(-2.0,   0.5),
                doubleArrayOf(-1.25, -1.25),
                doubleArrayOf( 0.0,   0.0),
                doubleArrayOf( 1.0,   1.0),
                doubleArrayOf( 1.25,  1.25),
                doubleArrayOf( 1.5,  -1.0),
                doubleArrayOf( 2.0,  -0.5),
                doubleArrayOf( 2.5,   0.0),
                doubleArrayOf( 3.5,   1.0),
                doubleArrayOf( 3.75, -1.25),
                doubleArrayOf( 4.0,  -1.0)
        )
        for ((a, r) in data) {
            assertEquals(r, a.IEEErem(2.5), "($a).IEEErem(2.5)")
        }

        assertTrue(Double.NaN.IEEErem(2.5).isNaN())
        assertTrue(2.0.IEEErem(Double.NaN).isNaN())
        assertTrue(Double.POSITIVE_INFINITY.IEEErem(2.0).isNaN())
        assertTrue(2.0.IEEErem(0.0).isNaN())
        assertEquals(PI, PI.IEEErem(Double.NEGATIVE_INFINITY))
    }

    @Test fun nextAndPrev() {
        for (value in listOf(0.0, -0.0, Double.MIN_VALUE, -1.0, 2.0.pow(10))) {
            val next = value.nextUp()
            if (next > 0) {
                assertEquals(next, value + value.ulp)
            } else {
                assertEquals(value, next - next.ulp)
            }

            val prev = value.nextDown()
            if (prev > 0) {
                assertEquals(value, prev + prev.ulp)
            }
            else {
                assertEquals(prev, value - value.ulp)
            }

            val toZero = value.nextTowards(0.0)
            if (toZero != 0.0) {
                assertEquals(value, toZero + toZero.ulp.withSign(toZero))
            }

            assertEquals(Double.POSITIVE_INFINITY, Double.MAX_VALUE.nextUp())
            assertEquals(Double.MAX_VALUE, Double.POSITIVE_INFINITY.nextDown())

            assertEquals(Double.NEGATIVE_INFINITY, (-Double.MAX_VALUE).nextDown())
            assertEquals((-Double.MAX_VALUE), Double.NEGATIVE_INFINITY.nextUp())

            assertTrue(Double.NaN.ulp.isNaN())
            assertTrue(Double.NaN.nextDown().isNaN())
            assertTrue(Double.NaN.nextUp().isNaN())
            assertTrue(Double.NaN.nextTowards(0.0).isNaN())
        }
    }
}