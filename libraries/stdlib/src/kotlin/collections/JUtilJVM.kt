package kotlin

import java.util.*

/** Returns a new read-only set of given elements */
public fun setOf<T>(vararg values: T): Set<T> = values.toCollection(LinkedHashSet<T>())

/** Returns a new LinkedList with a variable number of initial elements */
public fun linkedListOf<T>(vararg values: T): LinkedList<T> = values.toCollection(LinkedList<T>())

/**
 * Returns a new [[SortedSet]] with the initial elements
 */
public fun sortedSetOf<T>(vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [[SortedSet]] with the given *comparator* and the initial elements
 */
public fun sortedSetOf<T>(comparator: Comparator<T>, vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

/**
 * Returns a new [[SortedMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createSortedMap
 */
public fun <K, V> sortedMapOf(vararg values: Pair<K, V>): SortedMap<K, V> {
    val answer = TreeMap<K, V>()
    /**
    TODO replace by this simpler call when we can pass vararg values into other methods
    answer.putAll(values)
     */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}

/** Returns the Set if its not null otherwise returns the empty set */
public fun <T> Set<T>?.orEmpty(): Set<T>
        = if (this != null) this else Collections.EMPTY_SET as Set<T>


/**
 * Returns a list containing the elements returned by the
 * specified enumeration in the order they are returned by the
 * enumeration.
 */
public fun <T> Enumeration<T>.toList(): List<T> = Collections.list(this)
