package kotlin

/**
 * Adds element to this [[MutableCollection]]
 * Operator overloading for +=
 */
public fun <T> MutableCollection<in T>.plusAssign(element: T) {
    add(element)
}

/**
 * Removes element from this [[MutableCollection]]
 * Operator overloading for -=
 */
public fun <T> MutableCollection<in T>.minusAssign(element: T) {
    remove(element)
}

/**
 * Adds all elements of the given *iterable* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.addAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> addAll(iterable)
        else -> for (item in iterable) add(item)
    }
}

/**
 * Adds all elements of the given *iterable* to this [[MutableCollection]]
 * Operator overloading for +=
 */
public fun <T> MutableCollection<in T>.plusAssign(iterable: Iterable<T>) {
    addAll(iterable)
}

/**
 * Adds all elements of the given *stream* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.addAll(stream: Stream<T>) {
    for (item in stream) add(item)
}

/**
 * Adds all elements of the given *stream* to this [[MutableCollection]]
 * Operator overloading for +=
 */
public fun <T> MutableCollection<in T>.plusAssign(stream: Stream<T>) {
    addAll(stream)
}

/**
 * Adds all elements of the given *array* to this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.addAll(array: Array<T>) {
    for (item in array) add(item)
}

/**
 * Adds all elements of the given *array* to this [[MutableCollection]]
 * Operator overloading for +=
 */
public fun <T> MutableCollection<in T>.plusAssign(array: Array<T>) {
    addAll(array)
}

/**
 * Removes all elements of the given *iterable* from this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.removeAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> removeAll(iterable)
        else -> for (item in iterable) remove(item)
    }
}

/**
 * Removes all elements of the given *iterable* from this [[MutableCollection]]
 * Operator overloading for -=
 */
public fun <T> MutableCollection<in T>.minusAssign(iterable: Iterable<T>) {
    removeAll(iterable)
}

/**
 * Removes all elements of the given *stream* from this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.removeAll(stream: Stream<T>) {
    for (item in stream) remove(item)
}

/**
 * Removes all elements of the given *stream* from this [[MutableCollection]]
 * Operator overloading for -=
 */
public fun <T> MutableCollection<in T>.minusAssign(stream: Stream<T>) {
    removeAll(stream)
}

/**
 * Removes all elements of the given *array* from this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.removeAll(array: Array<T>) {
    for (item in array) remove(item)
}

/**
 * Removes all elements of the given *array* from this [[MutableCollection]]
 * Operator overloading for -=
 */
public fun <T> MutableCollection<in T>.minusAssign(array: Array<T>) {
    removeAll(array)
}

/**
 * Retains only elements of the given *iterable* in this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.retainAll(iterable: Iterable<T>) {
    when (iterable) {
        is Collection -> retainAll(iterable)
        else -> retainAll(iterable.toSet())
    }
}

/**
 * Retains only elements of the given *array* in this [[MutableCollection]]
 */
public fun <T> MutableCollection<in T>.retainAll(array: Array<T>) {
    retainAll(array.toSet())
}
