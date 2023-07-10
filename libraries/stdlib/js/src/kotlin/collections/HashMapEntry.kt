/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * This is the most performant implementations of [HashMap.keys] container.
 * It is implemented over [InternalMap] interface and can not be used without it.
 * [HashMapKeys] is more efficient than the implementation [HashMapKeysDefault] from HashMapEntryDefault.kt.
 */
internal class HashMapKeys<E> internal constructor(
    private val backing: InternalMap<E, *>,
) : MutableSet<E>, AbstractMutableSet<E>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.size == 0
    override fun contains(element: E): Boolean = backing.contains(element)
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = backing.remove(element) != null
    override fun iterator(): MutableIterator<E> = backing.keysIterator()

    override fun checkIsMutable() = backing.checkIsMutable()
}

/**
 * This is the most performant implementations of [HashMap.values] container.
 * It is implemented over [InternalMap] interface and can not be used without it.
 * [HashMapValues] is more efficient than the implementation [HashMapValuesDefault] from HashMapEntryDefault.kt.
 */
internal class HashMapValues<V> internal constructor(
    private val backing: InternalMap<*, V>,
) : MutableCollection<V>, AbstractMutableCollection<V>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.size == 0
    override fun contains(element: V): Boolean = backing.containsValue(element)
    override fun add(element: V): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<V>): Boolean = throw UnsupportedOperationException()
    override fun clear() = backing.clear()
    override fun iterator(): MutableIterator<V> = backing.valuesIterator()
    override fun remove(element: V): Boolean = backing.removeValue(element)

    override fun checkIsMutable() = backing.checkIsMutable()
}

/**
 * Note: intermediate class with [E] `: Map.Entry<K, V>` is required to support
 * [contains] for values that are [Map.Entry] but not [MutableMap.MutableEntry],
 * and probably same for other functions.
 * This is important because an instance of this class can be used as a result of [Map.entries],
 * which should support [contains] for [Map.Entry].
 * For example, this happens when upcasting [MutableMap] to [Map].
 *
 * The compiler enables special type-safe barriers to methods like [contains], which has [UnsafeVariance].
 * Changing type from [MutableMap.MutableEntry] to [E] makes the compiler generate barriers checking that
 * argument `is` [E] (so technically `is` [Map.Entry]) instead of `is` [MutableMap.MutableEntry].
 *
 * See also [KT-42248](https://youtrack.jetbrains.com/issue/KT-42428).
 */
internal abstract class HashMapEntrySetBase<K, V, E : Map.Entry<K, V>> internal constructor(
    val backing: InternalMap<K, V>,
) : MutableSet<E>, AbstractMutableSet<E>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.size == 0
    override fun contains(element: E): Boolean = backing.containsEntry(element)
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = backing.removeEntry(element)
    override fun containsAll(elements: Collection<E>): Boolean = backing.containsAllEntries(elements)

    override fun checkIsMutable() = backing.checkIsMutable()
}

/**
 * This is the implementations of [HashMap.entries] container.
 * It is implemented over [InternalMap] interface and can not be used without it.
 */
internal class HashMapEntrySet<K, V> internal constructor(
    backing: InternalMap<K, V>,
) : HashMapEntrySetBase<K, V, MutableMap.MutableEntry<K, V>>(backing) {
    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = backing.entriesIterator()
}
