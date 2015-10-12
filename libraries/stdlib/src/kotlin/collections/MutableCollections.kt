@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin

@Deprecated("Use property `size` instead", ReplaceWith("this.size"))
public inline fun Collection<*>.size() = size

@Deprecated("Use property `size` instead", ReplaceWith("this.size"))
public inline fun Map<*, *>.size() = size

@Deprecated("Use property `isEmpty` instead", ReplaceWith("this.isEmpty"))
public inline fun Collection<*>.isEmpty() = isEmpty

@Deprecated("Use property `isEmpty` instead", ReplaceWith("this.isEmpty"))
public inline fun Map<*, *>.isEmpty() = isEmpty


@Deprecated("Use property `key` instead", ReplaceWith("this.key"))
public fun <K, V> Map.Entry<K, V>.getKey(): K = key

@Deprecated("Use property `value` instead", ReplaceWith("this.value"))
public fun <K, V> Map.Entry<K, V>.getValue(): V = value

@Deprecated("Use operator 'get' instead", ReplaceWith("this[index]"))
public fun CharSequence.charAt(index: Int): Char = this[index]

@Deprecated("Use `removeAt` instead", ReplaceWith("this.removeAt(index)"))
public fun <E> MutableList<E>.remove(index: Int): E = removeAt(index)

@Deprecated("Use explicit cast to MutableCollection<Any?> instead", ReplaceWith("(this as MutableCollection<Any?>).remove(o)"))
public fun <E> MutableCollection<E>.remove(o: Any?): Boolean = remove(o as E)

@Deprecated("Use 'length' property instead", ReplaceWith("this.length"))
public fun CharSequence.length(): Int = length

@Deprecated("Use explicit cast to Map<Any?, V> instead", ReplaceWith("(this as Map<Any?, V>).get(o)"))
public inline operator fun <K, V> Map<K, V>.get(o: Any?): V? = get(o as K)

@Deprecated("Use explicit cast to Map<Any?, V> instead", ReplaceWith("(this as Map<Any?, V>).containsKey(o)"))
public inline fun <K, V> Map<K, V>.containsKey(o: Any?): Boolean = containsKey(o as K)

@Deprecated("Use explicit cast to Map<K, Any?> instead", ReplaceWith("(this as Map<K, Any?>).containsValue(o)"))
public inline fun <K, V> Map<K, V>.containsValue(o: Any?): Boolean = containsValue(o as V)

@Deprecated("Use 'keys' instead", ReplaceWith("this.keys()"))
public inline fun <K, V> Map<K, V>.keySet(): Set<K> = keys

@kotlin.jvm.JvmName("mutableKeys")
@Deprecated("Use 'keys' instead", ReplaceWith("this.keys"))
public inline fun <K, V> MutableMap<K, V>.keySet(): MutableSet<K> = keys

@Deprecated("Use 'entries' instead", ReplaceWith("this.entries"))
public inline fun <K, V> Map<K, V>.entrySet(): Set<Map.Entry<K, V>> = entries

@kotlin.jvm.JvmName("mutableEntrySet")
@Deprecated("Use 'entries' instead", ReplaceWith("this.entries"))
public inline fun <K, V> MutableMap<K, V>.entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = entries

@Deprecated("Use 'values' properties instead", ReplaceWith("this.values"))
public inline fun <K, V> Map<K, V>.values(): Collection<V> = values

@kotlin.jvm.JvmName("mutableValues")
@Deprecated("Use 'values' properties instead", ReplaceWith("this.values"))
public inline fun <K, V> MutableMap<K, V>.values(): MutableCollection<V> = values


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
