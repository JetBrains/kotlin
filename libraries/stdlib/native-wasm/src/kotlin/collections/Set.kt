/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A generic unordered collection of unique elements. The interface allows checking if an element is contained by it
 * and iterating over all elements. Complex operations are built upon this functionality
 * and provided in form of [kotlin.collections] extension functions.
 *
 * It is implementation-specific how [Set] defines element's uniqueness. If not stated otherwise, [Set] implementations are usually
 * distinguishing elements using [Any.equals]. However, it is not the only way to distinguish elements, and some implementations may use
 * referential equality or compare elements by some of their properties. It is recommended to explicitly specify how a class
 * implementing [Set] distinguish elements.
 *
 * Methods in this interface support only read-only access to the set;
 * read/write access is supported through the [MutableSet] interface.
 *
 * Unlike [List], [Set] does not guarantee any particular order for iteration. However, particular implementations
 * are free to have fixed iteration order, like "smaller", in some sense, elements are visited prior to "larger". In this case,
 * it is recommended to explicitly document ordering guarantees for the [Set] implementation.
 *
 * As with [Collection], implementing [Any.toString], [Any.equals] and [Any.hashCode] is not enforced,
 * but [Set] implementations should override these functions and provide implementations such that:
 * - [Set.toString] should return a string containing string representation of contained elements in iteration order.
 * - [Set.equals] should consider two sets equal if and only if they contain the same number of elements and each element
 *   from one set is contained in another set. Unlike some other `equals` implementations, [Set.equals]
 *   should consider two sets equal even if they are instances of different classes; the only requirement here is that both sets have
 *   to implement [Set] interface.
 * - [Set.hashCode] should be computed as a sum of elements' hash codes using the following algorithm:
 *   ```kotlin
 *   var hashCode: Int = 0
 *   for (element in this) hashCode += element.hashCode()
 *   ```
 *
 * @param E the type of elements contained in the set. The set is covariant in its element type.
 */
public actual interface Set<out E> : Collection<E> {
    // Query Operations
    actual override val size: Int

    actual override fun isEmpty(): Boolean
    actual override fun contains(element: @UnsafeVariance E): Boolean
    actual override fun iterator(): Iterator<E>

    // Bulk Operations
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

/**
 * A generic unordered collection of unique elements that supports adding and removing elements, iterating over them
 * and checking if a collection contains a particular value.
 *
 * If a particular use case does not require set's modification,
 * a read-only counterpart, [Set] could be used instead.
 *
 * [MutableSet] extends [Set] contact with functions allowing to add and remove elements.
 *
 * Unlike [Set], an iterator returned by [iterator] allows modifying the set during iteration.
 *
 * Until stated otherwise, [MutableSet] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * @param E the type of elements contained in the set. The mutable set is invariant in its element type.
 */
public actual interface MutableSet<E> : Set<E>, MutableCollection<E> {
    // Query Operations
    actual override fun iterator(): MutableIterator<E>

    // Modification Operations

    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     *
     * @sample samples.collections.Collections.Sets.add
     */
    actual override fun add(element: E): Boolean

    actual override fun remove(element: E): Boolean

    // Bulk Modification Operations
    actual override fun addAll(elements: Collection<E>): Boolean

    actual override fun removeAll(elements: Collection<E>): Boolean
    actual override fun retainAll(elements: Collection<E>): Boolean
    actual override fun clear(): Unit
}
