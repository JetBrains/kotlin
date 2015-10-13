@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin

/**
 * Adds the specified [element] to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(element: T) {
    this.add(element)
}

/**
 * Adds all elements of the given [collection] to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(collection: Iterable<T>) {
    this.addAll(collection)
}

/**
 * Adds all elements of the given [array] to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(array: Array<T>) {
    this.addAll(array)
}

/**
 * Adds all elements of the given [sequence] to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(sequence: Sequence<T>) {
    this.addAll(sequence)
}

/**
 * Removes a single instance of the specified [element] from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(element: T) {
    this.remove(element)
}

/**
 * Removes all elements contained in the given [collection] from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(collection: Iterable<T>) {
    this.removeAll(collection)
}

/**
 * Removes all elements contained in the given [array] from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(array: Array<T>) {
    this.removeAll(array)
}

/**
 * Removes all elements contained in the given [sequence] from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(sequence: Sequence<T>) {
    this.removeAll(sequence)
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
    removeAll(iterable.convertToSetForSetOperationWith(this))
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [sequence].
 */
public fun <T> MutableCollection<in T>.removeAll(sequence: Sequence<T>) {
    val set = sequence.toHashSet()
    if (set.isNotEmpty())
        removeAll(set)
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [array].
 */
public fun <T> MutableCollection<in T>.removeAll(array: Array<out T>) {
    if (array.isNotEmpty())
        removeAll(array.toHashSet())
//    else
//        removeAll(emptyList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [iterable].
 */
public fun <T> MutableCollection<in T>.retainAll(iterable: Iterable<T>) {
    retainAll(iterable.convertToSetForSetOperationWith(this))
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [array].
 */
public fun <T> MutableCollection<in T>.retainAll(array: Array<out T>) {
    if (array.isNotEmpty())
        retainAll(array.toHashSet())
    else
        clear()
//        retainAll(emptyList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [sequence].
 */
public fun <T> MutableCollection<in T>.retainAll(sequence: Sequence<T>) {
    val set = sequence.toHashSet()
    if (set.isNotEmpty())
        retainAll(set)
    else
        clear()
}
