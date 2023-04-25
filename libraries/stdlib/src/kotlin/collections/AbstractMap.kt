/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractMap
 * Copyright 2007 Google Inc.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the read-only [Map] interface.
 *
 * The implementor is required to implement [entries] property, which should return read-only set of map entries.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The map is covariant in its value type.
 */
@SinceKotlin("1.1")
public abstract class AbstractMap<K, out V> protected constructor() : Map<K, V> {

    override fun containsKey(key: K): Boolean {
        return implFindEntry(key) != null
    }

    override fun containsValue(value: @UnsafeVariance V): Boolean = entries.any { it.value == value }

    internal fun containsEntry(entry: Map.Entry<*, *>?): Boolean {
        // since entry comes from @UnsafeVariance parameters it can be virtually anything
        if (entry !is Map.Entry<*, *>) return false
        val key = entry.key
        val value = entry.value
        val ourValue = get(key)

        if (value != ourValue) {
            return false
        }

        // Perhaps it was null and we don't contain the key?
        if (ourValue == null && !containsKey(key)) {
            return false
        }

        return true
    }


    /**
     * Compares this map with other instance with the ordered structural equality.
     *
     * @return true, if [other] instance is a [Map] of the same size, all entries of which are contained in the [entries] set of this map.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return other.entries.all { containsEntry(it) }
    }

    override operator fun get(key: K): V? = implFindEntry(key)?.value


    /**
     * Returns the hash code value for this map.
     *
     * It is the same as the hashCode of [entries] set.
     */
    override fun hashCode(): Int = entries.hashCode()

    override fun isEmpty(): Boolean = size == 0
    override val size: Int get() = entries.size

    /**
     * Returns a read-only [Set] of all keys in this map.
     *
     * Accessing this property first time creates a keys view from [entries].
     * All subsequent accesses just return the created instance.
     */
    override val keys: Set<K>
        get() {
            if (_keys == null) {
                _keys = object : AbstractSet<K>() {
                    override operator fun contains(element: K): Boolean = containsKey(element)

                    override operator fun iterator(): Iterator<K> {
                        val entryIterator = entries.iterator()
                        return object : Iterator<K> {
                            override fun hasNext(): Boolean = entryIterator.hasNext()
                            override fun next(): K = entryIterator.next().key
                        }
                    }

                    override val size: Int get() = this@AbstractMap.size
                }
            }
            return _keys!!
        }

    @kotlin.concurrent.Volatile
    private var _keys: Set<K>? = null


    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) "(this Map)" else o.toString()

    /**
     * Returns a read-only [Collection] of all values in this map.
     *
     * Accessing this property first time creates a values view from [entries].
     * All subsequent accesses just return the created instance.
     */
    override val values: Collection<V>
        get() {
            if (_values == null) {
                _values = object : AbstractCollection<V>() {
                    override operator fun contains(element: @UnsafeVariance V): Boolean = containsValue(element)

                    override operator fun iterator(): Iterator<V> {
                        val entryIterator = entries.iterator()
                        return object : Iterator<V> {
                            override fun hasNext(): Boolean = entryIterator.hasNext()
                            override fun next(): V = entryIterator.next().value
                        }
                    }

                    override val size: Int get() = this@AbstractMap.size
                }
            }
            return _values!!
        }

    @kotlin.concurrent.Volatile
    private var _values: Collection<V>? = null

    private fun implFindEntry(key: K): Map.Entry<K, V>? = entries.firstOrNull { it.key == key }

    internal companion object {

        internal fun entryHashCode(e: Map.Entry<*, *>): Int = with(e) { (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0) }
        internal fun entryToString(e: Map.Entry<*, *>): String = with(e) { "$key=$value" }
        internal fun entryEquals(e: Map.Entry<*, *>, other: Any?): Boolean {
            if (other !is Map.Entry<*, *>) return false
            return e.key == other.key && e.value == other.value
        }
    }
}
