package kotlin

/**
 * Adds the specified [element] to this mutable collection.
 */
public fun <T> MutableCollection<in T>.plusAssign(element: T) {
    this.add(element)
}

/**
 * Adds all elements of the given [collection] to this mutable collection.
 */
public fun <T> MutableCollection<in T>.plusAssign(collection: Iterable<T>) {
    this.addAll(collection)
}

/**
 * Adds all elements of the given [array] to this mutable collection.
 */
public fun <T> MutableCollection<in T>.plusAssign(array: Array<T>) {
    this.addAll(array)
}


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
 * Adds all elements of the given [array] to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(array: Array<out T>) {
    addAll(array.asList())
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [iterable].
 */
public fun <T> MutableCollection<in T>.removeAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> removeAll(iterable)
        else -> removeAll(iterable.toHashSet())
    }
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [sequence].
 */
public fun <T> MutableCollection<in T>.removeAll(sequence: Sequence<T>) {
    removeAll(sequence.toHashSet())
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [array].
 */
public fun <T> MutableCollection<in T>.removeAll(array: Array<out T>) {
    removeAll(array.toHashSet())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [iterable].
 */
public fun <T> MutableCollection<in T>.retainAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> retainAll(iterable)
        else -> retainAll(iterable.toHashSet())
    }
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [array].
 */
public fun <T> MutableCollection<in T>.retainAll(array: Array<out T>) {
    retainAll(array.toHashSet())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [sequence].
 */
public fun <T> MutableCollection<in T>.retainAll(sequence: Sequence<T>) {
    retainAll(sequence.toHashSet())
}
