/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*

class AssertContentEqualsTest {

    @Test
    fun testAssertContentEqualsIterable() {
        val list: Iterable<Int> = listOf(1, 2, 3)
        val range: Iterable<Int> = 1..3

        assertContentEquals(list, list) // ref equality
        assertContentEquals(list, range) // elements equal

        assertContentEquals(null as Iterable<Int>?, null as Iterable<Int>?) // null equality

        testFailureMessage("Expected <null> Iterable, actual <[1, 2, 3]>.") {
            assertContentEquals(null, list)
        }
        testFailureMessage("Expected non-null Iterable <1..3>, actual <null>.") {
            assertContentEquals(range, null)
        }
        testFailureMessage("Iterable elements differ at index 1. Expected element <2>, actual element <3>.") {
            assertContentEquals(range, listOf(1, 3, 2))
        }
        testFailureMessage("Iterable lengths differ. Expected length is bigger than 3, actual length is 3.") {
            assertContentEquals(1..4, list)
        }
        testFailureMessage("Iterable lengths differ. Expected length is 3, actual length is bigger than 3.") {
            assertContentEquals(list, 1..4)
        }
        testFailureMessage("Iterable elements differ at index 0. Expected element <1>, actual element <3>.") {
            assertContentEquals(setOf(1, 2, 3).asIterable(), setOf(3, 2, 1))
        }
    }

    @Test
    fun testAssertContentEqualsSequence() {
        val sequence1: Sequence<Int> = object : Sequence<Int> {
            override fun iterator(): Iterator<Int> = listOf(1, 2, 3).iterator()
            override fun toString(): String = "[1, 2, 3]"
        }
        val sequence2: Sequence<Int> = object : Sequence<Int> {
            override fun iterator(): Iterator<Int> = (1..3).iterator()
            override fun toString(): String = "1..3"
        }

        assertContentEquals(sequence1, sequence1) // ref equality
        assertContentEquals(sequence1, sequence2) // elements equal

        assertContentEquals(null as Iterable<Int>?, null as Iterable<Int>?) // null equality

        testFailureMessage("Expected <null> Sequence, actual <[1, 2, 3]>.") {
            assertContentEquals(null, sequence1)
        }
        testFailureMessage("Expected non-null Sequence <1..3>, actual <null>.") {
            assertContentEquals(sequence2, null)
        }
        testFailureMessage("Sequence elements differ at index 1. Expected element <2>, actual element <3>.") {
            assertContentEquals(sequence2, sequenceOf(1, 3, 2))
        }
        testFailureMessage("Sequence lengths differ. Expected length is bigger than 3, actual length is 3.") {
            assertContentEquals((1..4).asSequence(), sequence1)
        }
        testFailureMessage("Sequence lengths differ. Expected length is 3, actual length is bigger than 3.") {
            assertContentEquals(sequence1, (1..4).asSequence())
        }
    }

    @Test
    fun testAssertContentEqualsArray() {
        val array1: Array<Int> = arrayOf(1, 2, 3)
        val array2: Array<Int> = arrayOf(1, 2, 3)

        assertContentEquals(array1, array1) // ref equality
        assertContentEquals(array1, array2) // elements equal

        assertContentEquals(null as Array<Int>?, null as Array<Int>?) // null equality

        testFailureMessage("Expected <null> Array, actual <[1, 2, 3]>.") {
            assertContentEquals(null, array1)
        }
        testFailureMessage("Expected non-null Array <[1, 2, 3]>, actual <null>.") {
            assertContentEquals(array2, null)
        }
        testFailureMessage("Array elements differ at index 1. Expected element <2>, actual element <3>.\nExpected <[1, 2, 3]>, actual <[1, 3, 2]>.") {
            assertContentEquals(array2, arrayOf(1, 3, 2))
        }
        testFailureMessage("Array sizes differ. Expected size is 4, actual size is 3.\nExpected <[0, -1, -2, -3]>, actual <[1, 2, 3]>.") {
            assertContentEquals(Array(4) { -it }, array1)
        }
        testFailureMessage("Array sizes differ. Expected size is 3, actual size is 4.\nExpected <[1, 2, 3]>, actual <[0, -1, -2, -3]>.") {
            assertContentEquals(array1, Array(4) { -it })
        }
    }

    @Test
    fun testAssertContentEqualsDoubleArray() {
        val array1: DoubleArray = doubleArrayOf(1.0, Double.NaN, 3.0)
        val array2: DoubleArray = doubleArrayOf(1.0, Double.NaN, 3.0)

        assertContentEquals(array1, array1) // ref equality
        assertContentEquals(array1, array2) // elements equal

        assertContentEquals(null as DoubleArray?, null as DoubleArray?) // null equality

        testFailureMessage("Expected <null> Array, actual <${array1.contentToString()}>.") {
            assertContentEquals(null, array1)
        }
        testFailureMessage("Expected non-null Array <${array2.contentToString()}>, actual <null>.") {
            assertContentEquals(array2, null)
        }

        val sameSizeArray = doubleArrayOf(1.0, Double.NaN, 2.0)
        testFailureMessage("Array elements differ at index 2. Expected element <${array2[2]}>, actual element <${sameSizeArray[2]}>.\nExpected <${array2.contentToString()}>, actual <${sameSizeArray.contentToString()}>.") {
            assertContentEquals(array2, sameSizeArray)
        }

        val largerArray = DoubleArray(4) { -it.toDouble() }
        testFailureMessage("Array sizes differ. Expected size is 4, actual size is 3.\nExpected <${largerArray.contentToString()}>, actual <${array1.contentToString()}>.") {
            assertContentEquals(largerArray, array1)
        }
        testFailureMessage("Array sizes differ. Expected size is 3, actual size is 4.\nExpected <${array1.contentToString()}>, actual <${largerArray.contentToString()}>.") {
            assertContentEquals(array1, largerArray)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testAssertContentEqualsULongArray() {
        val array1: ULongArray = ulongArrayOf(1u, 2u, 3u)
        val array2: ULongArray = ulongArrayOf(1u, 2u, 3u)

        assertContentEquals(array1, array1) // ref equality
        assertContentEquals(array1, array2) // elements equal

        assertContentEquals(null as ULongArray?, null as ULongArray?) // null equality

        testFailureMessage("Expected <null> Array, actual <[1, 2, 3]>.") {
            assertContentEquals(null, array1)
        }
        testFailureMessage("Expected non-null Array <[1, 2, 3]>, actual <null>.") {
            assertContentEquals(array2, null)
        }
        testFailureMessage("Array elements differ at index 1. Expected element <2>, actual element <3>.\nExpected <[1, 2, 3]>, actual <[1, 3, 2]>.") {
            assertContentEquals(array2, ulongArrayOf(1u, 3u, 2u))
        }
        testFailureMessage("Array sizes differ. Expected size is 4, actual size is 3.\nExpected <[4, 3, 2, 1]>, actual <[1, 2, 3]>.") {
            assertContentEquals(ULongArray(4) { 4uL - it.toUInt() }, array1)
        }
        testFailureMessage("Array sizes differ. Expected size is 3, actual size is 4.\nExpected <[1, 2, 3]>, actual <[4, 3, 2, 1]>.") {
            assertContentEquals(array1, ULongArray(4) { 4uL - it.toUInt() })
        }
    }
}
