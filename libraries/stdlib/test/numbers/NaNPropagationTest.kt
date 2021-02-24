/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package numbers

import kotlin.test.*

val Double.Companion.values get() = listOf(0.0, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)
val Float.Companion.values get() = listOf(0.0f, NEGATIVE_INFINITY, MIN_VALUE, MAX_VALUE, POSITIVE_INFINITY, NaN)

class NaNPropagationTest {

    private fun propagateOf2(
        f2d: (Double, Double) -> Double,
        f2f: (Float, Float) -> Float,
        function: String
    ) {
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

    private fun propagateOf3(
        f3d: (Double, Double, Double) -> Double,
        f3f: (Float, Float, Float) -> Float, function: String
    ) {

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

    @Test
    fun minOf() {
        propagateOf2(::minOf, ::minOf, "minOf")
        propagateOf3(::minOf, ::minOf, "minOf")
    }

    @Test
    fun minOfVararg() {
        propagateOf3(
            { a, b, c -> minOf(a, *doubleArrayOf(b, c)) },
            { a, b, c -> minOf(a, *floatArrayOf(b, c)) },
            "minOf(a, vararg other)"
        )
    }

    @Test
    fun maxOf() {
        propagateOf2(::maxOf, ::maxOf, "maxOf")
        propagateOf3(::maxOf, ::maxOf, "maxOf")
    }

    @Test
    fun maxOfVararg() {
        propagateOf3(
            { a, b, c -> maxOf(a, *doubleArrayOf(b, c)) },
            { a, b, c -> maxOf(a, *floatArrayOf(b, c)) },
            "maxOf(a, vararg other)"
        )
    }

    @Test
    fun arrayMinOrNull() {
        propagateOf2(
            { a, b -> arrayOf(a, b).minOrNull()!! },
            { a, b -> arrayOf(a, b).minOrNull()!! },
            "arrayOf().minOrNull()"
        )
    }

    @Test
    fun arrayMaxOrNull() {
        propagateOf2(
            { a, b -> arrayOf(a, b).maxOrNull()!! },
            { a, b -> arrayOf(a, b).maxOrNull()!! },
            "arrayOf().maxOrNull()"
        )
    }

    @Test
    fun arrayMinOf() {
        propagateOf3(
            { a, b, c -> arrayOf(a, b, c).minOf { it } },
            { a, b, c -> arrayOf(a, b, c).minOf { it } },
            "arrayOf().minOf()"
        )
    }

    @Test
    fun arrayMaxOf() {
        propagateOf3(
            { a, b, c -> arrayOf(a, b, c).maxOf { it } },
            { a, b, c -> arrayOf(a, b, c).maxOf { it } },
            "arrayOf().maxOf()"
        )
    }

    @Test
    fun primitiveArrayMinOrNull() {
        propagateOf2(
            { a, b -> doubleArrayOf(a, b).minOrNull()!! },
            { a, b -> floatArrayOf(a, b).minOrNull()!! },
            "primitiveArrayOf().minOrNull()"
        )
    }

    @Test
    fun primitiveArrayMax() {
        propagateOf2(
            { a, b -> doubleArrayOf(a, b).minOrNull()!! },
            { a, b -> floatArrayOf(a, b).minOrNull()!! },
            "primitiveArrayOf().maxOrNull()"
        )
    }

    @Test
    fun primitiveArrayMinOf() {
        propagateOf3(
            { a, b, c -> doubleArrayOf(a, b, c).minOf { it } },
            { a, b, c -> floatArrayOf(a, b, c).minOf { it } },
            "primitiveArrayOf().minOf()"
        )
        propagateOf3(
            { a, b, c -> val arr = doubleArrayOf(a, b, c); intArrayOf(0, 1, 2).minOf { arr[it] } },
            { a, b, c -> val arr = floatArrayOf(a, b, c); intArrayOf(0, 1, 2).minOf { arr[it] } },
            "intArrayOf().minOf()"
        )
    }

    @Test
    fun primitiveArrayMaxOf() {
        propagateOf3(
            { a, b, c -> doubleArrayOf(a, b, c).maxOf { it } },
            { a, b, c -> floatArrayOf(a, b, c).maxOf { it } },
            "primitiveArrayOf().maxOf()"
        )
        propagateOf3(
            { a, b, c -> val arr = doubleArrayOf(a, b, c); intArrayOf(0, 1, 2).maxOf { arr[it] } },
            { a, b, c -> val arr = floatArrayOf(a, b, c); intArrayOf(0, 1, 2).maxOf { arr[it] } },
            "intArrayOf().maxOf()"
        )
    }

    @Test
    fun listMinOrNull() {
        propagateOf2(
            { a, b -> listOf(a, b).minOrNull()!! },
            { a, b -> listOf(a, b).minOrNull()!! },
            "listOf().minOrNull()"
        )
    }

    @Test
    fun listMaxOrNull() {
        propagateOf2(
            { a, b -> listOf(a, b).maxOrNull()!! },
            { a, b -> listOf(a, b).maxOrNull()!! },
            "listOf().maxOrNull()"
        )
    }

    @Test
    fun listMinOf() {
        propagateOf3(
            { a, b, c -> listOf(a, b, c).minOf { it } },
            { a, b, c -> listOf(a, b, c).minOf { it } },
            "listOf().minOf()"
        )
    }

    @Test
    fun listMaxOf() {
        propagateOf3(
            { a, b, c -> listOf(a, b, c).maxOf { it } },
            { a, b, c -> listOf(a, b, c).maxOf { it } },
            "listOf().maxOf()"
        )
    }

    @Test
    fun sequenceMinOrNull() {
        propagateOf2(
            { a, b -> sequenceOf(a, b).minOrNull()!! },
            { a, b -> sequenceOf(a, b).minOrNull()!! },
            "sequenceOf().minOrNull()"
        )
    }

    @Test
    fun sequenceMaxOrNull() {
        propagateOf2(
            { a, b -> sequenceOf(a, b).maxOrNull()!! },
            { a, b -> sequenceOf(a, b).maxOrNull()!! },
            "sequenceOf().maxOrNull()"
        )
    }

    @Test
    fun sequenceMinOf() {
        propagateOf3(
            { a, b, c -> sequenceOf(a, b, c).minOf { it } },
            { a, b, c -> sequenceOf(a, b, c).minOf { it } },
            "sequenceOf().minOf()"
        )
    }

    @Test
    fun sequenceMaxOf() {
        propagateOf3(
            { a, b, c -> sequenceOf(a, b, c).maxOf { it } },
            { a, b, c -> sequenceOf(a, b, c).maxOf { it } },
            "sequenceOf().maxOf()"
        )
    }
}

class NaNTotalOrderTest {

    private fun <T : Comparable<T>> totalOrderMinOf2(f2t: (T, T) -> T, function: String) {
        @Suppress("UNCHECKED_CAST")
        with(Double) {
            assertEquals<Any>(0.0, f2t(0.0 as T, NaN as T), "$function(0, NaN)")
            assertEquals<Any>(0.0, f2t(NaN as T, 0.0 as T), "$function(NaN, 0)")
        }
    }

    private fun <T : Comparable<T>> totalOrderMaxOf2(f2t: (T, T) -> T, function: String) {
        @Suppress("UNCHECKED_CAST")
        with(Double) {
            assertTrue((f2t(0.0 as T, NaN as T) as Double).isNaN(), "$function(0, NaN)")
            assertTrue((f2t(NaN as T, 0.0 as T) as Double).isNaN(), "$function(NaN, 0)")
        }
    }

    @Test
    fun minOfT() {
        totalOrderMinOf2<Comparable<Any>>(::minOf, "minOf")
    }

    @Test
    fun maxOfT() {
        totalOrderMaxOf2<Comparable<Any>>(::maxOf, "maxOf")
    }

    @Test
    fun arrayTMinOrNull() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> arrayOf(a, b).minOrNull()!! }, "arrayOf().minOrNull()")
    }

    @Test
    fun arrayTMaxOrNull() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> arrayOf(a, b).maxOrNull()!! }, "arrayOf().maxOrNull()")
    }


    @Test
    fun listTMinOrNull() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> listOf(a, b).minOrNull()!! }, "listOf().minOrNull()")
    }

    @Test
    fun listTMaxOrNull() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> listOf(a, b).maxOrNull()!! }, "listOf().maxOrNull()")
    }


    @Test
    fun sequenceTMinOrNull() {
        totalOrderMinOf2<Comparable<Any>>({ a, b -> sequenceOf(a, b).minOrNull()!! }, "sequenceOf().minOrNull()")
    }

    @Test
    fun sequenceTMaxOrNull() {
        totalOrderMaxOf2<Comparable<Any>>({ a, b -> sequenceOf(a, b).maxOrNull()!! }, "sequenceOf().maxOrNull()")
    }
}
