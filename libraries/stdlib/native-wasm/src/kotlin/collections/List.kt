/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A generic ordered collection of elements. The interface allows iterating over contained elements,
 * accessing elements by index, checking if a list contains some elements, and searching indices for particular values.
 * Complex operations are built upon this functionality and provided in form of [kotlin.collections] extension functions.
 *
 * Functions in this interface support only read-only access to the list;
 * read/write access is supported through the [MutableList] interface.
 *
 * In addition to a regular iteration, it is possible to obtain [ListIterator] using [listIterator] that provides
 * bidirectional iteration facilities, and allows accessing elements' indices in addition to their values.
 *
 * It is possible to get a view over a continuous span of elements using [subList].
 *
 * Unlike [Set], lists can contain duplicate elements.
 *
 * Unlike [Collection] implementations, [List] implementations must override [Any.toString], [Any.equals] and [Any.hashCode] functions
 * and provide implementations such that:
 * - [List.toString] should return a string containing string representation of contained elements in exact same order
 *   these elements are stored within the list.
 * - [List.equals] should consider two lists equal if and only if they contain the same number of elements and each element
 *   in one list is equal to an element in another list at the same index. Unlike some other `equals` implementations, [List.equals]
 *   should consider two lists equal even if they are instances of different classes; the only requirement here is that both lists have
 *   to implement [List] interface.
 * - [List.hashCode] should be computed as a combination of elements' hash codes using the following algorithm:
 *   ```kotlin
 *   var hashCode: Int = 1
 *   for (element in this) hashCode = hashCode * 31 + element.hashCode()
 *   ```
 *
 * @param E the type of elements contained in the list. The list is covariant in its element type.
 */
public actual interface List<out E> : Collection<E> {
    // Query Operations
    actual override val size: Int
    actual override fun isEmpty(): Boolean
    actual override fun contains(element: @UnsafeVariance E): Boolean
    actual override fun iterator(): Iterator<E>

    // Bulk Operations
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    // Positional Access Operations
    /**
     * Returns the element at the specified index in the list.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to [size] of this list.
     *
     * @sample samples.collections.Collections.Lists.get
     */
    public actual operator fun get(index: Int): E

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or `-1` if the specified
     * element is not contained in the list.
     *
     * For lists containing more than [Int.MAX_VALUE] elements, a result of this function is unspecified.
     *
     * @sample samples.collections.Collections.Lists.indexOf
     */
    public actual fun indexOf(element: @UnsafeVariance E): Int

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     *
     * For lists containing more than [Int.MAX_VALUE] elements, a result of this function is unspecified.
     *
     * @sample samples.collections.Collections.Lists.lastIndexOf
     */
    public actual fun lastIndexOf(element: @UnsafeVariance E): Int

    // List Iterators
    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     */
    public actual fun listIterator(): ListIterator<E>

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to [size] of this list.
     */
    public actual fun listIterator(index: Int): ListIterator<E>

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list,
     * and vice versa.
     *
     * Structural changes in the base list make the behavior of the view unspecified.
     *
     * @throws IndexOutOfBoundsException if [fromIndex] less than zero or [toIndex] greater than [size] of this list.
     * @throws IllegalArgumentException of [fromIndex] is greater than [toIndex].
     *
     * @sample samples.collections.Collections.Lists.subList
     */
    public actual fun subList(fromIndex: Int, toIndex: Int): List<E>
}

/**
 * A generic ordered collection of elements that supports adding, replacing and removing elements, as well as
 * iterating over contained elements, accessing them by an index and checking if a collection contains a particular value.
 *
 * If a particular use case does not require list's modification,
 * a read-only counterpart, [List] could be used instead.
 *
 * [MutableList] extends [List] contract with functions allowing to add, replace and remove elements.
 *
 * Unlike [List], iterators returned by [iterator] and [listIterator] allow modifying the list during iteration.
 * A view returned by [subList] also allows modifications of the underlying list.
 *
 * Until stated otherwise, [MutableList] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * @param E the type of elements contained in the list. The mutable list is invariant in its element type.
 */
public actual interface MutableList<E> : List<E>, MutableCollection<E> {
    // Modification Operations
    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     *
     * @sample samples.collections.Collections.Lists.add
     */
    actual override fun add(element: E): Boolean

    actual override fun remove(element: E): Boolean

    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to the end of this list.
     *
     * The elements are appended in the order they appear in the [elements] collection.
     *
     * @return `true` if the list was changed as the result of the operation.
     *
     * @sample samples.collections.Collections.Lists.addAll
     */
    actual override fun addAll(elements: Collection<E>): Boolean

    /**
     * Inserts all of the elements of the specified collection [elements] into this list at the specified [index].
     *
     * The elements are inserted in the order they appear in the [elements] collection.
     *
     * All elements that initially were stored at indices `index .. index + size - 1` are shifted `elements.size` positions to the end.
     *
     * If [index] is equal to [size], [elements] will be appended to the list.
     *
     * @return `true` if the list was changed as the result of the operation.
     *
     * @throws IndexOutOfBoundsException if [index] less than zero or greater than [size] of this list.
     *
     * @sample samples.collections.Collections.Lists.addAllAt
     */
    public actual fun addAll(index: Int, elements: Collection<E>): Boolean

    actual override fun removeAll(elements: Collection<E>): Boolean
    actual override fun retainAll(elements: Collection<E>): Boolean
    actual override fun clear(): Unit

    // Positional Access Operations
    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @return the element previously at the specified position.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to [size] of this list.
     *
     * @sample samples.collections.Collections.Lists.set
     */
    public actual operator fun set(index: Int, element: E): E

    /**
     * Inserts an element into the list at the specified [index].
     *
     * All elements that had indices `index .. index + size - 1` are shifted 1 position right.
     *
     * If [index] is equal to [size], [element] will be appended to this list.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than [size] of this list.
     *
     * @sample samples.collections.Collections.Lists.addAt
     */
    public actual fun add(index: Int, element: E): Unit

    /**
     * Removes an element at the specified [index] from the list.
     *
     * All elements placed after [index] are shifted 1 position left.
     *
     * @return the element that has been removed.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than or equal to [size] of this list.
     *
     * @sample samples.collections.Collections.Lists.removeAt
     */
    public actual fun removeAt(index: Int): E

    // List Iterators
    actual override fun listIterator(): MutableListIterator<E>

    actual override fun listIterator(index: Int): MutableListIterator<E>

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so changes in the returned list are reflected in this list, and vice-versa.
     *
     * Structural changes in the base list make the behavior of the view unspecified.
     *
     * @throws IndexOutOfBoundsException if [fromIndex] less than zero or [toIndex] greater than [size] of this list.
     * @throws IllegalArgumentException of [fromIndex] is greater than [toIndex].
     *
     * @sample samples.collections.Collections.Lists.subList
     */
    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}
