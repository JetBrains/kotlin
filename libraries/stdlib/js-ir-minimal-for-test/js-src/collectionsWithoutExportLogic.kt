/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.collections

// The minimal version of the collections to exclude the quite big part related to all the implementations for the interfaces

public actual interface Iterable<out T> {
    public actual operator fun iterator(): Iterator<T>
}

public actual interface MutableIterable<out T> : Iterable<T> {
    actual override fun iterator(): MutableIterator<T>
}

public actual interface Collection<out E> : Iterable<E> {
    public actual val size: Int
    public actual fun isEmpty(): Boolean
    public actual operator fun contains(element: @UnsafeVariance E): Boolean
    actual override fun iterator(): Iterator<E>
    public actual fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

public actual interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    actual override fun iterator(): MutableIterator<E>
    public actual fun add(element: E): Boolean
    public actual fun remove(element: E): Boolean
    public actual fun addAll(elements: Collection<E>): Boolean
    public actual fun removeAll(elements: Collection<E>): Boolean
    public actual fun retainAll(elements: Collection<E>): Boolean
    public actual fun clear(): Unit
}

public actual interface List<out E> : Collection<E> {
    actual override val size: Int
    actual override fun isEmpty(): Boolean
    actual override fun contains(element: @UnsafeVariance E): Boolean
    actual override fun iterator(): Iterator<E>
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    public actual operator fun get(index: Int): E
    public actual fun indexOf(element: @UnsafeVariance E): Int
    public actual fun lastIndexOf(element: @UnsafeVariance E): Int
    public actual fun listIterator(): ListIterator<E>
    public actual fun listIterator(index: Int): ListIterator<E>
    public actual fun subList(fromIndex: Int, toIndex: Int): List<E>
}

public actual interface MutableList<E> : List<E>, MutableCollection<E> {
    actual override fun add(element: E): Boolean
    actual override fun remove(element: E): Boolean
    actual override fun addAll(elements: Collection<E>): Boolean
    public actual fun addAll(index: Int, elements: Collection<E>): Boolean
    actual override fun removeAll(elements: Collection<E>): Boolean
    actual override fun retainAll(elements: Collection<E>): Boolean
    actual override fun clear(): Unit
    public actual operator fun set(index: Int, element: E): E
    public actual fun add(index: Int, element: E): Unit
    public actual fun removeAt(index: Int): E
    actual override fun listIterator(): MutableListIterator<E>
    actual override fun listIterator(index: Int): MutableListIterator<E>
    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

public actual interface Set<out E> : Collection<E> {
    actual override val size: Int
    actual override fun isEmpty(): Boolean
    actual override fun contains(element: @UnsafeVariance E): Boolean
    actual override fun iterator(): Iterator<E>
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

public actual interface MutableSet<E> : Set<E>, MutableCollection<E> {
    actual override fun iterator(): MutableIterator<E>
    actual override fun add(element: E): Boolean
    actual override fun remove(element: E): Boolean
    actual override fun addAll(elements: Collection<E>): Boolean
    actual override fun removeAll(elements: Collection<E>): Boolean
    actual override fun retainAll(elements: Collection<E>): Boolean
    actual override fun clear(): Unit
}

public actual interface Map<K, out V> {
    public actual val size: Int
    public actual fun isEmpty(): Boolean
    public actual fun containsKey(key: K): Boolean
    public actual fun containsValue(value: @UnsafeVariance V): Boolean
    public actual operator fun get(key: K): V?
    public actual val keys: Set<K>
    public actual val values: Collection<V>
    public actual val entries: Set<Map.Entry<K, V>>
    public actual interface Entry<out K, out V> {
        public actual val key: K
        public actual val value: V
    }
}

public actual interface MutableMap<K, V> : Map<K, V> {
    public actual fun put(key: K, value: V): V?
    public actual fun remove(key: K): V?
    public actual fun putAll(from: Map<out K, V>): Unit
    public actual fun clear(): Unit
    actual override val keys: MutableSet<K>
    actual override val values: MutableCollection<V>
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    public actual interface MutableEntry<K, V> : Map.Entry<K, V> {
        public actual fun setValue(newValue: V): V
    }
}

public fun <E> createListFrom(array: dynamic): List<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createMutableListFrom(array: dynamic): MutableList<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createSetFrom(set: dynamic): Set<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createMutableSetFrom(set: dynamic): MutableSet<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <K, V> createMapFrom(map: dynamic): Map<K, V> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <K, V> createMutableMapFrom(map: dynamic): MutableMap<K, V> = TODO("Use WITH_STDLIB pragma to use this function")
