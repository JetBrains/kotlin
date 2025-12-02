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

/**
 * A hash table implementation of [MutableMap].
 *
 * This class stores key-value pairs using a hash table data structure that provides fast lookups
 * based on keys. It fully implements the [MutableMap] contract, providing all standard map operations
 * including insertion, removal, and lookup of values by key.
 *
 * ## Null keys and values
 *
 * [HashMap] accepts `null` as a key. Since keys are unique, at most one entry with a `null` key
 * can exist in the map. [HashMap] also accepts `null` as a value, and multiple entries can have
 * `null` values.
 *
 * ## Key's hash code and equality contracts
 *
 * [HashMap] relies on the [Any.hashCode] and [Any.equals] functions of keys to organize and locate entries.
 * Keys are considered equal if their [Any.equals] function returns `true`, and keys that are equal must
 * have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the key objects. Modifying a key object in a way that changes its hash code or equality
 * after it has been used as a key in a [HashMap] may lead to the entry becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of keys distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [HashMap] provides efficient implementation for common operations:
 *
 * - **Lookup** ([get], [containsKey]): O(1) time
 * - **Insertion and removal** ([put], [remove]): O(1) time
 * - **Value search** ([containsValue]): O(n) time, requires scanning all entries
 * - **Iteration** ([entries], [keys], [values]): O(n) time
 *
 * ## Iteration order
 *
 * [HashMap] does not guarantee any particular order for iteration over its keys, values, or entries.
 * The iteration order is unpredictable and may change when the map is rehashed (when entries are
 * added or removed and the internal capacity is adjusted). Do not rely on any specific iteration order.
 *
 * If a predictable iteration order is required, consider using [LinkedHashMap], which maintains
 * insertion order.
 *
 * ## Usage guidelines
 *
 * [HashMap] uses an internal data structure with a finite *capacity* - the maximum number of entries
 * it can store before needing to grow. As entries are added, the map tracks its *load factor*, which is
 * the ratio of the number of entries to the current capacity. When this ratio exceeds a certain threshold,
 * the map automatically increases its capacity and performs *rehashing* - rebuilding the internal data
 * structure to redistribute entries. Rehashing is a relatively expensive operation that temporarily impacts
 * performance. When creating a [HashMap], you can optionally provide values for the initial capacity and
 * load factor threshold.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of entries is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the map grows.
 * - Choose an appropriate load factor when creating the map. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure key objects have well-distributed [Any.hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [putAll] over multiple individual [put] calls when adding multiple entries.
 *
 * ## Thread safety
 *
 * [HashMap] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
@SinceKotlin("1.1") public actual typealias HashMap<K, V> = java.util.HashMap<K, V>

@SinceKotlin("1.1") public actual typealias LinkedHashSet<E> = java.util.LinkedHashSet<E>

/**
 * A hash table implementation of [MutableSet].
 *
 * This class stores unique elements using a hash table data structure that provides fast lookups
 * and ensures no duplicate elements are stored. It fully implements the [MutableSet] contract,
 * providing all standard Set operations including lookup, insertion, and removal.
 *
 * ## Null elements
 *
 * [HashSet] accepts `null` as an element. Since elements are unique, at most one `null` element
 * can exist in the set.
 *
 * ## Element's hash code and equality contracts
 *
 * [HashSet] relies on the [Any.hashCode] and [Any.equals] functions of elements to organize and locate them.
 * Elements are considered equal if their [Any.equals] function returns `true`, and elements that are equal
 * must have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior, such as
 * duplicate elements being stored or elements becoming unreachable.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the element objects. Modifying an element in a way that changes its hash code or equality
 * after it has been added to a [HashSet] may lead to the element becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of elements distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [HashSet] provides efficient implementation for common operations:
 *
 * - **Lookup** ([contains]): O(1) time
 * - **Insertion and removal** ([add], [remove]): O(1) time
 * - **Iteration**: O(n) time
 *
 * ## Iteration order
 *
 * [HashSet] does not guarantee any particular order for iteration over its elements.
 * The iteration order is unpredictable and may change when the set is rehashed (when elements are
 * added or removed and the internal capacity is adjusted). Do not rely on any specific iteration order.
 *
 * If a predictable iteration order is required, consider using [LinkedHashSet], which maintains
 * insertion order.
 *
 * ## Usage guidelines
 *
 * [HashSet] uses an internal data structure with a finite *capacity* - the maximum number of elements
 * it can store before needing to grow. As elements are added, the set tracks its *load factor*, which is
 * the ratio of the number of elements to the current capacity. When this ratio exceeds a certain threshold,
 * the set automatically increases its capacity and performs *rehashing* - rebuilding the internal data
 * structure to redistribute elements. Rehashing is a relatively expensive operation that temporarily impacts
 * performance. When creating a [HashSet], you can optionally provide values for the initial capacity and
 * load factor threshold.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the set grows.
 * - Choose an appropriate load factor when creating the set. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure element objects have well-distributed [Any.hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 *
 * ## Thread safety
 *
 * [HashSet] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the set. The mutable set is invariant in its element type.
 */
@SinceKotlin("1.1") public actual typealias HashSet<E> = java.util.HashSet<E>
