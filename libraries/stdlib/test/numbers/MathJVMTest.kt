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

}