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

public abstract class AbstractMutableMap<K, V> protected constructor() : AbstractMap<K, V>(), MutableMap<K, V> {

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

    override fun clear() {
        entries.clear()
    }

    override val keys: MutableSet<K> get() {
        return object : AbstractMutableSet<K>() {
            override fun clear() {
                this@AbstractMutableMap.clear()
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
                    this@AbstractMutableMap.remove(element)
                    return true
                }
                return false
            }

            override val size: Int get() = this@AbstractMutableMap.size
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

    override val values: MutableCollection<V> get() {
        return object : AbstractMutableCollection<V>() {
            override fun clear() = this@AbstractMutableMap.clear()

            override operator fun contains(element: V): Boolean = containsValue(element)

            override operator fun iterator(): MutableIterator<V> {
                val outerIter = entries.iterator()
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean = outerIter.hasNext()
                    override fun next(): V = outerIter.next().value
                    override fun remove() = outerIter.remove()
                }
            }

            override val size: Int get() = this@AbstractMutableMap.size

            // TODO: should we implement them this way? Currently it's unspecified in JVM
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Collection<*>) return false
                return AbstractList.orderedEquals(this, other)
            }
            override fun hashCode(): Int = AbstractList.orderedHashCode(this)
        }
    }

    override fun remove(key: K): V? {
        val iter = entries.iterator()
        while (iter.hasNext()) {
            var entry = iter.next()
            val k = entry.key
            if (key == k) {
                val value = entry.value
                iter.remove()
                return value
            }
        }
        return null
    }

}
