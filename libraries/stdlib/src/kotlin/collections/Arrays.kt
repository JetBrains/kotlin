@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")


package kotlin

import java.util.*

/**
 * Returns an array with the specified [size], where each element is calculated by calling the specified
 * [init] function. The `init` function returns an array element given its index.
 */
public inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
    val result = arrayOfNulls<T>(size)

    for (i in 0..size - 1) {
        result[i] = init(i)
    }

    return result as Array<T>
}

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

/**
 * Returns a single list of all elements from all arrays in the given array.
 */
public fun <T> Array<Array<out T>>.flatten(): List<T> {
    val result = ArrayList<T>(sumBy { it.size() })
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this array,
 * *second* list is built from the second values of each pair from this array.
 */
public fun <T, R> Array<Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val listT = ArrayList<T>(size())
    val listR = ArrayList<R>(size())
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}
