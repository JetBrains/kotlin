/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.collections

// The minimal version of the collections to exclude the quite big part related to all the implementations for the interfaces

public interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}

public interface MutableIterable<out T> : Iterable<T> {
    override fun iterator(): MutableIterator<T>
}

public interface Collection<out E> : Iterable<E> {
    public val size: Int
    public fun isEmpty(): Boolean
    public operator fun contains(element: @UnsafeVariance E): Boolean
    override fun iterator(): Iterator<E>
    public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

public interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    override fun iterator(): MutableIterator<E>
    public fun add(element: E): Boolean
    public fun remove(element: E): Boolean
    public fun addAll(elements: Collection<E>): Boolean
    public fun removeAll(elements: Collection<E>): Boolean
    public fun retainAll(elements: Collection<E>): Boolean
    public fun clear(): Unit
}

public interface List<out E> : Collection<E> {
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun iterator(): Iterator<E>
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    public operator fun get(index: Int): E
    public fun indexOf(element: @UnsafeVariance E): Int
    public fun lastIndexOf(element: @UnsafeVariance E): Int
    public fun listIterator(): ListIterator<E>
    public fun listIterator(index: Int): ListIterator<E>
    public fun subList(fromIndex: Int, toIndex: Int): List<E>
}

public interface MutableList<E> : List<E>, MutableCollection<E> {
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    public fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear(): Unit
    public operator fun set(index: Int, element: E): E
    public fun add(index: Int, element: E): Unit
    public fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

public interface Set<out E> : Collection<E> {
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun iterator(): Iterator<E>
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

public interface MutableSet<E> : Set<E>, MutableCollection<E> {
    override fun iterator(): MutableIterator<E>
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear(): Unit
}

public interface Map<K, out V> {
    public val size: Int
    public fun isEmpty(): Boolean
    public fun containsKey(key: K): Boolean
    public fun containsValue(value: @UnsafeVariance V): Boolean
    public operator fun get(key: K): V?
    public val keys: Set<K>
    public val values: Collection<V>
    public val entries: Set<Map.Entry<K, V>>
    public interface Entry<out K, out V> {
        public val key: K
        public val value: V
    }
}

public interface MutableMap<K, V> : Map<K, V> {
    public fun put(key: K, value: V): V?
    public fun remove(key: K): V?
    public fun putAll(from: Map<out K, V>): Unit
    public fun clear(): Unit
    override val keys: MutableSet<K>
    override val values: MutableCollection<V>
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    public interface MutableEntry<K, V> : Map.Entry<K, V> {
        public fun setValue(newValue: V): V
    }
}

public fun <E> createListFrom(array: dynamic): List<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createMutableListFrom(array: dynamic): MutableList<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createSetFrom(set: dynamic): Set<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <E> createMutableSetFrom(set: dynamic): MutableSet<E> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <K, V> createMapFrom(map: dynamic): Map<K, V> = TODO("Use WITH_STDLIB pragma to use this function")
public fun <K, V> createMutableMapFrom(map: dynamic): MutableMap<K, V> = TODO("Use WITH_STDLIB pragma to use this function")
