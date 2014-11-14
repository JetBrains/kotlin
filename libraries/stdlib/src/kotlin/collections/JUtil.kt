package kotlin

import java.util.*

private class stdlib_emptyListClass : List<Any> by ArrayList<Any>() {}
private val stdlib_emptyList : List<Any> = stdlib_emptyListClass()
private fun stdlib_emptyList<T>() = stdlib_emptyList as List<T>

private class stdlib_emptyMapClass : Map<Any, Any> by HashMap<Any, Any>() {}
private val stdlib_emptyMap : Map<Any, Any> = stdlib_emptyMapClass()
private fun stdlib_emptyMap<K,V>() = stdlib_emptyMap as Map<K,V>

/** Returns a new read-only list of given elements */
public fun listOf<T>(vararg values: T): List<T> = if (values.size() == 0) stdlib_emptyList() else arrayListOf(*values)

/** Returns an empty list */
public fun listOf<T>(): List<T> = stdlib_emptyList()

/** Returns a new read-only map of given pairs, where the first value is the key, and the second is value */
public fun mapOf<K, V>(vararg values: Pair<K, V>): Map<K, V> = if (values.size() == 0) stdlib_emptyMap() else linkedMapOf(*values)

/** Returns an empty read-only map */
public fun mapOf<K, V>(): Map<K, V> = stdlib_emptyMap()

/** Returns a new read-only set of given elements */
public fun setOf<T>(vararg values: T): Set<T> = values.toCollection(LinkedHashSet<T>())

/** Returns a new LinkedList with a variable number of initial elements */
public fun linkedListOf<T>(vararg values: T): LinkedList<T> = values.toCollection(LinkedList<T>())

/** Returns a new ArrayList with a variable number of initial elements */
public fun arrayListOf<T>(vararg values: T): ArrayList<T> = values.toCollection(ArrayList(values.size()))

/** Returns a new HashSet with a variable number of initial elements */
public fun hashSetOf<T>(vararg values: T): HashSet<T> = values.toCollection(HashSet(values.size()))

/**
 * Returns a new [[HashMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createUsingPairs
 */
public fun <K, V> hashMapOf(vararg values: Pair<K, V>): HashMap<K, V> {
    val answer = HashMap<K, V>(values.size())
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
    val answer = LinkedHashMap<K, V>(values.size())
    answer.putAll(*values)
    return answer
}

public val Collection<*>.indices: IntRange
    get() = 0..size() - 1

public val Int.indices: IntRange
    get() = 0..this - 1

/**
 * Returns the index of the last item in the list or -1 if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt lastIndex
 */
public val <T> List<T>.lastIndex: Int
    get() = this.size() - 1


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
