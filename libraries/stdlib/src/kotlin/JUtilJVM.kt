package kotlin

import java.util.*

/** Returns a new read-only list of given elements */
public fun listOf<T>(vararg values: T): List<T> = arrayListOf(*values)

/** Returns a new read-only set of given elements */
public fun setOf<T>(vararg values: T): Set<T> = values.toCollection(LinkedHashSet<T>())

/** Returns a new read-only map of given pairs, where the first value is the key, and the second is value */
public fun mapOf<K, V>(vararg values: Pair<K, V>): Map<K, V> = hashMapOf(*values)

/** Returns a new ArrayList with a variable number of initial elements */
public fun arrayListOf<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size))

deprecated("Use listOf(...) or arrayListOf(...) instead")
public fun arrayList<T>(vararg values: T) : ArrayList<T> = arrayListOf(*values)

/** Returns a new LinkedList with a variable number of initial elements */
public fun linkedListOf<T>(vararg values: T) : LinkedList<T>  = values.toCollection(LinkedList<T>())

deprecated("Use listOf(...) or linkedListOf(...) instead")
public fun linkedList<T>(vararg values: T) : LinkedList<T>  = linkedListOf(*values)

/** Returns a new HashSet with a variable number of initial elements */
public fun hashSetOf<T>(vararg values: T) : HashSet<T> = values.toCollection(HashSet<T>(values.size))

deprecated("Use setOf(...) or hashSetOf(...) instead")
public fun hashSet<T>(vararg values: T) : HashSet<T> = hashSetOf(*values)

/**
 * Returns a new [[SortedSet]] with the initial elements
 */
public fun sortedSetOf<T>(vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>())

deprecated("Use sortedSetOf(...) instead")
public fun sortedSet<T>(vararg values: T) : TreeSet<T> = sortedSetOf(*values)

/**
 * Returns a new [[SortedSet]] with the given *comparator* and the initial elements
 */
public fun sortedSetOf<T>(comparator: Comparator<T>, vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

deprecated("Use sortedSetOf(...) instead")
public fun sortedSet<T>(comparator: Comparator<T>, vararg values: T) : TreeSet<T> = sortedSetOf(comparator, *values)

/**
 * Returns a new [[HashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createUsingPairs
 */
public fun <K,V> hashMapOf(vararg values: Pair<K,V>): HashMap<K,V> {
    val answer = HashMap<K,V>(values.size)
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}

deprecated("Use mapOf(...) or hashMapOf(...) instead")
public fun <K,V> hashMap(vararg values: Pair<K,V>): HashMap<K,V> = hashMapOf(*values)

/**
 * Returns a new [[SortedMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createSortedMap
 */
public fun <K,V> sortedMapOf(vararg values: Pair<K, V>): SortedMap<K,V> {
    val answer = TreeMap<K,V>()
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}

deprecated("Use sortedMapOf(...) instead")
public fun <K,V> sortedMap(vararg values: Pair<K, V>): SortedMap<K,V> = sortedMapOf(*values)

/**
 * Returns a new [[LinkedHashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value. This map preserves insertion order so iterating through
 * the map's entries will be in the same order
 *
 * @includeFunctionBody ../../test/MapTest.kt createLinkedMap
 */
public fun <K,V> linkedMapOf(vararg values: Pair<K, V>): LinkedHashMap<K,V> {
    val answer = LinkedHashMap<K,V>(values.size)
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}

deprecated("Use linkedMapOf(...) instead")
public fun <K,V> linkedMap(vararg values: Pair<K, V>): LinkedHashMap<K,V> = linkedMapOf(*values)

/** Returns the Set if its not null otherwise returns the empty set */
public fun <T> Set<T>?.orEmpty() : Set<T>
    = if (this != null) this else Collections.EMPTY_SET as Set<T>
