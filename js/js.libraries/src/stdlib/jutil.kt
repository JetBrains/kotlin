package kotlin

import java.util.*

/** Returns a new ArrayList with a variable number of initial elements */
public inline fun arrayList<T>(vararg values: T) : ArrayList<T> {
    val list = ArrayList<T>()
    for (value in values) {
        list.add(value)
    }
    return list
}


/**
 * Returns a new List containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/CollectionTest.kt map
 */
public inline fun <K,V,R> java.util.Map<K,V>.map(transform: (java.util.Map.Entry<K,V>) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(), transform)
}


/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt mapValues
 */
public inline fun <K,V,R> java.util.Map<K,V>.mapValues(transform : (java.util.Map.Entry<K,V>) -> R): java.util.Map<K,R> {
    return mapValuesTo(java.util.HashMap<K,R>(), transform)
}


/**
 * Returns a new List containing the results of applying the given *transform* function to each element in this collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt map
 */
public inline fun <T, R> java.util.Collection<T>.map(transform : (T) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(), transform)
}
