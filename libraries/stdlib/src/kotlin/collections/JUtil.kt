package kotlin

import java.util.*

private class stdlib_emptyListClass : List<Any> by ArrayList<Any>() {}
private val stdlib_emptyList : List<Any> = ArrayList<Any>() // TODO: Change to stdlib_emptyListClass() when KT-5192 is fixed
private fun stdlib_emptyList<T>() = stdlib_emptyList as List<T>

private class stdlib_emptyMapClass : Map<Any, Any> by HashMap<Any, Any>() {}
private val stdlib_emptyMap : Map<Any, Any> = HashMap<Any, Any>() // TODO: Change to stdlib_emptyMapClass() when KT-5192 is fixed
private fun stdlib_emptyMap<K,V>() = stdlib_emptyMap as Map<K,V>

/** Returns a new read-only list of given elements */
public fun listOf<T>(vararg values: T): List<T> = if (values.size == 0) stdlib_emptyList() else arrayListOf(*values)

/** Returns an empty list */
public fun listOf<T>(): List<T> = stdlib_emptyList()

/** Returns a new read-only map of given pairs, where the first value is the key, and the second is value */
public fun mapOf<K, V>(vararg values: Pair<K, V>): Map<K, V> = if (values.size == 0) stdlib_emptyMap() else linkedMapOf(*values)

/** Returns an empty read-only map */
public fun mapOf<K, V>(): Map<K, V> = stdlib_emptyMap()

/** Returns a new ArrayList with a variable number of initial elements */
public fun arrayListOf<T>(vararg values: T): ArrayList<T> = values.toCollection(ArrayList(values.size))

/** Returns a new HashSet with a variable number of initial elements */
public fun hashSetOf<T>(vararg values: T): HashSet<T> = values.toCollection(HashSet(values.size))

/**
 * Returns a new [[HashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createUsingPairs
 */
public fun <K, V> hashMapOf(vararg values: Pair<K, V>): HashMap<K, V> {
    val answer = HashMap<K, V>(values.size)
    answer.putAll(*values)
    return answer
}

/**
 * Returns a new [[LinkedHashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value. This map preserves insertion order so iterating through
 * the map's entries will be in the same order
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createLinkedMap
 */
public fun <K, V> linkedMapOf(vararg values: Pair<K, V>): LinkedHashMap<K, V> {
    val answer = LinkedHashMap<K, V>(values.size)
    answer.putAll(*values)
    return answer
}

/** Returns the size of the collection */
public val Collection<*>.size: Int
    get() = size()

/** Returns true if this collection is empty */
public val Collection<*>.empty: Boolean
    get() = isEmpty()

public val Collection<*>.indices: IntRange
    get() = 0..size - 1

public val Int.indices: IntRange
    get() = 0..this - 1

/** Returns true if the collection is not empty */
public fun <T> Collection<T>.isNotEmpty(): Boolean = !this.isEmpty()

/** Returns true if this collection is not empty */
public val Collection<*>.notEmpty: Boolean
    get() = isNotEmpty()

/** Returns the Collection if its not null otherwise it returns the empty list */
public fun <T> Collection<T>?.orEmpty(): Collection<T> = this ?: stdlib_emptyList()

// List APIs

/** Returns the List if its not null otherwise returns the empty list */
public fun <T> List<T>?.orEmpty(): List<T> = this ?: stdlib_emptyList()

/**
TODO figure out necessary variance/generics ninja stuff... :)
public inline fun <in T> List<T>.sort(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
val comparator = java.util.Comparator<T>() {
public fun compare(o1: T, o2: T): Int {
val v1 = transform(o1)
val v2 = transform(o2)
if (v1 == v2) {
return 0
} else {
return v1.compareTo(v2)
}
}
}
answer.sort(comparator)
}
 */

/**
 * Returns the first item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt first
 */
public val <T> List<T>.first: T?
    get() = this.head


/**
 * Returns the last item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt last
 */
public val <T> List<T>.last: T?
    get() {
        val s = this.size
        return if (s > 0) this[s - 1] else null
    }

/**
 * Returns the index of the last item in the list or -1 if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt lastIndex
 */
public val <T> List<T>.lastIndex: Int
    get() = this.size - 1

/**
 * Returns the first item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt head
 */
public val <T> List<T>.head: T?
    get() = if (this.isNotEmpty()) this[0] else null

/**
 * Returns all elements in this collection apart from the first one
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt tail
 */
public val <T> List<T>.tail: List<T>
    get() {
        return this.drop(1)
    }
