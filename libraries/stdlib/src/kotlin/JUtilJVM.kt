package kotlin

import java.util.*

/** Returns a new LinkedList with a variable number of initial elements */
public inline fun linkedList<T>(vararg values: T) : LinkedList<T>  = values.toCollection(LinkedList<T>())

/** Returns a new HashSet with a variable number of initial elements */
public inline fun hashSet<T>(vararg values: T) : HashSet<T> = values.toCollection(HashSet<T>(values.size))

/**
 * Returns a new [[SortedSet]] with the initial elements
 */
public inline fun sortedSet<T>(vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [[SortedSet]] with the given *comparator* and the initial elements
 */
public inline fun sortedSet<T>(comparator: Comparator<T>, vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

/**
 * Returns a new [[HashMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createUsingTuples
 */
public inline fun <K,V> hashMap(vararg values: Pair<K,V>): HashMap<K,V> {
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

/**
 * Returns a new [[SortedMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createSortedMap
 */
public inline fun <K,V> sortedMap(vararg values: Pair<K, V>): SortedMap<K,V> {
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

/**
 * Returns a new [[LinkedHashMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value. This map preserves insertion order so iterating through
 * the map's entries will be in the same order
 *
 * @includeFunctionBody ../../test/MapTest.kt createLinkedMap
 */
public inline fun <K,V> linkedMap(vararg values: Pair<K, V>): LinkedHashMap<K,V> {
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


/** Returns the Set if its not null otherwise returns the empty set */
public inline fun <T> Set<T>?.orEmpty() : Set<T>
    = if (this != null) this else Collections.EMPTY_SET as Set<T>

/** Returns a new ArrayList with a variable number of initial elements */
public inline fun arrayList<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size))



