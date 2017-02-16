package kotlin.util

import kotlin.comparisons.*

// Yes, rather naive qsort for now.
private fun <T> partition(array: Array<T>, left: Int, right: Int): Int {
    var i = left
    var j = right
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

internal fun <T> sortArrayWith(
        array: Array<out T>, fromIndex: Int, toIndex: Int, comparator: Comparator<T>) {
    quickSort(array as Array<T>, fromIndex, toIndex, comparator)
}

internal fun <T> sortArrayComparable(array: Array<out T>) {
    quickSort(array as Array<T>, 0, array.size - 1)
}