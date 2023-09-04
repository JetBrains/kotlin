/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

// Native-specific part of stdlib/test/collections/ArraysTest.kt
class ArraysNativeTest {
    @Test fun byteArraySet() {
        val arr = ByteArray(5)
        arr[1] = 1
        arr[3] = -1
        assertEquals(1, arr[1])
        assertEquals(-1, arr[3])
    }

    @Test fun shortArraySet() {
        val arr = ShortArray(5)
        arr[1] = 1
        arr[3] = -1
        assertEquals(1, arr[1])
        assertEquals(-1, arr[3])
    }

    @Test fun intArraySet() {
        val arr = IntArray(5)
        arr[1] = 1
        arr[3] = -1
        assertEquals(1, arr[1])
        assertEquals(-1, arr[3])
    }

    @Test fun longArraySet() {
        val arr = LongArray(5)
        arr[1] = 1
        arr[3] = -1
        assertEquals(1, arr[1])
        assertEquals(-1, arr[3])
    }

    @Test fun arrayGetOutOfBounds() {
        val arr = Array<Any?>(5) { null }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun byteArrayGetOutOfBounds() {
        val arr = ByteArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun shortArrayGetOutOfBounds() {
        val arr = ShortArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun intArrayGetOutOfBounds() {
        val arr = IntArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun floatArrayGetOutOfBounds() {
        val arr = FloatArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun doubleArrayGetOutOfBounds() {
        val arr = DoubleArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun charArrayGetOutOfBounds() {
        val arr = CharArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun arraySetOutOfBounds() {
        val arr = Array<Any?>(5) { null }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = Any()
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = Any()
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = Any()
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = Any()
        }
    }

    @Test fun byteArraySetOutOfBounds() {
        val arr = ByteArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1
        }
    }

    @Test fun shortArraySetOutOfBounds() {
        val arr = ShortArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1
        }
    }

    @Test fun intArraySetOutOfBounds() {
        val arr = IntArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1
        }
    }

    @Test fun floatArraySetOutOfBounds() {
        val arr = FloatArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1.0f
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1.0f
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1.0f
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1.0f
        }
    }

    @Test fun doubleArraySetOutOfBounds() {
        val arr = DoubleArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1.0
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1.0
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1.0
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1.0
        }
    }

    @Test fun charArraySetOutOfBounds() {
        val arr = CharArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = '1'
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = '1'
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = '1'
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = '1'
        }
    }

    @Test fun largeByteArray() {
        val arr = ByteArray(1000000) { it.toByte() }

        var sum = 0
        for (b in arr) {
            sum += b
        }

        assertEquals(-497952, sum)
    }

    @Test fun largeArray() {
        val arr = Array<Byte>(1000000) { it.toByte() }

        var sum = 0
        for (b in arr) {
            sum += b
        }

        assertEquals(-497952, sum)
    }

    private fun Array<Int>.assertSorted(cmp: Comparator<Int>, message: String = "") {
        for (i in 1 until size) {
            assertTrue(cmp.compare(this[i - 1], this[i]) <= 0, message)
        }
    }

    private fun Array<MyComparable>.assertSorted(message: String = "") {
        for (i in 1 until size) {
            assertTrue(this[i - 1] <= this[i], message)
        }
    }

    private class MyComparable (val value: Int, val comparator: Comparator<Int>): Comparable<MyComparable> {
        override fun compareTo(other: MyComparable): Int = comparator.compare(value, other.value)
    }

    @Test
    fun sortWith() {
        listOf<Array<Int>>(
                arrayOf(),
                arrayOf(1),
                arrayOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
                arrayOf(Int.MAX_VALUE, 0, Int.MIN_VALUE),
                arrayOf(1, 2, 3),
                arrayOf(-2, -1, 99, 1, 2),
                arrayOf(90, 91, 0, 98, 99),
                arrayOf(2, 1, 99, -1, 2),
                arrayOf(99, 98, 0, 91, 90),
                arrayOf(42, 42, 42),
                arrayOf(99, 42, 0, 42, 50),
                arrayOf(
                        100000, 190001, 200002, 200003, 200004, 210005, 220006, 250007, 300008, 310009, 360010, 365011,
                        380012, 390013, 390014, 399015, 400016, 400017, 400018, 400019, 400020, 400021, 400022, 400023,
                        400024, 400025, 400026, 450027, 450028, 480029, 480030, 500031, 500032, 500033, 500034, 500035,
                        500036, 500037, 500038, 500039, 500040, 500041, 500042, 500043, 500044, 500045, 500046, 500047,
                        500048, 500049, 500050, 500051, 500052, 500053, 505054, 510055, 510056, 510057, 510058, 510059,
                        510060, 510061, 510062, 510063, 410064, 410065, 511066, 511067, 520068, 520069, 420070, 520071,
                        530072, 530073, 530074, 430075, 430076, 530077, 540078, 540079, 540080, 540081, 540082, 540083,
                        540084, 490085, 540086, 540087, 542088, 544089, 546090, 550091, 550092, 550093, 550094, 590095,
                        590096, 595097, 600098, 600099, 600100, 600101, 600102, 600103, 600104, 550105, 600106, 600107,
                        600108, 600109, 600110, 610111, 610112, 610113, 620114, 620115, 620116, 620117, 620118, 620119,
                        640120, 640121, 645122, 645123, 645124, 645125, 645126, 645127, 650128, 700129, 700130
                )
        ).forEach { array ->
            data class ComparatorInfo(val name: String, val isCorrect: Boolean, val comparator: Comparator<Int>)
            // Assert that the array is sorted in terms of a comparator only for correct/partially correct cases
            listOf<ComparatorInfo>(
                    ComparatorInfo("Correct increasing", true) { a, b ->
                        when {
                            a > b -> 1
                            a < b -> -1
                            else -> 0
                        }
                    },
                    ComparatorInfo("Correct decreasing", true) { a, b ->
                        when {
                            a < b -> 1
                            a > b -> -1
                            else -> 0
                        }
                    },
                    ComparatorInfo("Incorrect increasing", false) { a, b -> if (a > b) 1 else -1 },
                    ComparatorInfo("Incorrect decreasing", false) { a, b -> if (a < b) 1 else -1 },
                    ComparatorInfo("Always 1", false) { a, b -> 1 },
                    ComparatorInfo("Always -1", false) { a, b -> -1 },
                    ComparatorInfo("Always 0", false) { a, b -> 0 },
            ).forEach { (name, isCorrect, comparator) ->
                // Test with custom comparator
                val arrayUnderTest = array.copyOf()
                arrayUnderTest.sortWith(comparator)
                if (isCorrect) {
                    arrayUnderTest.assertSorted(comparator, """
                    Assert sorted failed for comparator: "${name}"
                    Array: ${array.joinToString()}
                    Array after sorting: ${arrayUnderTest.joinToString()}
                """.trimIndent())
                }

                // Test of a custom comparable
                val comparableArrayUnderTest = Array(array.size) { i ->
                    MyComparable(array[i], comparator)
                }
                comparableArrayUnderTest.sort()

                if (isCorrect) {
                    comparableArrayUnderTest.assertSorted("""
                    Assert sorted failed for Comparable: "${name}"
                    Array: ${array.joinToString()}
                    Array after sorting: ${comparableArrayUnderTest.joinToString()}
                """.trimIndent())
                }
            }
        }
    }
}