package numbers

import kotlin.test.*
import kotlin.comparisons.*

val Double.Companion.values get() = listOf(0.0, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)
val Float.Companion.values get() = listOf(0.0f, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)

class NaNPropagationTest {

    private fun propagateOf2(f2d: (Double, Double) -> Double,
                             f2f: (Float, Float) -> Float,
                             function: String) {
        with(Double) {
            for (d in values) {
                assertTrue(f2d(d, NaN).isNaN(), "$function($d, NaN)")
                assertTrue(f2d(NaN, d).isNaN(), "$function(NaN, $d)")
            }
        }
        with(Float) {
            for (f in values) {
                assertTrue(f2f(f, NaN).isNaN(), "$function($f, NaN)")
                assertTrue(f2f(NaN, f).isNaN(), "$function(NaN, $f)")
            }
        }
    }

    private fun propagateOf3(f3d: (Double, Double, Double) -> Double,
                             f3f: (Float, Float, Float) -> Float, function: String) {

        with(Double) {
            for (d in values) {
                assertTrue(f3d(NaN, d, POSITIVE_INFINITY).isNaN(), "$function(NaN, $d, +inf)")
                assertTrue(f3d(d, NaN, POSITIVE_INFINITY).isNaN(), "$function($d, NaN, +inf)")
                assertTrue(f3d(d, POSITIVE_INFINITY, NaN).isNaN(), "$function($d, +inf, NaN)")
            }
        }
        with(Float) {
            for (f in values) {
                assertTrue(f3f(NaN, f, POSITIVE_INFINITY).isNaN(), "$function(NaN, $f, +inf)")
                assertTrue(f3f(f, NaN, POSITIVE_INFINITY).isNaN(), "$function($f, NaN, +inf)")
                assertTrue(f3f(f, POSITIVE_INFINITY, NaN).isNaN(), "$function($f, +inf, NaN)")
            }
        }
    }

    @Test fun minOf() {
        propagateOf2(::minOf, ::minOf, "minOf")
        propagateOf3(::minOf, ::minOf, "minOf")
    }

    @Test fun maxOf() {
        propagateOf2(::maxOf, ::maxOf, "maxOf")
        propagateOf3(::maxOf, ::maxOf, "maxOf")
    }

    @Test fun arrayMin() {
        propagateOf2({ a, b -> arrayOf(a, b).min()!! },
                     { a, b -> arrayOf(a, b).min()!! },
                     "arrayOf().min()")
    }

    @Test fun arrayMax() {
        propagateOf2({ a, b -> arrayOf(a, b).max()!! },
                     { a, b -> arrayOf(a, b).max()!! },
                     "arrayOf().max()")
    }

    @Test fun primitiveArrayMin() {
        propagateOf2({ a, b -> doubleArrayOf(a, b).min()!! },
                     { a, b -> floatArrayOf(a, b).min()!! },
                     "primitiveArrayOf().min()")
    }

    @Test fun primitiveArrayMax() {
        propagateOf2({ a, b -> doubleArrayOf(a, b).max()!! },
                     { a, b -> floatArrayOf(a, b).max()!! },
                     "primitiveArrayOf().max()")
    }

    @Test fun listMin() {
        propagateOf2({ a, b -> listOf(a, b).min()!! },
                     { a, b -> listOf(a, b).min()!! },
                     "listOf().min()")
    }

    @Test fun listMax() {
        propagateOf2({ a, b -> listOf(a, b).max()!! },
                     { a, b -> listOf(a, b).max()!! },
                     "listOf().max()")
    }

    @Test fun sequenceMin() {
        propagateOf2({ a, b -> sequenceOf(a, b).min()!! },
                     { a, b -> sequenceOf(a, b).min()!! },
                     "sequenceOf().min()")
    }

    @Test fun sequenceMax() {
        propagateOf2({ a, b -> sequenceOf(a, b).max()!! },
                     { a, b -> sequenceOf(a, b).max()!! },
                     "sequenceOf().max()")
    }

}

class NaNTotalOrderTest {

    private fun <T : Comparable<T>> totalOrderMinOf2(f2t: (T, T) -> T, function: String) {
        @Suppress("UNCHECKED_CAST")
        with (Double) {
            assertEquals<Any>(0.0, f2t(0.0 as T, NaN as T), "$function(0, NaN)")
            assertEquals<Any>(0.0, f2t(NaN as T, 0.0 as T), "$function(NaN, 0)")
        }
    }

    private fun <T : Comparable<T>> totalOrderMaxOf2(f2t: (T, T) -> T, function: String) {
        @Suppress("UNCHECKED_CAST")
        with (Double) {
            assertTrue((f2t(0.0 as T, NaN as T) as Double).isNaN(), "$function(0, NaN)")
            assertTrue((f2t(NaN as T, 0.0 as T) as Double).isNaN(), "$function(NaN, 0)")
        }
    }

    @Test fun minOfT() {
        totalOrderMinOf2<Comparable<Any>>(::minOf, "minOf")
    }
    @Test fun maxOfT() {
        totalOrderMaxOf2<Comparable<Any>>(::maxOf, "maxOf")
    }

    @Test fun arrayTMin() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> arrayOf(a, b).min()!! }, "arrayOf().min()")
    }
    @Test fun arrayTMax() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> arrayOf(a, b).max()!! }, "arrayOf().max()")
    }
    
    
    @Test fun listTMin() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> listOf(a, b).min()!! }, "listOf().min()")
    }
    @Test fun listTMax() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> listOf(a, b).max()!! }, "listOf().max()")
    }


    @Test fun sequenceTMin() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> sequenceOf(a, b).min()!! }, "sequenceOf().min()")
    }
    @Test fun sequenceTMax() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> sequenceOf(a, b).max()!! }, "sequenceOf().max()")
    }
}