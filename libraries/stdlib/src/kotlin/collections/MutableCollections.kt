@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

import java.util.*

/**
 * Checks if all elements in the specified collection are contained in this collection.
 *
 * Allows to overcome type-safety restriction of `containsAll` that requires to pass a collection of type `Collection<E>`.
 */
public fun <@kotlin.internal.OnlyInputTypes T> Collection<T>.containsAll(elements: Collection<T>): Boolean = this.containsAll(elements)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Collections have incompatible types. Upcast either to Collection<Any?> if you're sure.", ReplaceWith("containsAll<Any?>(elements)"))
public inline fun Collection<*>.containsAllRaw(elements: Collection<Any?>): Boolean = containsAll(elements)

@Deprecated("Collections have incompatible types. Upcast either to Collection<Any?> if you're sure.", ReplaceWith("containsAll<Any?>(elements)"))
@kotlin.jvm.JvmName("containsAllOfAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <E> Collection<E>.containsAll(elements: Collection<Any?>): Boolean = containsAll(elements)

/**
 * Removes a single instance of the specified element from this
 * collection, if it is present.
 *
 * Allows to overcome type-safety restriction of `remove` that requires to pass an element of type `E`.
 *
 * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
 */
public fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.remove(element: T): Boolean = (this as MutableCollection<T>).remove(element)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Collection and element have incompatible types. Upcast element to Any? if you're sure.", ReplaceWith("remove(element as Any?)"))
public inline fun <E> MutableCollection<E>.removeRaw(element: Any?): Boolean = remove(element)

@Deprecated("Collection and element have incompatible types. Upcast element to Any? if you're sure.", ReplaceWith("remove(element as T)"))
@kotlin.jvm.JvmName("removeAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <T> MutableCollection<out T>.remove(element: T): Boolean = remove(element)

/**
 * Removes all of this collection's elements that are also contained in the specified collection.

 * Allows to overcome type-safety restriction of `removeAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
 */
public fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.removeAll(elements: Collection<T>): Boolean = (this as MutableCollection<T>).removeAll(elements)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Collections have incompatible types. Upcast elements to Collection<Any?> if you're sure.", ReplaceWith("removeAll<Any?>(elements)"))
public inline fun <E> MutableCollection<E>.removeAllRaw(elements: Collection<Any?>): Boolean = removeAll(elements)

@Deprecated("Collections have incompatible types. Upcast elements to Collection<Any?> if you're sure.", ReplaceWith("removeAll<Any?>(elements)"))
@kotlin.jvm.JvmName("removeAllOfAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <E> MutableCollection<E>.removeAll(elements: Collection<Any?>): Boolean = removeAll(elements)

/**
 * Retains only the elements in this collection that are contained in the specified collection.
 *
 * Allows to overcome type-safety restriction of `retailAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
 */
public fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.retainAll(elements: Collection<T>): Boolean = (this as MutableCollection<T>).retainAll(elements)

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Collections have incompatible types. Upcast elements to Collection<Any?> if you're sure.", ReplaceWith("retainAll<Any?>(elements)"))
public inline fun <E> MutableCollection<E>.retainAllRaw(elements: Collection<Any?>): Boolean = retainAll(elements)

@Deprecated("Collections have incompatible types. Upcast elements to Collection<Any?> if you're sure.", ReplaceWith("retainAll<Any?>(elements)"))
@kotlin.jvm.JvmName("retainAllOfAny")
@kotlin.internal.LowPriorityInOverloadResolution
public fun <E> MutableCollection<E>.retainAll(elements: Collection<Any?>): Boolean = retainAll(elements as Collection<Any?>)


@Deprecated("Use operator 'get' instead", ReplaceWith("this[index]"))
public fun CharSequence.charAt(index: Int): Char = this[index]

@Deprecated("Use property 'size' instead", ReplaceWith("size"))
public inline fun Collection<*>.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("size"))
public inline fun Map<*, *>.size() = size

@Deprecated("Use property 'key' instead", ReplaceWith("key"))
public fun <K, V> Map.Entry<K, V>.getKey(): K = key


@Deprecated("Use property 'value' instead.", ReplaceWith("value"))
public fun <K, V> Map.Entry<K, V>.getValue(): V = value

@Deprecated("Use 'removeAt' instead.", ReplaceWith("removeAt(index)"))
public fun <E> MutableList<E>.remove(index: Int): E = removeAt(index)




@Deprecated("Use property 'length' instead.", ReplaceWith("length"))
public fun CharSequence.length(): Int = length

@Deprecated("Map and key have incompatible types. Upcast key to Any? if you're sure.", ReplaceWith("get(key as K)"))
@kotlin.internal.LowPriorityInOverloadResolution
public inline operator fun <K, V> Map<out K, V>.get(key: K): V? = get(key)

@Deprecated("Map and key have incompatible types. Upcast key to Any? if you're sure.", ReplaceWith("containsKey(key as K)"))
@kotlin.internal.LowPriorityInOverloadResolution
public inline fun <K, V> Map<out K, V>.containsKey(key: K): Boolean = containsKey(key)

@Deprecated("Map and value have incompatible types. Upcast value to Any? if you're sure.", ReplaceWith("containsValue(value as V)"))
@kotlin.internal.LowPriorityInOverloadResolution
public inline fun <K, V> Map<K, V>.containsValue(value: V): Boolean = containsValue(value as Any?)

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
public fun <T> MutableCollection<in T>.addAll(elements: Iterable<T>): Boolean {
    when (elements) {
        is Collection -> return addAll(elements)
        else -> {
            var result: Boolean = false
            for (item in elements)
                if (add(item)) result = true
            return result
        }
    }
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.addAll(elements: Iterable<T>) {
    when (elements) {
        is Collection -> addAll(elements)
        else -> for (item in elements) add(item)
    }
}

/**
 * Adds all elements of the given [elements] sequence to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(elements: Sequence<T>): Boolean {
    var result: Boolean = false
    for (item in elements) {
        if (add(item)) result = true
    }
    return result
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.addAll(elements: Sequence<T>) {
    for (item in elements) add(item)
}

/**
 * Adds all elements of the given [elements] array to this [MutableCollection].
 */
public fun <T> MutableCollection<in T>.addAll(elements: Array<out T>): Boolean {
    return addAll(elements.asList())
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.addAll(elements: Array<out T>) {
    addAll(elements.asList())
}

/**
 * Removes all elements from this [MutableIterable] that match the given [predicate].
 */
public fun <T> MutableIterable<T>.removeAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, true)

/**
 * Retains only elements of this [MutableIterable] that match the given [predicate].
 */
public fun <T> MutableIterable<T>.retainAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, false)

private fun <T> MutableIterable<T>.filterInPlace(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    var result = false
    with (iterator()) {
        while (hasNext())
            if (predicate(next()) == predicateResultToRemove) {
                remove()
                result = true
            }
    }
    return result
}

/**
 * Removes all elements from this [MutableList] that match the given [predicate].
 */
public fun <T> MutableList<T>.removeAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, true)

/**
 * Retains only elements of this [MutableList] that match the given [predicate].
 */
public fun <T> MutableList<T>.retainAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, false)

private fun <T> MutableList<T>.filterInPlace(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    var writeIndex: Int = 0
    for (readIndex in 0..lastIndex) {
        val element = this[readIndex]
        if (predicate(element) == predicateResultToRemove)
            continue

        if (writeIndex != readIndex)
            this[writeIndex] = element

        writeIndex++
    }
    if (writeIndex < size) {
        for (removeIndex in lastIndex downTo writeIndex)
            removeAt(removeIndex)

        return true
    }
    else {
        return false
    }
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] collection.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Iterable<T>): Boolean {
    return removeAll(elements.convertToSetForSetOperationWith(this))
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.removeAll(elements: Iterable<T>) {
    removeAll(elements.convertToSetForSetOperationWith(this))
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] sequence.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Sequence<T>): Boolean {
    val set = elements.toHashSet()
    return set.isNotEmpty() && removeAll(set)
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.removeAll(elements: Sequence<T>) {
    val set = elements.toHashSet()
    if (set.isNotEmpty())
        removeAll(set)
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] array.
 */
public fun <T> MutableCollection<in T>.removeAll(elements: Array<out T>): Boolean {
    return elements.isNotEmpty() && removeAll(elements.toHashSet())
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.removeAll(elements: Array<out T>) {
    if (elements.isNotEmpty())
        removeAll(elements.toHashSet())
//    else
//        removeAll(emptyList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] collection.
 */
public fun <T> MutableCollection<in T>.retainAll(elements: Iterable<T>): Boolean {
    return retainAll(elements.convertToSetForSetOperationWith(this))
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.retainAll(elements: Iterable<T>) {
    retainAll(elements.convertToSetForSetOperationWith(this))
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] array.
 */
public fun <T> MutableCollection<in T>.retainAll(elements: Array<out T>): Boolean {
    if (elements.isNotEmpty())
        return retainAll(elements.toHashSet())
    else
        return retainNothing()
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
public fun <T> MutableCollection<in T>.retainAll(elements: Sequence<T>): Boolean {
    val set = elements.toHashSet()
    if (set.isNotEmpty())
        return retainAll(set)
    else
        return retainNothing()
}

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T> MutableCollection<in T>.retainAll(elements: Sequence<T>) {
    val set = elements.toHashSet()
    if (set.isNotEmpty())
        retainAll(set)
    else
        clear()
}

private fun MutableCollection<*>.retainNothing(): Boolean {
    val result = isNotEmpty()
    clear()
    return result
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
