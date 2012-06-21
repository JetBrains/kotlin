package kotlin

import java.util.*

/** Copies all elements into a [[Set]] */
public inline fun <in T> java.lang.Iterable<T>.toSet() : Set<T> = toCollection(HashSet<T>())

/** Copies all elements into a [[SortedSet]] */
public inline fun <in T> java.lang.Iterable<T>.toSortedSet() : SortedSet<T> = toCollection(TreeSet<T>())

