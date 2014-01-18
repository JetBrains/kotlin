package kotlin

/**
 * Adds all elements of the Iterator to the MutableCollection
 */
public fun <T> MutableCollection<T>.addAll(iterator: Iterator<T>): Unit
        = iterator.forEach { e -> add(e) }

/**
 * Adds all elements of the Iterable to the MutableCollection
 */
public fun <T> MutableCollection<T>.addAll(iterable: Iterable<T>): Unit
        = iterable.forEach { e ->  add(e) }