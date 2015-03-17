package kotlin

/**
 * Adds all elements of the given [iterable] to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> addAll(iterable)
        else -> for (item in iterable) add(item)
    }
}

/**
 * Adds all elements of the given [sequence] to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(sequence: Sequence<T>) {
    for (item in sequence) add(item)
}

/**
 * Adds all elements of the given [sequence] to this [MutableCollection].
 */
deprecated("Use Sequence<T> instead of Stream<T>")
public fun <T> MutableCollection<in T>.addAll(stream: Stream<T>) {
    for (item in stream) add(item)
}

/**
 * Adds all elements of the given [array] to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(array: Array<out T>) {
    for (item in array) add(item)
}

/**
 * Removes all elements of the given [iterable] from this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.removeAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> removeAll(iterable)
        else -> for (item in iterable) remove(item)
    }
}

/**
 * Removes all elements of the given [sequence] from this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.removeAll(sequence: Sequence<T>) {
    for (item in sequence) remove(item)
}

/**
 * Removes all elements of the given [stream] from this [MutableCollection].
 */
deprecated("Use Sequence<T> instead of Stream<T>")
public fun <T> MutableCollection<in T>.removeAll(stream: Stream<T>) {
    for (item in stream) remove(item)
}

/**
 * Removes all elements of the given [array] from this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.removeAll(array: Array<out T>) {
    for (item in array) remove(item)
}

/**
 * Retains only elements of the given [iterable] in this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.retainAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> retainAll(iterable)
        else -> retainAll(iterable.toSet())
    }
}

/**
 * Retains only elements of the given [array] in this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.retainAll(array: Array<out T>) {
    retainAll(array.toSet())
}
