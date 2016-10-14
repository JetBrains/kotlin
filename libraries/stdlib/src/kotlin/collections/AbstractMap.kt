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

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return other.entries.all { containsEntry(it) }
    }

    override operator fun get(key: K): V? = implFindEntry(key)?.value

    override fun hashCode(): Int = entries.hashCode()

    override fun isEmpty(): Boolean = size == 0
    override val size: Int get() = entries.size

    private @Volatile var _keys: Set<K>? = null
    override val keys: Set<K> get() {
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

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) "(this Map)" else o.toString()

    private @Volatile var _values: Collection<V>? = null
    override val values: Collection<V> get() {
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
