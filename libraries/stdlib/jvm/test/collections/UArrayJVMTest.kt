/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UArrayJVMTest {
    @Test
    fun binarySearch() {

        fun <A, E> testFailures(array: A, binarySearch: A.(E, Int, Int) -> Int, element: E, arraySize: Int) {
            assertFailsWith<IllegalArgumentException> {
                array.binarySearch(element, 1, 0)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                array.binarySearch(element, 1, arraySize + 1)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                array.binarySearch(element, -1, 1)
            }
        }

        val array = uintArrayOf(1u, 0u, 4u, 5u, 8u, 12u, 2u, 3u, 7u, 7u, 7u, 9u, 2u)

        testFailures(array.toUByteArray(), UByteArray::binarySearch, 0u, array.size)
        testFailures(array.toUShortArray(), UShortArray::binarySearch, 0u, array.size)
        testFailures(array, UIntArray::binarySearch, 0u, array.size)
        testFailures(array.toULongArray(), ULongArray::binarySearch, 0u, array.size)

        fun <A, E> test(
            array: A,
            binarySearch: A.(E, Int, Int) -> Int,
            operations: List<OperationOnRange<UInt, Int>>,
            transform: UInt.() -> E
        ) {
            operations.forEach { o ->
                val result = array.binarySearch(o.element.transform(), o.fromIndex, o.toIndex)
                assertEquals(o.expectedResult, result)
            }
        }

        val operations = listOf(
            OperationOnRange(0u, 1, 6, 1),
            OperationOnRange(12u, 1, 6, 5),
            OperationOnRange(8u, 1, 6, 4),
            OperationOnRange(4u, 1, 6, 2),
            OperationOnRange(5u, 2, 6, 3),
            OperationOnRange(8u, 2, 5, 4),
            OperationOnRange(5u, 3, 4, 3),

            OperationOnRange(5u, 3, 3, -4),
            OperationOnRange(7u, 1, 6, -5),
            OperationOnRange(8u, 6, 12, -12),
            OperationOnRange(5u, 6, 12, -9),

            OperationOnRange(7u, 6, 12, 8)
        )

        test(array.toUByteArray(), UByteArray::binarySearch, operations, UInt::toUByte)
        test(array.toUShortArray(), UShortArray::binarySearch, operations, UInt::toUShort)
        test(array, UIntArray::binarySearch, operations, UInt::toUInt)
        test(array.toULongArray(), ULongArray::binarySearch, operations, UInt::toULong)
    }

    @Test
    fun fill() {
        fun <A, E> testFailures(array: A, fill: A.(E, Int, Int) -> Unit, element: E, arraySize: Int) {
            assertFailsWith<ArrayIndexOutOfBoundsException> {
                array.fill(element, -1, arraySize)
            }
            assertFailsWith<ArrayIndexOutOfBoundsException> {
                array.fill(element, 0, arraySize + 1)
            }
            assertFailsWith<IllegalArgumentException> {
                array.fill(element, 1, 0)
            }
        }

        testFailures(UByteArray(5) { it.toUByte() }, UByteArray::fill, 0u, 5)
        testFailures(UShortArray(5) { it.toUShort() }, UShortArray::fill, 0u, 5)
        testFailures(UIntArray(5) { it.toUInt() }, UIntArray::fill, 0u, 5)
        testFailures(ULongArray(5) { it.toULong() }, ULongArray::fill, 0u, 5)

        fun <A, E> test(
            array: UIntArray,
            fill: A.(E, Int, Int) -> Unit,
            operations: List<OperationOnRange<UInt, UIntArray>>,
            arrayTransform: UIntArray.() -> A,
            elementTransform: UInt.() -> E,
            contentEquals: A.(A) -> Boolean
        ) {
            for (o in operations) {
                val result = array.arrayTransform()
                result.fill(o.element.elementTransform(), o.fromIndex, o.toIndex)
                assertTrue(o.expectedResult.arrayTransform().contentEquals(result))
            }
        }

        val array = UIntArray(5) { it.toUInt() }

        val operations = listOf(
            OperationOnRange(5u, 1, 4, uintArrayOf(0u, 5u, 5u, 5u, 4u)),
            OperationOnRange(1u, 0, 5, uintArrayOf(1u, 1u, 1u, 1u, 1u)),
            OperationOnRange(2u, 0, 3, uintArrayOf(2u, 2u, 2u, 3u, 4u)),
            OperationOnRange(3u, 2, 5, uintArrayOf(0u, 1u, 3u, 3u, 3u))
        )

        test(array, UByteArray::fill, operations, UIntArray::toUByteArray, UInt::toUByte, UByteArray::contentEquals)
        test(array, UShortArray::fill, operations, UIntArray::toUShortArray, UInt::toUShort, UShortArray::contentEquals)
        test(array, UIntArray::fill, operations, UIntArray::copyOf, UInt::toUInt, UIntArray::contentEquals)
        test(array, ULongArray::fill, operations, UIntArray::toULongArray, UInt::toULong, ULongArray::contentEquals)
    }
}


private class OperationOnRange<E, R>(
    val element: E,
    val fromIndex: Int,
    val toIndex: Int,
    val expectedResult: R
)


private fun UIntArray.toUByteArray(): UByteArray {
    return UByteArray(size) { get(it).toUByte() }
}

private fun UIntArray.toUShortArray(): UShortArray {
    return UShortArray(size) { get(it).toUShort() }
}

private fun UIntArray.toULongArray(): ULongArray {
    return ULongArray(size) { get(it).toULong() }
}
