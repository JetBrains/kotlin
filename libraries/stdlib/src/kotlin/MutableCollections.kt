package kotlin

/**
 * Adds all elements of the given *iterator* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<T>.addAll(iterator: Iterator<T>): Unit {
    for (e in iterator) add(e)
}

/**
 * Adds all elements of the given *iterable* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<T>.addAll(iterable: Iterable<T>): Unit {
    for (e in iterable) add(e)
}
