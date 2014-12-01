package kotlin

import java.util.*

/**
 * Returns a new [[SortedSet]] with the initial elements
 */
public fun sortedSetOf<T>(vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [[SortedSet]] with the given *comparator* and the initial elements
 */
public fun sortedSetOf<T>(comparator: Comparator<T>, vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

/**
 * Returns a list containing the elements returned by the
 * specified enumeration in the order they are returned by the
 * enumeration.
 */
public fun <T> Enumeration<T>.toList(): List<T> = Collections.list(this)
