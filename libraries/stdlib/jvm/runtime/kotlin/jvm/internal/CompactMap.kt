/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.math.ceil

internal fun <K: Any, V> Map<K, V>.compact() = LinearProbingMap(this)

private const val LOAD_FACTOR = 0.8

/**
 * This is a compact implementation of a linear probing hash map (see https://en.wikipedia.org/wiki/Linear_probing), using the same
 * underlying layout as java.util.IdentityHashMap, where keys and values are interleaved in a single array. To simplify the design, the map
 * is constructed with a fixed load factor, which allows calculating the size of the map from the size of the backing array. A load of 0.5
 * is small enough to offer comparable performance to HashMap, while keeping overhead to four array entries per key value pair. A load of
 * 0.8 offers a smaller overhead, but decreases performance slightly.
 */
@Suppress("UNCHECKED_CAST")
@JvmInline
internal value class LinearProbingMap<K: Any, V> private constructor(private val data: Array<Any?>) : Map<K, V> {
    constructor(src: Map<K, V>) : this(Array(ceil(src.size / LOAD_FACTOR).toInt() * 2) { null }) {
        for ((key, value) in src.entries.sortedBy { hash(it.key) }) {
            val index = findIndex(key)
            data[index] = key
            data[index + 1] = value
        }
    }

    override val size: Int
        get() = (data.size / 2 * LOAD_FACTOR).toInt()

    override fun isEmpty(): Boolean = data.isEmpty()

    override fun containsKey(key: K): Boolean = data.isNotEmpty() && data[findIndex(key)] != null

    override fun containsValue(value: V): Boolean {
        for (i in data.indices step 2) {
            if (data[i + 1] == value) return true
        }
        return false
    }

    override operator fun get(key: K): V? = if (data.isEmpty()) null else data[findIndex(key) + 1] as V?

    override val keys: Set<K> get() = data.indices.step(2).mapNotNull { data[it] as K? }.toSet()

    override val values: Collection<V> get() = data.indices.step(2).filter { data[it] != null }.map { data[it + 1] as V }

    private data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

    override val entries: Set<Map.Entry<K, V>>
        get() = data.indices.step(2).filter { data[it] != null }.map { Entry(data[it] as K, data[it + 1] as V) }.toSet()

    private fun hash(key: Any): Int = (key.hashCode() * 2).mod(data.size)

    private fun findIndex(key: K): Int {
        val hash = hash(key)
        for (i in hash until data.size step 2) {
            if (data[i] == null || key == data[i]) return i
        }
        for (i in 0 until hash step 2) {
            if (data[i] == null || key == data[i]) return i
        }
        throw IllegalStateException("CompactMap should contain a null entry")
    }
}
