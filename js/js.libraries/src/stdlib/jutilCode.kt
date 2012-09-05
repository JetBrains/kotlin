package kotlin

import java.util.*

public inline fun <T> Iterable<T>.toString(): String {
    return makeString(", ", "[", "]")
}

public inline fun <T> List<T>.equals(that: List<T>): Boolean {
    val s1 = this.size()
    val s2 = that.size()
    if (s1 == s2) {
        for (i in 0.rangeTo(s1)) {
            val elem1 = this.get(i)
            val elem2 = that.get(i)
            if (elem1 != elem2) {
                return false
            }
        }
        return true
    }
    return false
}

/** Returns a new ArrayList with a variable number of initial elements */
public inline fun arrayList<T>(vararg values: T) : ArrayList<T> {
    val list = ArrayList<T>()
    for (value in values) {
        list.add(value)
    }
    return list
}

/** Returns a new HashSet with a variable number of initial elements */
public inline fun hashSet<T>(vararg values: T) : HashSet<T> {
    val list = HashSet<T>()
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
public inline fun <K,V,R> Map<K,V>.map(transform: (Map.Entry<K,V>) -> R) : List<R> {
    return mapTo(java.util.ArrayList<R>(), transform)
}


/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt mapValues
 */
public inline fun <K,V,R> MutableMap<K,V>.mapValues(transform : (Map.Entry<K,V>) -> R): Map<K,R> {
    return mapValuesTo(HashMap<K,R>(), transform)
}


/**
 * Returns a new List containing the results of applying the given *transform* function to each element in this collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt map
 */
public inline fun <T, R> Collection<T>.map(transform : (T) -> R) : List<R> {
    return mapTo(java.util.ArrayList<R>(), transform)
}
