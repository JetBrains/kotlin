@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin

import java.util.*

/**
 * Checks if all elements in the specified collection are contained in this collection.
 *
 * Allows to overcome type-safety restriction of `containsAll` that requires to pass a collection of type `Collection<E>`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Collection<*>.containsAllRaw(elements: Collection<Any?>): Boolean = (this as Collection<Any?>).containsAll(elements)

/**
 * Removes a single instance of the specified element from this
 * collection, if it is present.
 *
 * Allows to overcome type-safety restriction of `remove` that requires to pass an element of type `E`.
 *
 * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <E> MutableCollection<E>.removeRaw(element: Any?): Boolean = (this as MutableCollection<Any?>).remove(element)

/**
 * Removes all of this collection's elements that are also contained in the specified collection.

 * Allows to overcome type-safety restriction of `removeAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <E> MutableCollection<E>.removeAllRaw(elements: Collection<Any?>): Boolean = (this as MutableCollection<Any?>).removeAll(elements)

/**
 * Retains only the elements in this collection that are contained in the specified collection.
 *
 * Allows to overcome type-safety restriction of `retailAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <E> MutableCollection<E>.retainAllRaw(elements: Collection<Any?>): Boolean = (this as MutableCollection<Any?>).retainAll(elements)



@Deprecated("Use operator 'get' instead", ReplaceWith("this[index]"))
public fun CharSequence.charAt(index: Int): Char = this[index]

@Deprecated("Use property 'size' instead", ReplaceWith("size"))
public inline fun Collection<*>.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("size"))
public inline fun Map<*, *>.size() = size

@Deprecated("Use property 'key' instead", ReplaceWith("key"))
public fun <K, V> Map.Entry<K, V>.getKey(): K = key

@Deprecated("Use containsAllRaw() instead.", ReplaceWith("containsAllRaw(collection)"))
public fun <E> Collection<E>.containsAll(collection: Collection<Any?>): Boolean = containsAllRaw(collection)

@Deprecated("Use property 'value' instead.", ReplaceWith("value"))
public fun <K, V> Map.Entry<K, V>.getValue(): V = value

@Deprecated("Use 'removeAt' instead.", ReplaceWith("removeAt(index)"))
public fun <E> MutableList<E>.remove(index: Int): E = removeAt(index)

@Deprecated("Use 'removeRaw' instead.", ReplaceWith("removeRaw(o)"))
public fun <E> MutableCollection<E>.remove(o: Any?): Boolean = removeRaw(o)

@Deprecated("Use 'removeAllRaw' instead.", ReplaceWith("removeAllRaw(c)"))
public fun <E> MutableCollection<E>.removeAll(c: Collection<Any?>): Boolean = removeAllRaw(c)

@Deprecated("Use 'retainAllRaw' instead.", ReplaceWith("retainAllRaw(c)"))
public fun <E> MutableCollection<E>.retainAll(c: Collection<Any?>): Boolean = retainAllRaw(c)

@Deprecated("Use 'indexOfRaw' instead.", ReplaceWith("indexOfRaw(o)"))
public fun <E> List<E>.indexOf(o: Any?): Int = indexOfRaw(o)

@Deprecated("Use 'lastIndexOfRaw' instead.", ReplaceWith("lastIndexOfRaw(o)"))
public fun <E> List<E>.lastIndexOf(o: Any?): Int = lastIndexOfRaw(o)

@Deprecated("Use property 'length' instead.", ReplaceWith("length"))
public fun CharSequence.length(): Int = length

@Deprecated("Use 'getRaw' instead.", ReplaceWith("getRaw(key)"))
public inline operator fun <K, V> Map<K, V>.get(key: Any?): V? = getRaw(key)

@Deprecated("Use 'containsKeyRaw' instead.", ReplaceWith("containsKeyRaw(key)"))
public inline fun <K, V> Map<K, V>.containsKey(key: Any?): Boolean = containsKeyRaw(key)

@Deprecated("Use 'containsValueRaw' instead.", ReplaceWith("containsValueRaw(value)"))
public inline fun <K, V> Map<K, V>.containsValue(value: Any?): Boolean = containsValueRaw(value)

@Deprecated("Use property 'keys' instead.", ReplaceWith("keys"))
public inline fun <K, V> Map<K, V>.keySet(): Set<K> = keys

@kotlin.jvm.JvmName("mutableKeys")
@Deprecated("Use property 'keys' instead.", ReplaceWith("keys"))
public fun <K, V> MutableMap<K, V>.keySet(): MutableSet<K> = keys

@Deprecated("Use property 'entries' instead.", ReplaceWith("entries"))
public inline fun <K, V> Map<K, V>.entrySet(): Set<Map.Entry<K, V>> = entries

@kotlin.jvm.JvmName("mutableEntrySet")
@Deprecated("Use property 'entries' instead.", ReplaceWith("entries"))
public fun <K, V> MutableMap<K, V>.entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = entries

@Deprecated("Use property 'values' instead.", ReplaceWith("values"))
public inline fun <K, V> Map<K, V>.values(): Collection<V> = values

@kotlin.jvm.JvmName("mutableValues")
@Deprecated("Use property 'values' instead.", ReplaceWith("values"))
public fun <K, V> MutableMap<K, V>.values(): MutableCollection<V> = values

/**
 * Adds the specified [element] to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(element: T) {
    this.add(element)
}

/**
 * Adds all elements of the given [elements] collection to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(elements: Iterable<T>) {
    this.addAll(elements)
}

/**
 * Adds all elements of the given [elements] array to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(elements: Array<T>) {
    this.addAll(elements)
}

/**
 * Adds all elements of the given [elements] sequence to this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.plusAssign(elements: Sequence<T>) {
    this.addAll(elements)
}

/**
 * Removes a single instance of the specified [element] from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(element: T) {
    this.remove(element)
}

/**
 * Removes all elements contained in the given [elements] collection from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(elements: Iterable<T>) {
    this.removeAll(elements)
}

/**
 * Removes all elements contained in the given [elements] array from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(elements: Array<T>) {
    this.removeAll(elements)
}

/**
 * Removes all elements contained in the given [elements] sequence from this mutable collection.
 */
public operator fun <T> MutableCollection<in T>.minusAssign(elements: Sequence<T>) {
    this.removeAll(elements)
}

/**
 * Adds all elements of the given [elements] collection to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(elements: Iterable<T>) {
    when (elements) {
        is Collection -> addAll(elements)
        else -> for (item in elements) add(item)
    }
}

/**
 * Adds all elements of the given [elements] sequence to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(elements: Sequence<T>) {
    for (item in elements) add(item)
}

/**
 * Adds all elements of the given [elements] array to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(elements: Array<out T>) {
    addAll(elements.asList())
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] collection.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Iterable<T>) {
    removeAll(elements.convertToSetForSetOperationWith(this))
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] sequence.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Sequence<T>) {
    val set = elements.toHashSet()
    if (set.isNotEmpty())
        removeAll(set)
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] array.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Array<out T>) {
    if (elements.isNotEmpty())
        removeAll(elements.toHashSet())
//    else
//        removeAll(emptyList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] collection.
 */
public fun <T> MutableCollection<in T>.retainAll(elements: Iterable<T>) {
    retainAll(elements.convertToSetForSetOperationWith(this))
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] array.
 */
public fun <T> MutableCollection<in T>.retainAll(elements: Array<out T>) {
    if (elements.isNotEmpty())
        retainAll(elements.toHashSet())
    else
        clear()
//        retainAll(emptyList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] sequence.
 */
public fun <T> MutableCollection<in T>.retainAll(elements: Sequence<T>) {
    val set = elements.toHashSet()
    if (set.isNotEmpty())
        retainAll(set)
    else
        clear()
}

/**
 * Sorts elements in the list in-place according to their natural sort order.
 * */
public fun <T: Comparable<T>> MutableList<T>.sort(): Unit {
    if (size > 1) java.util.Collections.sort(this)
}

/**
 *  Sorts elements in the list in-place according to order specified with [comparator].
 */
public fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) java.util.Collections.sort(this, comparator)
}
