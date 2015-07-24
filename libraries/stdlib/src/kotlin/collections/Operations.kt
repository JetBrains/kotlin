package kotlin

import java.util.ArrayList
import kotlin.platform.platformName

/**
 * Returns a single list of all elements from all collections in the given collection.
 */
public fun <T> Iterable<Iterable<T>>.flatten(): List<T> {
    val result = ArrayList<T>()
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a sequence of all elements from all sequences in this sequence.
 */
public fun <T> Sequence<Sequence<T>>.flatten(): Sequence<T> {
    return MultiSequence(this)
}

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
 * *first* list is built from the first values of each pair from this collection,
 * *second* list is built from the second values of each pair from this collection.
 */
public fun <T, R> Iterable<Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val expectedSize = collectionSizeOrDefault(10)
    val listT = ArrayList<T>(expectedSize)
    val listR = ArrayList<R>(expectedSize)
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
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

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this sequence,
 * *second* list is built from the second values of each pair from this sequence.
 */
public fun <T, R> Sequence<Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val listT = ArrayList<T>()
    val listR = ArrayList<R>()
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}