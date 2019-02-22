/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

// UByteArray    =============================================================================
@ExperimentalUnsignedTypes
private fun partition(
    array: UByteArray, left: Int, right: Int): Int {
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

@ExperimentalUnsignedTypes
private fun quickSort(
    array: UByteArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// UShortArray   =============================================================================
@ExperimentalUnsignedTypes
private fun partition(
    array: UShortArray, left: Int, right: Int): Int {
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

@ExperimentalUnsignedTypes
private fun quickSort(
    array: UShortArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// UIntArray     =============================================================================
@ExperimentalUnsignedTypes
private fun partition(
    array: UIntArray, left: Int, right: Int): Int {
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

@ExperimentalUnsignedTypes
private fun quickSort(
    array: UIntArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}

// ULongArray    =============================================================================
@ExperimentalUnsignedTypes
private fun partition(
    array: ULongArray, left: Int, right: Int): Int {
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

@ExperimentalUnsignedTypes
private fun quickSort(
    array: ULongArray, left: Int, right: Int) {
    val index = partition(array, left, right)
    if (left < index - 1)
        quickSort(array, left, index - 1)
    if (index < right)
        quickSort(array, index, right)
}


// Interfaces   =============================================================================
/**
 * Sorts the given array using qsort algorithm.
 */
@ExperimentalUnsignedTypes
internal fun sortArray(array: UByteArray)    = quickSort(array, 0, array.size - 1)
@ExperimentalUnsignedTypes
internal fun sortArray(array: UShortArray)   = quickSort(array, 0, array.size - 1)
@ExperimentalUnsignedTypes
internal fun sortArray(array: UIntArray)     = quickSort(array, 0, array.size - 1)
@ExperimentalUnsignedTypes
internal fun sortArray(array: ULongArray)    = quickSort(array, 0, array.size - 1)