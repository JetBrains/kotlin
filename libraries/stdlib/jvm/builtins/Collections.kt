/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin
@file:kotlin.internal.SuppressBytecodeGeneration

package kotlin.collections

import kotlin.internal.PlatformDependent

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over.
 * @param T the type of element being iterated over. The iterator is covariant in its element type.
 */
public actual interface Iterable<out T> {
    /**
     * Returns an iterator over the elements of this object.
     */
    public actual operator fun iterator(): Iterator<T>
}

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over and that supports removing elements during iteration.
 * @param T the type of element being iterated over. The mutable iterator is invariant in its element type.
 */
public actual interface MutableIterable<out T> : Iterable<T> {
    /**
     * Returns an iterator over the elements of this sequence that supports removing elements during iteration.
     */
    actual override fun iterator(): MutableIterator<T>
}

/**
 * A generic collection of elements. The interface allows iterating over contained elements
 * and checking whether something is contained within the collection. Complex operations are built upon this
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
 * [Collection] does not impose any requirements for [toString], [equals] and [hashCode] functions
 * and implementations are free to inherit a default behavior.
 * More specialized interfaces extending [Collection] (like [List], [Set] and [Map]) may impose stricter requirements.
 *
 * By convention, [Collection] implementations are usually also implement [java.io.Serializable].
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
 * collection contains some elements. Complex operations are built upon this
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
 * By convention, [MutableCollection] implementations are usually also implement [java.io.Serializable].
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
 *   in one list is equal to an element in another list at the same index.
 * - [List.hashCode] should be computed as a combination of elements' hash codes using the following algorithm:
 *   ```kotlin
 *   var hashCode: Int = 1
 *   for (element in this) hashCode = hashCode * 31 + element?.hashCode() ?: 0
 *   ```
 *
 * By convention, [List] implementations are usually also implement [java.io.Serializable].
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
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list.
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
 * A view returned by [subList] is also allows modifications of the underlying list.
 *
 * Until stated otherwise, [MutableList] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * By convention, [MutableList] implementations are also implement [java.io.Serializable].
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
     * All elements that initially were stored at indices `index .. index + size - 1` are shifted `elements.size` positions right.
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
 * are free to have fixed iteration order, like "smaller", in some sense, elements are visited prior "larger". In this case,
 * it is recommended to explicitly document ordering guarantees for the [Set] implementation.
 *
 * Unlike [Collection] implementations, [Set] implementations must override [Any.toString], [Any.equals] and [Any.hashCode] functions
 * and provide implementations such that:
 * - [Set.toString] should return a string containing string representation of contained elements in iteration order.
 * - [Set.equals] should consider two sets equal if and only if they contain the same number of elements and each element
 *   from one set is contained in another set.
 * - [Set.hashCode] should be computed as a sum of elements' hash codes using the following algorithm:
 *   ```kotlin
 *   var hashCode: Int = 0
 *   for (element in this) hashCode += element?.hashCode() ?: 0
 *   ```
 *
 * By convention, [Set] implementations are usually also implement [java.io.Serializable].
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
 * By convention, [MutableSet] implementations are usually also implement [java.io.Serializable].
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

/**
 * A collection that holds pairs of objects (keys and values) and supports retrieving the value corresponding to each key,
 * checking if a collection holds a particular key or a value. Maps also allow iterating over keys, values or key-value pairs (entries).
 * Complex operations are built upon this functionality and provided in form of [kotlin.collections] extension functions.
 *
 * Map keys are unique; the map holds only one value for each key. In contrast, values may be duplicated; there might be several
 * unique keys associated with the same value.
 *
 * It is implementation-specific how [Map] defines key's uniqueness. If not stated otherwise, [Map] implementations are usually
 * distinguishing elements using [Any.equals]. However, it is not the only way to distinguish elements, and some implementations may use
 * referential equality or compare elements by some of their properties. It is recommended to explicitly specify how a class
 * implementing [Map] distinguish elements.
 *
 * It is also implementation-specific how [Map] handles `null` keys and values: some [Map] implementations may support them, while
 * other may not. It is recommended to explicitly define key/value nullability policy when implementing [Map].
 *
 * Unlike [Collection] implementations, [Map] implementations must override [Any.toString], [Any.equals] and [Any.hashCode] functions
 * and provide implementations such that:
 * - [Map.toString] should return a string containing string representation of contained key-value pairs in iteration order.
 * - [Map.equals] should consider two maps equal if and only if they contain the same keys and values associated with these keys
 *   are equal.
 * - [Map.hashCode] should be computed as a sum of [Entry] hash codes, and entry's hash code should be computed as exclusive or (XOR) of
 *   hash codes corresponding to a key and a value:
 *   ```kotlin
 *   var hashCode: Int = 0
 *   for ((k, v) in entries) hashCode += (k?.hashCode() ?: 0) ^ (v?.hashCode() ?: 0)
 *   ```
 *
 * Functions in this interface support only read-only access to the map; read-write access is supported through
 * the [MutableMap] interface.
 *
 * By convention, [Map] implementations are usually also implement [java.io.Serializable].
 *
 * @param K the type of map keys. The map is invariant in its key type, as it
 *          can accept a key as a parameter (of [containsKey] for example) and return it in a [keys] set.
 * @param V the type of map values. The map is covariant in its value type.
 */
public actual interface Map<K, out V> {
    // Query Operations
    /**
     * Returns the number of key/value pairs in the map.
     *
     * If a map contains more than [Int.MAX_VALUE] elements, the value of this property is unspecified.
     * For implementations allowing to have more than [Int.MAX_VALUE] elements,
     * it is recommended to explicitly document behavior of this property.
     *
     * @sample samples.collections.Maps.CoreApi.size
     */
    public actual val size: Int

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     *
     * @sample samples.collections.Maps.CoreApi.isEmpty
     */
    public actual fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [key].
     *
     * @sample samples.collections.Maps.CoreApi.containsKey
     */
    public actual fun containsKey(key: K): Boolean

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     *
     * @sample samples.collections.Maps.CoreApi.containsValue
     */
    public actual fun containsValue(value: @UnsafeVariance V): Boolean

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     *
     * Note that for maps supporting `null` values, `null` value associated with the [key] is indistinguishable from the missing [key],
     * so [containsKey] should be used to check if the map actually contains the [key].
     *
     * @sample samples.collections.Maps.CoreApi.get
     */
    public actual operator fun get(key: K): V?

    /**
     * Returns the value corresponding to the given [key], or [defaultValue] if such a key is not present in the map.
     *
     * @since JDK 1.8
     */
    @SinceKotlin("1.1")
    @PlatformDependent
    public fun getOrDefault(key: K, defaultValue: @UnsafeVariance V): V {
        // See default implementation in JDK sources
        throw NotImplementedError()
    }

    // Views
    /**
     * Returns a read-only [Set] of all keys in this map.
     *
     * @sample samples.collections.Maps.CoreApi.keySet
     */
    public actual val keys: Set<K>

    /**
     * Returns a read-only [Collection] of all values in this map. Note that this collection may contain duplicate values.
     *
     * @sample samples.collections.Maps.CoreApi.valueSet
     */
    public actual val values: Collection<V>

    /**
     * Returns a read-only [Set] of all key/value pairs in this map.
     *
     * @sample samples.collections.Maps.CoreApi.entrySet
     */
    public actual val entries: Set<Map.Entry<K, V>>

    /**
     * Represents a key/value pair held by a [Map].
     *
     * Map entries are not supposed to be stored separately or used long after they are obtained.
     * The behavior of an entry is unspecified if the backing map has been modified after the entry was obtained.
     */
    public actual interface Entry<out K, out V> {
        /**
         * Returns the key of this key/value pair.
         */
        public actual val key: K

        /**
         * Returns the value of this key/value pair.
         */
        public actual val value: V
    }
}

/**
 * A collection that holds pairs of objects (keys and values) and supports retrieving
 * the value corresponding to each key, as well as adding new, removing or updating existing pairs.
 *
 * Map keys are unique; the map holds only one value for each key. In contrast, values may be duplicated; there might be several
 * unique keys associated with the same value.
 *
 * If a particular use case does not require map's modification, a read-only counterpart, [Map] could be used instead.
 *
 * [MutableMap] extends [Map] contact with functions allowing to add, remove and update mapping between keys and values.
 *
 * Unlike [Map], iterators returned by [keys], [values] and [entries] are all mutable and let updating the map during the iteration.
 *
 * Until stated otherwise, [MutableMap] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * By convention, [MutableMap] implementations are usually also implement [java.io.Serializable].
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
public actual interface MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * @sample samples.collections.Maps.CoreApi.put
     */
    public actual fun put(key: K, value: V): V?

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * @sample samples.collections.Maps.CoreApi.remove
     */
    public actual fun remove(key: K): V?

    /**
     * Removes the entry for the specified key only if it is mapped to the specified value.
     *
     * @return true if entry was removed
     */
    @SinceKotlin("1.1")
    @PlatformDependent
    public fun remove(key: K, value: V): Boolean {
        // See default implementation in JDK sources
        return true
    }

    // Bulk Modification Operations
    /**
     * Updates this map with key/value pairs from the specified map [from].
     *
     * @sample samples.collections.Maps.CoreApi.putAll
     */
    public actual fun putAll(from: Map<out K, V>): Unit

    /**
     * Removes all elements from this map.
     *
     * @sample samples.collections.Maps.CoreApi.clear
     */
    public actual fun clear(): Unit

    // Views
    /**
     * Returns a [MutableSet] of all keys in this map.
     *
     * @sample samples.collections.Maps.CoreApi.keySetMutable
     */
    actual override val keys: MutableSet<K>

    /**
     * Returns a [MutableCollection] of all values in this map. Note that this collection may contain duplicate values.
     *
     * @sample samples.collections.Maps.CoreApi.valueSetMutable
     */
    actual override val values: MutableCollection<V>

    /**
     * Returns a [MutableSet] of all key/value pairs in this map.
     *
     * @sample samples.collections.Maps.CoreApi.entrySetMutable
     */
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    /**
     * Represents a key/value pair held by a [MutableMap].
     *
     * Map entries are not supposed to be stored separately or used long after they are obtained.
     * The behavior of an entry is unspecified if the backing map has been modified after the entry was obtained.
     */
    public actual interface MutableEntry<K, V> : Map.Entry<K, V> {
        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
        public actual fun setValue(newValue: V): V
    }
}
