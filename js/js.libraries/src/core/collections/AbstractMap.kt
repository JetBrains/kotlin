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

abstract class AbstractMap<K, V> protected constructor() : MutableMap<K, V> {

    /**
     * A mutable [Map.Entry] shared by several [Map] implementations.
     */
    open class SimpleEntry<K, V> : AbstractEntry<K, V> {
        constructor(key: K, value: V) : super(key, value) {
        }

        constructor(entry: Entry<out K, out V>) : super(entry.key, entry.value) {
        }
    }

    /**
     * An immutable [Map.Entry] shared by several [Map] implementations.
     */
    class SimpleImmutableEntry<K, V> : AbstractEntry<K, V> {
        constructor(key: K, value: V) : super(key, value) {
        }

        constructor(entry: Entry<out K, out V>) : super(entry.key, entry.value) {
        }

        override fun setValue(value: V): V {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Basic [Map.Entry] implementation used by [SimpleEntry]
     * and [SimpleImmutableEntry].
     */
    private abstract class AbstractEntry<K, V> protected constructor(private val key: K, private var value: V?) : Map.Entry<K, V> {

        override fun getKey(): K {
            return key
        }

        override fun getValue(): V {
            return value
        }

        override fun setValue(value: V): V {
            val oldValue = this.value
            this.value = value
            return oldValue
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Entry<*, *>) {
                return false
            }
            val entry = other as Entry<*, *>?
            return key == entry!!.key && value == entry!!.value
        }

        /**
         * Calculate the hash code using Sun's specified algorithm.
         */
        override fun hashCode(): Int {
            return Objects.hashCode(key) xor Objects.hashCode(value)
        }

        override fun toString(): String {
            // for compatibility with the real Jre: issue 3422
            return key + "=" + value
        }
    }

    override fun clear() {
        entries.clear()
    }

    override fun containsKey(key: Any): Boolean {
        return implFindEntry(key, false) != null
    }

    override fun containsValue(value: Any): Boolean {
        for ((key, v) in entries) {
            if (value == v) {
                return true
            }
        }
        return false
    }

    internal fun containsEntry(entry: Entry<*, *>): Boolean {
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

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is Map<*, *>) {
            return false
        }
        if (size != obj.size) {
            return false
        }

        for (entry in obj.entries) {
            if (!containsEntry(entry)) {
                return false
            }
        }
        return true
    }

    override operator fun get(key: Any): V? {
        return getEntryValueOrNull<K, V>(implFindEntry(key, false))
    }

    override fun hashCode(): Int {
        return Collections.hashCode(entries)
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun keySet(): Set<K> {
        return object : AbstractSet<K>() {
            override fun clear() {
                this@AbstractMap.clear()
            }

            override operator fun contains(key: Any?): Boolean {
                return containsKey(key)
            }

            override operator fun iterator(): Iterator<K> {
                val outerIter = entries.iterator()
                return object : Iterator<K> {
                    override fun hasNext(): Boolean {
                        return outerIter.hasNext()
                    }

                    override fun next(): K {
                        val entry = outerIter.next()
                        return entry.key
                    }

                    override fun remove() {
                        outerIter.remove()
                    }
                }
            }

            override fun remove(key: Any?): Boolean {
                if (containsKey(key)) {
                    this@AbstractMap.remove(key)
                    return true
                }
                return false
            }

            override fun size(): Int {
                return this@AbstractMap.size
            }
        }
    }

    override fun put(key: K, value: V): V {
        throw UnsupportedOperationException("Put not supported on this map")
    }

    override fun putAll(map: Map<out K, V>) {
        checkNotNull(map)
        for ((key, value) in map) {
            put(key, value)
        }
    }

    override fun remove(key: Any): V {
        return getEntryValueOrNull<K, V>(implFindEntry(key, true))
    }

    override fun size(): Int {
        return entries.size
    }

    override fun toString(): String {
        val joiner = StringJoiner(", ", "{", "}")
        for (entry in entries) {
            joiner.add(toString(entry))
        }
        return joiner.toString()
    }

    private fun toString(entry: Entry<K, V>): String {
        return toString(entry.key) + "=" + toString(entry.value)
    }

    private fun toString(o: Any): String {
        return if (o === this) "(this Map)" else o.toString()
    }

    override fun values(): Collection<V> {
        return object : AbstractCollection<V>() {
            override fun clear() {
                this@AbstractMap.clear()
            }

            override operator fun contains(value: Any?): Boolean {
                return containsValue(value)
            }

            override operator fun iterator(): Iterator<V> {
                val outerIter = entries.iterator()
                return object : Iterator<V> {
                    override fun hasNext(): Boolean {
                        return outerIter.hasNext()
                    }

                    override fun next(): V {
                        val entry = outerIter.next()
                        return entry.value
                    }

                    override fun remove() {
                        outerIter.remove()
                    }
                }
            }

            override fun size(): Int {
                return this@AbstractMap.size
            }
        }
    }

    private fun implFindEntry(key: Any, remove: Boolean): Entry<K, V>? {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            var entry = iter.next()
            val k = entry.key
            if (key == k) {
                if (remove) {
                    entry = SimpleEntry(entry.key, entry.value)
                    iter.remove()
                }
                return entry
            }
        }
        return null
    }

    companion object {

        internal fun <K, V> getEntryKeyOrNull(entry: Entry<K, V>?): K? {
            return if (entry == null) null else entry!!.key
        }

        internal fun <K, V> getEntryValueOrNull(entry: Entry<K, V>?): V? {
            return if (entry == null) null else entry!!.value
        }
    }
}
