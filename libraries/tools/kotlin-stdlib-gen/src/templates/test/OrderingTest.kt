/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object OrderingTest : TestTemplateGroupBase() {

    private val TestBuilder.elementConversion: String
        get() = when (primitive) {
            null -> "it.toString()"
            PrimitiveType.Char -> "'a' + it"
            PrimitiveType.Boolean -> "it % 2 == 0"
            else -> "it.to${test.P}()"
        }

    private val TestBuilder.arrayConversion: String
        get() = when (family) {
            ArraysOfPrimitives, ArraysOfUnsigned -> "to${test.P}Array()"
            else -> "toTypedArray()"
        }

    val f_reverse = test("reverse()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        body {
            """
            val arrays = (0..4).map { n -> (1..n).map { $elementConversion }.$arrayConversion }
            for (array in arrays) {
                val original = array.toList()
                array.reverse()
                val reversed = array.toList()
                assertEquals(original.asReversed(), reversed)
            }
            """
        }
    }

    val f_reverse_range = test("reverseRange()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        body {
            """
            val arrays = (0..7).map { n -> n to (0 until n).map { $elementConversion }.$arrayConversion }
            for ((size, array) in arrays) {
                for (fromIndex in 0 until size) {
                    for (toIndex in fromIndex..size) {
                        val original = array.toMutableList()
                        array.reverse(fromIndex, toIndex)
                        val reversed = array.toMutableList()
                        assertEquals(original.apply { subList(fromIndex, toIndex).reverse() }, reversed)
                    }
                }

                assertFailsWith<IndexOutOfBoundsException> { array.reverse(-1, size) }
                assertFailsWith<IndexOutOfBoundsException> { array.reverse(0, size + 1) }
                assertFailsWith<IllegalArgumentException> { array.reverse(0, -1) }
            }
            """
        }
    }

    val f_reversed = listOf("reversed", "reversedArray").map { op ->
        val isReversedArray = op == "reversedArray"
        test("$op()") {
            include(Iterables, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
            filter { family, _ -> (isReversedArray && !family.isArray()).not() }
        } builder {
            val resultF = if (isReversedArray) f else Lists
            body {
                "${resultF.assertEquals}(${resultF.of(3, 2, 1)}, ${of(1, 2, 3)}.$op())"
            }
            bodyAppend(Iterables, ArraysOfObjects) {
                """${resultF.assertEquals}(${resultF.of}("3", "2", "1"), $of("1", "2", "3").$op())"""
            }
            body(PrimitiveType.Char) {
                "${resultF.assertEquals}(${resultF.of}('3', '2', '1'), charArrayOf('1', '2', '3').$op())"
            }
            body(PrimitiveType.Boolean) {
                "${resultF.assertEquals}(${resultF.of}(false, false, true), booleanArrayOf(true, false, false).$op())"
            }
        }
    }

    val f_sorted = listOf("sorted", "sortedArray", "sortedDescending", "sortedArrayDescending").map { op ->
        val isSortedArray = op.startsWith("sortedArray")
        test("$op()") {
            includeDefault()
            include(ArraysOfUnsigned)
            exclude(PrimitiveType.Boolean)
            filter { family, _ -> (isSortedArray && !family.isArray()).not() }
        } builder {
            val cmp = if (op.endsWith("Descending")) ">=" else "<="
            val sortAndCheck = "$op().iterator().assertSorted { a, b -> a.compareTo(b) $cmp 0 }"
            body {
                """
                ${of(3, 7, 1)}.$sortAndCheck
                $of(ONE, MAX_VALUE, MIN_VALUE).$sortAndCheck
                """
            }
            bodyAppend(Iterables, Sequences, ArraysOfObjects) {
                """
                $of("ac", "aD", "aba").$sortAndCheck
                """
            }
            bodyAppend(PrimitiveType.Float, PrimitiveType.Double) {
                """
                $of(POSITIVE_INFINITY, NEGATIVE_INFINITY, MAX_VALUE, -MAX_VALUE, ONE, -ONE, 
                                MIN_VALUE, -MIN_VALUE, ZERO, -ZERO, $P.NaN).$sortAndCheck
                """
            }
            body(PrimitiveType.Char) {
                """
                $of('a', 'D', 'c').$sortAndCheck
                """
            }
        }
    }

    val f_sort = listOf("sort", "sortDescending").map { op ->
        val isDescending = op.endsWith("Descending")
        test("$op()") {
            include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
            exclude(PrimitiveType.Boolean)
        } builder {
            val reversed = (if (f.isArray()) ".reversedArray()" else ".reversed()").takeIf { isDescending } ?: ""
            body {
                val (five, nine, eighty) = listOf(5, 9, 80).map { literal(it) }
                """
                val data = $mutableOf($five, TWO, ONE, $nine, $eighty, MIN_VALUE, MAX_VALUE)
                data.$op()
                $assertEquals($of(MIN_VALUE, ONE, TWO, $five, $nine, $eighty, MAX_VALUE)$reversed, data)
                """
            }
            bodyAppend(Lists, ArraysOfObjects) {
                """
                val strings = $mutableOf("9", "80", "all", "Foo")
                strings.$op()
                $assertEquals($of("80", "9", "Foo", "all")$reversed, strings) 
                """
            }
            body(PrimitiveType.Char) {
                """
                val data = charArrayOf('d', 'c', 'E', 'a', '\u0000', '\uFFFF')
                data.$op()
                $assertEquals($of('\u0000', 'E', 'a', 'c', 'd', '\uFFFF')$reversed, data)
                """
            }
        }
    }

    val f_sortedWith = listOf("sortedWith", "sortedArrayWith").map { op ->
        val isSortedArray = op.contains("Array")
        test("$op()") {
            includeDefault()
            filter { family, _ -> !isSortedArray || family == ArraysOfObjects }
            exclude(PrimitiveType.Char, PrimitiveType.Boolean)
        } builder {
            body {
                val sortAndCheck = "$op(comparator).iterator().assertSorted { a, b -> comparator.compare(a, b) <= 0 }"
                """
                val comparator = compareBy { it: $P -> it % THREE }.thenByDescending { it }
                ${of(0, 1, 2, 3, 4, 5)}.$sortAndCheck
                """
            }
            bodyAppend(Iterables/*, Sequences*/, ArraysOfObjects) { // `.asSequence()` and `object : Sequence {  }` are not equal
                val resultF = if (isSortedArray || f == Sequences) f else Lists
                """
                val comparator1 = compareBy<String> { it.toUpperCase().reversed() }
                val data = $of("cat", "dad", "BAD")
        
                ${resultF.assertEquals}(${resultF.of}("BAD", "dad", "cat"), data.$op(comparator1))
                ${resultF.assertEquals}(${resultF.of}("cat", "dad", "BAD"), data.$op(comparator1.reversed()))
                ${resultF.assertEquals}(${resultF.of}("BAD", "dad", "cat"), data.$op(comparator1.reversed().reversed()))
                """
            }
        }
    }

    val f_sortBy = listOf("sortBy", "sortByDescending").map { op ->
        val isDescending = op.contains("Descending")
        test("$op()") {
            include(Lists, ArraysOfObjects)
        } builder {
            body {
                val (bySecond, byFirst, byLength) = if (!isDescending) {
                    listOf(
                        """"ab" to 3, "aa" to 3, "aa" to 20""",
                        """"aa" to 3, "aa" to 20, "ab" to 3""",
                        """"aa" to 3, "ab" to 3, "aa" to 20"""
                    )
                } else {
                    listOf(
                        """"aa" to 20, "ab" to 3, "aa" to 3""",
                        """"ab" to 3, "aa" to 20, "aa" to 3""",
                        """"aa" to 20, "ab" to 3, "aa" to 3"""
                    )
                }

                """
                val data = $mutableOf("aa" to 20, "ab" to 3, "aa" to 3)
                data.$op { it.second }
                $assertEquals($of($bySecond), data)
        
                data.$op { it.first }
                $assertEquals($of($byFirst), data)
                
                data.$op { (it.first + it.second).length }
                $assertEquals($of($byLength), data)
                """
            }
        }
    }

    val f_sortedBy = listOf("sortedBy", "sortedByDescending").map { op ->
        val isDescending = op.contains("Descending")
        test("$op()") {
            include(Lists, ArraysOfObjects, ArraysOfPrimitives)
            exclude(PrimitiveType.Boolean, PrimitiveType.Char)
        } builder {
            val resultF = if (f == Sequences) f else Lists
            val reversed = if (isDescending) ".reversed()" else ""
            body {
                """
                val values = arrayOf("ac", "aD", "aba")
                val indices = ${of(0, 1, 2)}
        
                assertEquals(${resultF.of(1, 2, 0)}$reversed, indices.$op { values[it.toInt()] }) 
                """
            }
            bodyAppend(ArraysOfUnsigned) {
                """
                val array = $of(5, 2, 1, 9, 80, 0, MAX_VALUE, MAX_VALUE - 100)
                assertEquals(listOf<$P>(MAX_VALUE - 100, MAX_VALUE, 0, 1, 2, 5, 9, 80)$reversed, array.$op { it.to${P.drop(1)}() })
                """
            }
            bodyAppend(Lists, ArraysOfObjects) {
                """
                assertEquals(${resultF.of}("two" to 3, "three" to 20)$reversed, $of("three" to 20, "two" to 3).$op { it.second })
                assertEquals(${resultF.of}("three" to 20, "two" to 3)$reversed, $of("three" to 20, "two" to 3).$op { it.first })
                """
            }
        }
    }


    val f_shuffle = test("shuffle()") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        val type = if (f == Lists) "MutableList" else (primitive?.name ?: "") + "Array"
        val typeParam = if (primitive == null) "<*>" else ""
        val conversion = if (primitive == PrimitiveType.Boolean) "it % 2 == 0" else "it.to${test.P}()"
        body {
            """
            fun test(data: $type$typeParam) {
                val original = data.toMutableList()
                data.shuffle()
                val shuffled = data.toMutableList()
                assertNotEquals(original, shuffled)
                assertEquals(original.groupBy { it }, shuffled.groupBy { it })
            }
            test($type(100) { $conversion })
            """
        }
        bodyAppend(Lists, ArraysOfObjects) {
            """
            test($mutableOf(1, "x", null, Any(), 'a', 2u, 5.0))
            """
        }
    }

    val f_shuffleRandom = test("shuffleRandom()") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        val type = if (f == Lists) "MutableList" else (primitive?.name ?: "") + "Array"
        val typeParam = if (primitive == null) "<*>" else ""
        val conversion = if (primitive == PrimitiveType.Boolean) "it % 2 == 0" else "it.to${test.P}()"
        body {
            """
            fun test(data: $type$typeParam) {
                val seed = Random.nextInt()
                val original = data.toMutableList()
                val originalShuffled = original.shuffled(Random(seed))
                data.shuffle(Random(seed))
                val shuffled = data.toMutableList()
                assertNotEquals(original, shuffled)
                assertEquals(originalShuffled, shuffled)
            }
            test($type(16) { $conversion })
            """
        }
        bodyAppend(Lists, ArraysOfObjects) {
            """
            test($mutableOf(1, "x", null, Any(), 'a', 2u, 5.0))
            """
        }
    }
}
