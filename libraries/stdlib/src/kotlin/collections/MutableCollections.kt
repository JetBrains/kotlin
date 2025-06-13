/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

/**
 * Removes a single instance of the specified element from this
 * collection, if it is present.
 *
 * Allows to overcome type-safety restriction of `remove` that requires to pass an element of type `E`.
 *
 * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
 */
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public inline fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.remove(element: T): Boolean =
    @Suppress("UNCHECKED_CAST") (this as MutableCollection<T>).remove(element)

/**
 * Removes all of this collection's elements that are also contained in the specified collection.

 * Allows to overcome type-safety restriction of `removeAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
 */
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public inline fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.removeAll(elements: Collection<T>): Boolean =
    @Suppress("UNCHECKED_CAST") (this as MutableCollection<T>).removeAll(elements)

/**
 * Retains only the elements in this collection that are contained in the specified collection.
 *
 * Allows to overcome type-safety restriction of `retainAll` that requires to pass a collection of type `Collection<E>`.
 *
 * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
 */
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public inline fun <@kotlin.internal.OnlyInputTypes T> MutableCollection<out T>.retainAll(elements: Collection<T>): Boolean =
    @Suppress("UNCHECKED_CAST") (this as MutableCollection<T>).retainAll(elements)

/**
 * Adds the specified [element] to this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.plusAssign(element: T) {
    this.add(element)
}

/**
 * Adds all elements of the given [elements] collection to this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.plusAssign(elements: Iterable<T>) {
    this.addAll(elements)
}

/**
 * Adds all elements of the given [elements] array to this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.plusAssign(elements: Array<T>) {
    this.addAll(elements)
}

/**
 * Adds all elements of the given [elements] sequence to this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.plusAssign(elements: Sequence<T>) {
    this.addAll(elements)
}

/**
 * Removes a single instance of the specified [element] from this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.minusAssign(element: T) {
    this.remove(element)
}

/**
 * Removes all elements contained in the given [elements] collection from this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.minusAssign(elements: Iterable<T>) {
    this.removeAll(elements)
}

/**
 * Removes all elements contained in the given [elements] array from this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.minusAssign(elements: Array<T>) {
    this.removeAll(elements)
}

/**
 * Removes all elements contained in the given [elements] sequence from this mutable collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> MutableCollection<in T>.minusAssign(elements: Sequence<T>) {
    this.removeAll(elements)
}

/**
 * Adds all elements of the given [elements] collection to this [MutableCollection].
 */
@IgnorableReturnValue
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

/**
 * Adds all elements of the given [elements] sequence to this [MutableCollection].
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.addAll(elements: Sequence<T>): Boolean {
    var result: Boolean = false
    for (item in elements) {
        if (add(item)) result = true
    }
    return result
}

/**
 * Adds all elements of the given [elements] array to this [MutableCollection].
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.addAll(elements: Array<out T>): Boolean {
    return addAll(elements.asList())
}

/**
 * Converts this [Iterable] to a list if it is not a [Collection].
 * Otherwise, returns this.
 */
internal fun <T> Iterable<T>.convertToListIfNotCollection(): Collection<T> =
    if (this is Collection) this else toList()

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] collection.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.removeAll(elements: Iterable<T>): Boolean {
    return removeAll(elements.convertToListIfNotCollection())
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] sequence.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.removeAll(elements: Sequence<T>): Boolean {
    val list = elements.toList()
    return list.isNotEmpty() && removeAll(list)
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] array.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.removeAll(elements: Array<out T>): Boolean {
    return elements.isNotEmpty() && removeAll(elements.asList())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] collection.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.retainAll(elements: Iterable<T>): Boolean {
    return retainAll(elements.convertToListIfNotCollection())
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] array.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.retainAll(elements: Array<out T>): Boolean {
    if (elements.isNotEmpty())
        return retainAll(elements.asList())
    else
        return retainNothing()
}

/**
 * Retains only elements of this [MutableCollection] that are contained in the given [elements] sequence.
 */
@IgnorableReturnValue
public fun <T> MutableCollection<in T>.retainAll(elements: Sequence<T>): Boolean {
    val list = elements.toList()
    if (list.isNotEmpty())
        return retainAll(list)
    else
        return retainNothing()
}

@IgnorableReturnValue
private fun MutableCollection<*>.retainNothing(): Boolean {
    val result = isNotEmpty()
    clear()
    return result
}


/**
 * Removes all elements from this [MutableIterable] that match the given [predicate].
 *
 * @return `true` if any element was removed from this collection, or `false` when no elements were removed and collection was not modified.
 */
@IgnorableReturnValue
public fun <T> MutableIterable<T>.removeAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, true)

/**
 * Retains only elements of this [MutableIterable] that match the given [predicate].
 *
 * @return `true` if any element was removed from this collection, or `false` when all elements were retained and collection was not modified.
 */
@IgnorableReturnValue
public fun <T> MutableIterable<T>.retainAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, false)

private fun <T> MutableIterable<T>.filterInPlace(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    var result = false
    with(iterator()) {
        while (hasNext())
            if (predicate(next()) == predicateResultToRemove) {
                remove()
                result = true
            }
    }
    return result
}


/**
 * Removes the element at the specified [index] from this list.
 * In Kotlin one should use the [MutableList.removeAt] function instead.
 */
@Deprecated("Use removeAt(index) instead.", ReplaceWith("removeAt(index)"), level = DeprecationLevel.ERROR)
@kotlin.internal.InlineOnly
public inline fun <T> MutableList<T>.remove(index: Int): T = removeAt(index)

/**
 * Removes the first element from this mutable list and returns that removed element, or throws [NoSuchElementException] if this list is empty.
 */
@SinceKotlin("1.4")
@IgnorableReturnValue
public fun <T> MutableList<T>.removeFirst(): T = if (isEmpty()) throw NoSuchElementException("List is empty.") else removeAt(0)

/**
 * Removes the first element from this mutable list and returns that removed element, or returns `null` if this list is empty.
 */
@SinceKotlin("1.4")
@IgnorableReturnValue
public fun <T> MutableList<T>.removeFirstOrNull(): T? = if (isEmpty()) null else removeAt(0)

/**
 * Removes the last element from this mutable list and returns that removed element, or throws [NoSuchElementException] if this list is empty.
 */
@SinceKotlin("1.4")
@IgnorableReturnValue
public fun <T> MutableList<T>.removeLast(): T = if (isEmpty()) throw NoSuchElementException("List is empty.") else removeAt(lastIndex)

/**
 * Removes the last element from this mutable list and returns that removed element, or returns `null` if this list is empty.
 */
@SinceKotlin("1.4")
@IgnorableReturnValue
public fun <T> MutableList<T>.removeLastOrNull(): T? = if (isEmpty()) null else removeAt(lastIndex)

/**
 * Removes all elements from this [MutableList] that match the given [predicate].
 *
 * @return `true` if any element was removed from this collection, or `false` when no elements were removed and collection was not modified.
 */
@IgnorableReturnValue
public fun <T> MutableList<T>.removeAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, true)

/**
 * Retains only elements of this [MutableList] that match the given [predicate].
 *
 * @return `true` if any element was removed from this collection, or `false` when all elements were retained and collection was not modified.
 */
@IgnorableReturnValue
public fun <T> MutableList<T>.retainAll(predicate: (T) -> Boolean): Boolean = filterInPlace(predicate, false)

private fun <T> MutableList<T>.filterInPlace(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    if (this !is RandomAccess)
        return (this as MutableIterable<T>).filterInPlace(predicate, predicateResultToRemove)

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
    } else {
        return false
    }
}
