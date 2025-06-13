/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * The common interface of [InternalStringMap] and [InternalHashMap].
 */
internal interface InternalMap<K, V> {
    val size: Int

    @IgnorableReturnValue
    fun put(key: K, value: V): V?
    fun putAll(from: Map<out K, V>)

    operator fun get(key: K): V?

    operator fun contains(key: K): Boolean
    fun containsValue(value: V): Boolean
    fun containsEntry(entry: Map.Entry<K, V>): Boolean
    fun containsOtherEntry(entry: Map.Entry<*, *>): Boolean

    @IgnorableReturnValue
    fun remove(key: K): V?
    @IgnorableReturnValue
    fun removeKey(key: K): Boolean
    @IgnorableReturnValue
    fun removeValue(value: V): Boolean
    @IgnorableReturnValue
    fun removeEntry(entry: Map.Entry<K, V>): Boolean

    fun clear()

    fun keysIterator(): MutableIterator<K>
    fun valuesIterator(): MutableIterator<V>
    fun entriesIterator(): MutableIterator<MutableMap.MutableEntry<K, V>>

    fun checkIsMutable()
    fun build()

    fun containsAllEntries(m: Collection<Map.Entry<*, *>>): Boolean {
        return m.all {
            // entry can be null due to variance.
            val entry = it.unsafeCast<Any?>()
            (entry is Map.Entry<*, *>) && containsOtherEntry(entry)
        }
    }
}
