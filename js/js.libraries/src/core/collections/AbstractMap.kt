/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
/*
 * Based on GWT AbstractMap
 * Copyright 2007 Google Inc.
 */

package kotlin.collections

public abstract class AbstractMap<K, V> protected constructor() : MutableMap<K, V> {

    /**
     * A mutable [Map.Entry] shared by several [Map] implementations.
     */
    open class SimpleEntry<K, V>(override val key: K, value: V) : MutableMap.MutableEntry<K, V> {
        constructor(entry: Map.Entry<K, V>) : this(entry.key, entry.value)

        private var _value = value

        override val value: V get() = _value

        override fun setValue(newValue: V): V {
            val oldValue = this._value
            this._value = newValue
            return oldValue
        }

        override fun hashCode(): Int = entryHashCode(this)
        override fun toString(): String = entryToString(this)
        override fun equals(other: Any?): Boolean = entryEquals(this, other)

    }

    /**
     * An immutable [Map.Entry] shared by several [Map] implementations.
     */
    class SimpleImmutableEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
        constructor(entry: Map.Entry<K, V>) : this(entry.key, entry.value)

        override fun hashCode(): Int = entryHashCode(this)
        override fun toString(): String = entryToString(this)
        override fun equals(other: Any?): Boolean = entryEquals(this, other)
    }


    override fun clear() {
        entries.clear()
    }

    override fun containsKey(key: K): Boolean {
        return implFindEntry(key, false) != null
    }

    override fun containsValue(value: V): Boolean = entries.any { it.value == value }

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

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return other.entries.all { containsEntry(it) }
    }

    override operator fun get(key: K): V? = implFindEntry(key, false)?.value

    override fun hashCode(): Int = entries.hashCode()

    override fun isEmpty(): Boolean = size == 0
    override val size: Int get() = entries.size


    override val keys: MutableSet<K> get() {
        return object : AbstractMutableSet<K>() {
            override fun clear() {
                this@AbstractMap.clear()
            }

            override operator fun contains(element: K): Boolean = containsKey(element)

            override operator fun iterator(): MutableIterator<K> {
                val outerIter = entries.iterator()
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean = outerIter.hasNext()
                    override fun next(): K = outerIter.next().key
                    override fun remove() = outerIter.remove()
                }
            }

            override fun remove(element: K): Boolean {
                if (containsKey(element)) {
                    this@AbstractMap.remove(element)
                    return true
                }
                return false
            }

            override val size: Int get() = this@AbstractMap.size
        }
    }

    override fun put(key: K, value: V): V? {
        throw UnsupportedOperationException("Put not supported on this map")
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun remove(key: K): V? = implFindEntry(key, true)?.value

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) "(this Map)" else o.toString()

    override val values: MutableCollection<V> get() {
        return object : AbstractMutableCollection<V>() {
            override fun clear() = this@AbstractMap.clear()

            override operator fun contains(element: V): Boolean = containsValue(element)

            override operator fun iterator(): MutableIterator<V> {
                val outerIter = entries.iterator()
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean = outerIter.hasNext()
                    override fun next(): V = outerIter.next().value
                    override fun remove() = outerIter.remove()
                }
            }

            override val size: Int get() = this@AbstractMap.size

            // TODO: should we implement them this way? Currently it's unspecified in JVM
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Collection<*>) return false
                return AbstractList.orderedEquals(this, other)
            }
            override fun hashCode(): Int = AbstractList.orderedHashCode(this)
        }
    }

    private fun implFindEntry(key: K, remove: Boolean): Map.Entry<K, V>? {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            var entry = iter.next()
            val k = entry.key
            if (key == k) {
                if (remove) {
                    entry = SimpleEntry(entry)
                    iter.remove()
                }
                return entry
            }
        }
        return null
    }

    companion object {

        internal fun entryHashCode(e: Map.Entry<*, *>): Int = with(e) { (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0) }
        internal fun entryToString(e: Map.Entry<*, *>): String = with(e) { "$key=$value" }
        internal fun entryEquals(e: Map.Entry<*, *>, other: Any?): Boolean {
            if (other !is Map.Entry<*, *>) return false
            return e.key == other.key && e.value == other.value
        }
    }
}
