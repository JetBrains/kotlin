/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)
/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.collections

import kotlin.js.collections.*

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over.
 * @param T the type of element being iterated over. The iterator is covariant in its element type.
 */
public interface Iterable<out T> {
    /**
     * Returns an iterator over the elements of this object.
     */
    public operator fun iterator(): Iterator<T>
}

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over and that supports removing elements during iteration.
 * @param T the type of element being iterated over. The mutable iterator is invariant in its element type.
 */
public interface MutableIterable<out T> : Iterable<T> {
    /**
     * Returns an iterator over the elements of this sequence that supports removing elements during iteration.
     */
    override fun iterator(): MutableIterator<T>
}

/**
 * A generic collection of elements. Methods in this interface support only read-only access to the collection;
 * read/write access is supported through the [MutableCollection] interface.
 * @param E the type of elements contained in the collection. The collection is covariant in its element type.
 */
public interface Collection<out E> : Iterable<E> {
    // Query Operations
    /**
     * Returns the size of the collection.
     */
    public val size: Int

    /**
     * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Checks if the specified element is contained in this collection.
     */
    public operator fun contains(element: @UnsafeVariance E): Boolean

    override fun iterator(): Iterator<E>

    // Bulk Operations
    /**
     * Checks if all elements in the specified collection are contained in this collection.
     */
    public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

/**
 * A generic collection of elements that supports adding and removing elements.
 *
 * @param E the type of elements contained in the collection. The mutable collection is invariant in its element type.
 */
public interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    // Query Operations
    override fun iterator(): MutableIterator<E>

    // Modification Operations
    /**
     * Adds the specified element to the collection.
     *
     * @return `true` if the element has been added, `false` if the collection does not support duplicates
     * and the element is already contained in the collection.
     */
    public fun add(element: E): Boolean

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    public fun remove(element: E): Boolean

    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    public fun addAll(elements: Collection<E>): Boolean

    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    public fun removeAll(elements: Collection<E>): Boolean

    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    public fun retainAll(elements: Collection<E>): Boolean

    /**
     * Removes all elements from this collection.
     */
    public fun clear(): Unit
}

/**
 * A generic ordered collection of elements. Methods in this interface support only read-only access to the list;
 * read/write access is supported through the [MutableList] interface.
 * @param E the type of elements contained in the list. The list is covariant in its element type.
 */
public interface List<out E> : Collection<E> {
    // Query Operations

    override val size: Int

    override fun isEmpty(): Boolean

    override fun contains(element: @UnsafeVariance E): Boolean

    override fun iterator(): Iterator<E>

    // Bulk Operations
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    // Positional Access Operations
    /**
     * Returns the element at the specified index in the list.
     */
    public operator fun get(index: Int): E

    // Search Operations
    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public fun indexOf(element: @UnsafeVariance E): Int

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    public fun lastIndexOf(element: @UnsafeVariance E): Int

    // List Iterators
    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     */
    public fun listIterator(): ListIterator<E>

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
     */
    public fun listIterator(index: Int): ListIterator<E>

    // View
    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
     *
     * Structural changes in the base list make the behavior of the view undefined.
     */
    public fun subList(fromIndex: Int, toIndex: Int): List<E>

    /**
     * Returns a view with the [JsReadonlyArray] methods to consume it in JavaScript as a regular readonly array.
     * Structural changes in the base list are synchronized with the view.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsReadonlyArrayView(): JsReadonlyArray<E> = createJsReadonlyArrayViewFrom(this)
}

/**
 * A generic ordered collection of elements that supports adding and removing elements.
 * @param E the type of elements contained in the list. The mutable list is invariant in its element type.
 */
public interface MutableList<E> : List<E>, MutableCollection<E> {
    // Modification Operations
    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: E): Boolean

    override fun remove(element: E): Boolean

    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to the end of this list.
     *
     * The elements are appended in the order they appear in the [elements] collection.
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    override fun addAll(elements: Collection<E>): Boolean

    /**
     * Inserts all of the elements of the specified collection [elements] into this list at the specified [index].
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    public fun addAll(index: Int, elements: Collection<E>): Boolean

    override fun removeAll(elements: Collection<E>): Boolean

    override fun retainAll(elements: Collection<E>): Boolean

    override fun clear(): Unit

    // Positional Access Operations
    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @return the element previously at the specified position.
     */
    public operator fun set(index: Int, element: E): E

    /**
     * Inserts an element into the list at the specified [index].
     */
    public fun add(index: Int, element: E): Unit

    /**
     * Removes an element at the specified [index] from the list.
     *
     * @return the element that has been removed.
     */
    public fun removeAt(index: Int): E

    // List Iterators
    override fun listIterator(): MutableListIterator<E>

    override fun listIterator(index: Int): MutableListIterator<E>

    // View
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>

    /**
     * Returns a view with the [JsArray] methods to consume it in JavaScript as a regular array.
     * Structural changes in the base list are synchronized with the view, and vice verse.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsArrayView(): JsArray<E> = createJsArrayViewFrom(this)
}

/**
 * A generic unordered collection of elements that does not support duplicate elements.
 * Methods in this interface support only read-only access to the set;
 * read/write access is supported through the [MutableSet] interface.
 * @param E the type of elements contained in the set. The set is covariant in its element type.
 */
public interface Set<out E> : Collection<E> {
    // Query Operations

    override val size: Int

    override fun isEmpty(): Boolean

    override fun contains(element: @UnsafeVariance E): Boolean

    override fun iterator(): Iterator<E>

    // Bulk Operations
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    /**
     * Returns a view with the [JsReadonlySet] methods to consume it in JavaScript as a regular readonly Set.
     * Structural changes in the base set are synchronized with the view.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsReadonlySetView(): JsReadonlySet<E> = createJsReadonlySetViewFrom(this)
}

/**
 * A generic unordered collection of elements that does not support duplicate elements, and supports
 * adding and removing elements.
 * @param E the type of elements contained in the set. The mutable set is invariant in its element type.
 */
public interface MutableSet<E> : Set<E>, MutableCollection<E> {
    // Query Operations
    override fun iterator(): MutableIterator<E>

    // Modification Operations

    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    override fun add(element: E): Boolean

    override fun remove(element: E): Boolean

    // Bulk Modification Operations

    override fun addAll(elements: Collection<E>): Boolean

    override fun removeAll(elements: Collection<E>): Boolean

    override fun retainAll(elements: Collection<E>): Boolean

    override fun clear(): Unit

    /**
     * Returns a view with the [JsSet] methods to consume it in JavaScript as a regular Set.
     * Structural changes in the base set are synchronized with the view, and vice verse.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsSetView(): JsSet<E> = createJsSetViewFrom(this)
}

/**
 * A collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * Methods in this interface support only read-only access to the map; read-write access is supported through
 * the [MutableMap] interface.
 * @param K the type of map keys. The map is invariant in its key type, as it
 *          can accept key as a parameter (of [containsKey] for example) and return it in [keys] set.
 * @param V the type of map values. The map is covariant in its value type.
 */
public interface Map<K, out V> {
    // Query Operations
    /**
     * Returns the number of key/value pairs in the map.
     */
    public val size: Int

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [key].
     */
    public fun containsKey(key: K): Boolean

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     */
    public fun containsValue(value: @UnsafeVariance V): Boolean

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    public operator fun get(key: K): V?

    // Views
    /**
     * Returns a read-only [Set] of all keys in this map.
     */
    public val keys: Set<K>

    /**
     * Returns a read-only [Collection] of all values in this map. Note that this collection may contain duplicate values.
     */
    public val values: Collection<V>

    /**
     * Returns a read-only [Set] of all key/value pairs in this map.
     */
    public val entries: Set<Map.Entry<K, V>>

    /**
     * Represents a key/value pair held by a [Map].
     */
    public interface Entry<out K, out V> {
        /**
         * Returns the key of this key/value pair.
         */
        public val key: K

        /**
         * Returns the value of this key/value pair.
         */
        public val value: V
    }

    /**
     * Returns a view with the [JsReadonlyMap] methods to consume it in JavaScript as a regular readonly Map.
     * Structural changes in the base map are synchronized with the view.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsReadonlyMapView(): JsReadonlyMap<K, V> = createJsReadonlyMapViewFrom(this)
}

/**
 * A modifiable collection that holds pairs of objects (keys and values) and supports efficiently retrieving
 * the value corresponding to each key. Map keys are unique; the map holds only one value for each key.
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
public interface MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun put(key: K, value: V): V?

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    public fun remove(key: K): V?

    // Bulk Modification Operations
    /**
     * Updates this map with key/value pairs from the specified map [from].
     */
    public fun putAll(from: Map<out K, V>): Unit

    /**
     * Removes all elements from this map.
     */
    public fun clear(): Unit

    // Views
    /**
     * Returns a [MutableSet] of all keys in this map.
     */
    override val keys: MutableSet<K>

    /**
     * Returns a [MutableCollection] of all values in this map. Note that this collection may contain duplicate values.
     */
    override val values: MutableCollection<V>

    /**
     * Returns a [MutableSet] of all key/value pairs in this map.
     */
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    /**
     * Represents a key/value pair held by a [MutableMap].
     */
    public interface MutableEntry<K, V> : Map.Entry<K, V> {
        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
        public fun setValue(newValue: V): V
    }

    /**
     * Returns a view with the [JsMap] methods to consume it in JavaScript as a regular Map.
     * Structural changes in the base map are synchronized with the view, and vice verse.
     */
    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("1.9")
    public fun asJsMapView(): JsMap<K, V> = createJsMapViewFrom(this)
}
