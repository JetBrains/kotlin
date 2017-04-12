/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.util

import kotlin.comparisons.*

// TODO: Implement sort for primitives with a custom comparator.

// Yes, rather naive qsort for now.
// Array<T>     =============================================================================
private fun <T> partition(array: Array<T>, left: Int, right: Int): Int {
    var i = left
    var j = right
    @Suppress("UNCHECKED_CAST")
    val pivot = array[(left + right) / 2] as Comparable<T>
    while (i <= j) {
        while (pivot.compareTo(array[i]) > 0) {
            i++
        }
        while (pivot.compareTo(array[j]) < 0) {
            j--
        }
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun <T> quickSort(array: Array<T>, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

private fun <T> partition(
        array: Array<T>, left: Int, right: Int, comparator: Comparator<T>): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (comparator.compare(array[i], pivot) < 0)
            i++
        while (comparator.compare(array[j], pivot) > 0)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun <T> quickSort(
        array: Array<T>, left: Int, right: Int, comparator: Comparator<T>) {
    val index = partition(array, left, right, comparator)
    if (left < index - 1)
        quickSort(array, left, index - 1, comparator)
    if (index < right)
        quickSort(array, index, right, comparator)
}

// ByteArray    =============================================================================
private fun partition(
        array: ByteArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: ByteArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// ShortArray   =============================================================================
private fun partition(
        array: ShortArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: ShortArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// IntArray     =============================================================================
private fun partition(
        array: IntArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: IntArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// LongArray    =============================================================================
private fun partition(
        array: LongArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: LongArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// CharArray    =============================================================================
private fun partition(
        array: CharArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: CharArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// FloatArray   =============================================================================
private fun partition(
        array: FloatArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: FloatArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// DoubleArray  =============================================================================
private fun partition(
        array: DoubleArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: DoubleArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// BooleanArray =============================================================================
private fun partition(
        array: BooleanArray, left: Int, right: Int): Int {
    var i = left
    var j = right
    val pivot = array[(left + right) / 2]
    while (i <= j) {
        while (array[i] < pivot)
            i++
        while (array[j] > pivot)
            j--
        if (i <= j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }
    return i
}

private fun quickSort(
        array: BooleanArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// Interfaces   =============================================================================
/**
 * Sorts the subarray specified by [fromIndex] (inclusive) and [toIndex] (exclusive) parameters
 * using the qsort algorithm with the given [comparator].
 */
internal fun <T> sortArrayWith(
        array: Array<out T>, fromIndex: Int = 0, toIndex: Int = array.size, comparator: Comparator<T>) {
    @Suppress("UNCHECKED_CAST")
    quickSort(array as Array<T>, fromIndex, toIndex - 1, comparator)
}

/**
 * Sorts a subarray of [Comparable] elements specified by [fromIndex] (inclusive) and
 * [toIndex] (exclusive) parameters using the qsort algorithm.
 */
internal fun <T> sortArrayComparable(array: Array<out T>) {
    @Suppress("UNCHECKED_CAST")
    quickSort(array as Array<T>, 0, array.size - 1)
}

/**
 * Sorts the given array using qsort algorithm.
 */
internal fun sortArray(array: ByteArray)    = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: ShortArray)   = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: IntArray)     = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: LongArray)    = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: CharArray)    = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: FloatArray)   = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: DoubleArray)  = quickSort(array, 0, array.size - 1)
internal fun sortArray(array: BooleanArray) = quickSort(array, 0, array.size - 1)