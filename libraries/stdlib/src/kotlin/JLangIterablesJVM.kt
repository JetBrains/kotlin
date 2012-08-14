package kotlin

import java.util.*

/** Copies all elements into a [[SortedSet]] */
public inline fun <in T> Iterable<T>.toSortedSet() : SortedSet<T> = toCollection(TreeSet<T>())

