/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A generic collection of elements. The interface allows iterating over contained elements
 * and checking whether something is contained within the collection. Complex operations are build upon this
 * functionality and provided in form of [kotlin.collections] extension functions.
 *
 * Functions in this interface support only read-only access to the collection;
 * read/write access is supported through the [MutableCollection] interface.
 *
 * [Collection] is a top-level interface for objects aggregating multiple different homogenous elements. Other more specific interfaces,
 * like [List], [Set], and [Map] extend [Collection] to provide more specific guarantees on how elements are stored and accessed, as well
 * as provide richer functionality.
 *
 * [Collection] implementation may have different guarantees on the order and uniqueness of contained elements,
 * for example, elements contained in a [List] are ordered and could contain duplicates, while elements contained in
 * a [Set] may not contain duplicates and there is no particular order imposed on them.
 *
 * [Collection.contains] behavior is implementation-specific, but usually, it uses [Any.equals] to compare elements
 * for equality.
 *
 * While it is not enforced explicitly, [Collection] implementations are expected to override [Any.toString],
 * [Any.equals] and [Any.hashCode]:
 * - [Collection.toString] implementations are expected to return a string representation of all contained elements.
 * - [Collection.equals] and [Collection.hashCode] should follow the contract of [Any.equals] and [Any.hashCode] correspondingly.
 *   Other than that, [Collection] does not impose additional restrictions on these functions, however more specialized interfaces
 *   extending [Collection] (like [List], [Set] and [Map]) may impose stricter requirements.
 *
 * @param E the type of elements contained in the collection. The collection is covariant in its element type.
 */
public actual interface Collection<out E> : Iterable<E> {
    // Query Operations
    /**
     * Returns the size of the collection.
     *
     * If a collection contains more than [Int.MAX_VALUE] elements, the value of this property is unspecified.
     * For implementations allowing to have more than [Int.MAX_VALUE] elements,
     * it is recommended to explicitly document behavior of this property.
     *
     * @sample samples.collections.Collections.Collections.collectionSize
     */
    public actual val size: Int

    /**
     * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
     *
     * @sample samples.collections.Collections.Collections.collectionIsEmpty
     */
    public actual fun isEmpty(): Boolean

    /**
     * Checks if the specified element is contained in this collection.
     *
     * @sample samples.collections.Collections.Collections.collectionContains
     */
    public actual operator fun contains(element: @UnsafeVariance E): Boolean

    actual override fun iterator(): Iterator<E>

    // Bulk Operations
    /**
     * Checks if all elements in the specified collection are contained in this collection.
     *
     * @sample samples.collections.Collections.Collections.collectionContainsAll
     */
    public actual fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

/**
 * A generic collection of elements that supports iterating, adding and removing elements, as well as checking if the
 * collection contains some elements. Complex operations are build upon this
 * functionality and provided in form of [kotlin.collections] extension functions.
 *
 * If a particular use case does not require collection's modification,
 * a read-only counterpart, [Collection] could be used instead.
 *
 * [MutableCollection] extends [Collection] contract with functions allowing to add or remove elements.
 *
 * [MutableCollection] is a top-level interface for mutable objects aggregating multiple different homogenous elements.
 * Other more specific interfaces, like [MutableList], [MutableSet], and [MutableMap] extend [MutableCollection] to provide
 * more specific guarantees on how elements are stored, accessed and modified, as well as provide richer functionality.
 *
 * Unlike [Collection], an iterator returned by [iterator] allows removing elements during iteration.
 *
 * Until stated otherwise, [MutableCollection] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * @param E the type of elements contained in the collection. The mutable collection is invariant in its element type.
 */
public actual interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    // Query Operations
    actual override fun iterator(): MutableIterator<E>

    // Modification Operations
    /**
     * Adds the specified element to the collection.
     *
     * @return `true` if the element has been added, `false` if the collection does not support duplicates
     * and the element is already contained in the collection.
     *
     * @sample samples.collections.Collections.Lists.add
     * @sample samples.collections.Collections.Sets.add
     */
    public actual fun add(element: E): Boolean

    /**
     * Removes a single instance of the specified element from this
     * collection, if the collection contains it.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not contained in the collection.
     *
     * @sample samples.collections.Collections.Lists.remove
     * @sample samples.collections.Collections.Sets.remove
     */
    public actual fun remove(element: E): Boolean

    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     *
     * @sample samples.collections.Collections.Lists.addAll
     * @sample samples.collections.Collections.Sets.addAll
     */
    public actual fun addAll(elements: Collection<E>): Boolean

    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     *
     * @sample samples.collections.Collections.Lists.removeAll
     * @sample samples.collections.Collections.Sets.removeAll
     */
    public actual fun removeAll(elements: Collection<E>): Boolean

    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     *
     * @sample samples.collections.Collections.Collections.retainAll
     */
    public actual fun retainAll(elements: Collection<E>): Boolean

    /**
     * Removes all elements from this collection.
     *
     * @sample samples.collections.Collections.Collections.clear
     */
    public actual fun clear(): Unit
}
