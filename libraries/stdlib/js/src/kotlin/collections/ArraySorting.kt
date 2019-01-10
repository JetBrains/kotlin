/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal fun <T> sortArrayWith(array: Array<out T>, comparison: (T, T) -> Int) {
    if (getStableSortingIsSupported()) {
        array.asDynamic().sort(comparison)
    } else {
        mergeSort(array.unsafeCast<Array<T>>(), 0, array.lastIndex, Comparator(comparison))
    }
}

internal fun <T> sortArrayWith(array: Array<out T>, comparator: Comparator<in T>) {
    if (getStableSortingIsSupported()) {
        val comparison = { a: T, b: T -> comparator.compare(a, b) }
        array.asDynamic().sort(comparison)
    } else {
        mergeSort(array.unsafeCast<Array<T>>(), 0, array.lastIndex, comparator)
    }
}

internal fun <T : Comparable<T>> sortArray(array: Array<out T>) {
    if (getStableSortingIsSupported()) {
        val comparison = { a: T, b: T -> a.compareTo(b) }
        array.asDynamic().sort(comparison)
    } else {
        mergeSort(array.unsafeCast<Array<T>>(), 0, array.lastIndex, naturalOrder())
    }
}

private var _stableSortingIsSupported: Boolean? = null
private fun getStableSortingIsSupported(): Boolean {
    _stableSortingIsSupported?.let { return it }
    _stableSortingIsSupported = false

    val array = js("[]").unsafeCast<Array<Int>>()
    // known implementations may use stable sort for arrays of up to 512 elements
    // so we create slightly more elements to test stability
    for (index in 0 until 600) array.asDynamic().push(index)
    val comparison = { a: Int, b: Int -> (a and 3) - (b and 3) }
    array.asDynamic().sort(comparison)
    for (index in 1 until array.size) {
        val a = array[index - 1]
        val b = array[index]
        if ((a and 3) == (b and 3) && a >= b) return false
    }
    _stableSortingIsSupported = true
    return true
}


private fun <T> mergeSort(array: Array<T>, start: Int, endInclusive: Int, comparator: Comparator<in T>) {
    val buffer = arrayOfNulls<Any?>(array.size).unsafeCast<Array<T>>()
    val result = mergeSort(array, buffer, start, endInclusive, comparator)
    if (result !== array) {
        result.forEachIndexed { i, v -> array[i] = v }
    }
}

// Both start and end are inclusive indices.
private fun <T> mergeSort(array: Array<T>, buffer: Array<T>, start: Int, end: Int, comparator: Comparator<in T>): Array<T> {
    if (start == end) {
        return array
    }

    val median = (start + end) / 2
    val left = mergeSort(array, buffer, start, median, comparator)
    val right = mergeSort(array, buffer, median + 1, end, comparator)

    val target = if (left === buffer) array else buffer

    // Merge.
    var leftIndex = start
    var rightIndex = median + 1
    for (i in start..end) {
        when {
            leftIndex <= median && rightIndex <= end -> {
                val leftValue = left[leftIndex]
                val rightValue = right[rightIndex]

                if (comparator.compare(leftValue, rightValue) <= 0) {
                    target[i] = leftValue
                    leftIndex++
                } else {
                    target[i] = rightValue
                    rightIndex++
                }
            }
            leftIndex <= median -> {
                target[i] = left[leftIndex]
                leftIndex++
            }
            else /* rightIndex <= end */ -> {
                target[i] = right[rightIndex]
                rightIndex++
            }
        }
    }

    return target
}