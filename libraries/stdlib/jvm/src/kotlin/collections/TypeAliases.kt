/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ACTUAL_WITHOUT_EXPECT") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.collections

/**
 * Marker interface indicating that the [List] implementation supports fast indexed access.
 */
@SinceKotlin("1.1") public actual typealias RandomAccess = java.util.RandomAccess


/**
 * A dynamic array implementation of [MutableList].
 *
 * This class stores elements contiguously in memory using an internal array that automatically
 * grows as needed. It fully implements the [MutableList] contract, providing all standard list
 * operations including indexed access, iteration, and modification. As an implementation of
 * [RandomAccess], it provides fast indexed access to elements.
 *
 * ## Performance characteristics
 *
 * [ArrayList] provides efficient implementation for common operations:
 *
 * - **Indexed access** ([get], [set]): O(1) constant time
 * - **Appending to the end** ([add]): O(1) [amortized](https://en.wikipedia.org/wiki/Amortized_analysis)
 *   constant time. When the internal array is full, it must be resized, which takes O(n) time to copy
 *   all existing elements to a new, larger array. However, these resize operations become less frequent
 *   as the list grows, making the average cost per appending constant over many operations.
 * - **Removing from the end** ([removeLast], [removeAt]`(size - 1)`): O(1) constant time
 * - **Inserting or removing at a position** ([add] with index, [removeAt]): O(n) linear time,
 *   as elements after the position must be shifted
 * - **Search operations** ([contains], [indexOf], [lastIndexOf]): O(n) linear time
 * - **Iteration**: O(n) linear time
 *
 * ## Usage guidelines
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple reallocations as the list grows.
 * - Use [ensureCapacity] before adding many elements to pre-allocate sufficient storage.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 * - Call [trimToSize] after all elements have been added to reduce memory consumption if no further
 *   growth is expected.
 *
 * ## Thread safety
 *
 * [ArrayList] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the list.
 */
@SinceKotlin("1.1") public actual typealias ArrayList<E> = java.util.ArrayList<E>

@SinceKotlin("1.1") public actual typealias LinkedHashMap<K, V> = java.util.LinkedHashMap<K, V>
@SinceKotlin("1.1") public actual typealias HashMap<K, V> = java.util.HashMap<K, V>
@SinceKotlin("1.1") public actual typealias LinkedHashSet<E> = java.util.LinkedHashSet<E>
@SinceKotlin("1.1") public actual typealias HashSet<E> = java.util.HashSet<E>
