package kotlin

import java.util.*


/**
 * Returns an immutable list containing only the specified object [value].
 * The returned list is serializable.
 */
public fun listOf<T>(value: T): List<T> = Collections.singletonList(value)

/**
 * Returns an immutable set containing only the specified object [value].
 * The returned set is serializable.
 */
public fun setOf<T>(value: T): Set<T> = Collections.singleton(value)

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.  The returned map is serializable.
 */
public fun mapOf<K, V>(keyValuePair: Pair<K, V>): Map<K, V> = Collections.singletonMap(keyValuePair.first, keyValuePair.second)

/**
 * Returns a new [SortedSet] with the given elements.
 */
public fun sortedSetOf<T>(vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [SortedSet] with the given [comparator] and elements.
 */
public fun sortedSetOf<T>(comparator: Comparator<T>, vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

/**
 * Returns a list containing the elements returned by this enumeration
 * in the order they are returned by the enumeration.
 */
public fun <T> Enumeration<T>.toList(): List<T> = Collections.list(this)
