package kotlin

/**
 * Adds all elements of the given *iterable* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.addAll(iterable: Iterable<T>): Unit {
    when (iterable) {
        is Collection -> addAll(iterable)
        else -> for (e in iterable) add(e)
    }
}

public fun <T> MutableCollection<in T>.addAll(stream: Stream<T>): Unit {
    for (e in stream) add(e)
}

public fun <T> MutableCollection<in T>.addAll(array: Array<T>): Unit {
    for (e in array) add(e)
}
