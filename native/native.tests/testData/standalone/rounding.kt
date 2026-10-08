// DISABLE_NATIVE: targetArchitecture=ARM32
// MODULE: cinterop
// FILE: fenv.def
package = fenv
headers = fenv.h
linkerOpts.linux = -lm

// MODULE: main(cinterop)
// FILE: main.kt
import fenv.*
import kotlin.math.round
import kotlin.test.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main() {
    for (mode in intArrayOf(FE_DOWNWARD, FE_TONEAREST, FE_TOWARDZERO, FE_UPWARD)) {
        assertEquals(0, fesetround(mode), "fesetround failed")

        // Double
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, 1.0, -10.0)) {
            assertEquals(value, round(value))
        }
        val data = arrayOf( //   v  round
                doubleArrayOf( 1.3,  1.0),
                doubleArrayOf(-1.3, -1.0),
                doubleArrayOf( 1.5,  2.0),
                doubleArrayOf(-1.5, -2.0),
                doubleArrayOf( 1.8,  2.0),
                doubleArrayOf(-1.8, -2.0),

                doubleArrayOf( 2.3,  2.0),
                doubleArrayOf(-2.3, -2.0),
                doubleArrayOf( 2.5,  2.0),
                doubleArrayOf(-2.5, -2.0),
                doubleArrayOf( 2.8,  3.0),
                doubleArrayOf(-2.8, -3.0),
        )
        for ([v, r] in data) {
            assertEquals(r, round(v), "round($v)")
        }

        // Float
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, 1.0F, -10.0F)) {
            assertEquals(value, round(value))
        }
        val fdata = arrayOf( //  v   round
                floatArrayOf( 1.3F,  1.0F),
                floatArrayOf(-1.3F, -1.0F),
                floatArrayOf( 1.5F,  2.0F),
                floatArrayOf(-1.5F, -2.0F),
                floatArrayOf( 1.8F,  2.0F),
                floatArrayOf(-1.8F, -2.0F),

                floatArrayOf( 2.3F,  2.0F),
                floatArrayOf(-2.3F, -2.0F),
                floatArrayOf( 2.5F,  2.0F),
                floatArrayOf(-2.5F, -2.0F),
                floatArrayOf( 2.8F,  3.0F),
                floatArrayOf(-2.8F, -3.0F),
        )
        for ([v, r] in fdata) {
            assertEquals(r, round(v), "round($v)")
        }
    }
}
