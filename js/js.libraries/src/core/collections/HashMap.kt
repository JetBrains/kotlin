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
 * Based on GWT AbstractHashMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

import kotlin.collections.Map.Entry
import kotlin.collections.MutableMap.MutableEntry

/**
 * Hash table based implementation of the [MutableMap] interface.
 *
 * This implementation makes no guarantees regarding the order of enumeration of [keys], [elements] and [entries] collections.
 */
public open class HashMap<K, V> : AbstractMutableMap<K, V> {

    private inner class EntrySet : AbstractMutableSet<MutableEntry<K, V>>() {

        override fun add(element: MutableEntry<K, V>): Boolean = throw UnsupportedOperationException("Add is not supported on entries")
        override fun clear() {
            this@HashMap.clear()
        }

        override operator fun contains(element: MutableEntry<K, V>): Boolean = containsEntry(element)

        override operator fun iterator(): MutableIterator<MutableEntry<K, V>> = internalMap.iterator()

        override fun remove(element: MutableEntry<K, V>): Boolean {
            if (contains(element)) {
                this@HashMap.remove(element.key)
                return true
            }
            return false
        }

        override val size: Int get() = this@HashMap.size
    }


    /**
     * Internal implementation of the map: either string-based or hashcode-based.
     */
    private val internalMap: InternalMap<K, V>

    private val equality: EqualityComparator

    internal constructor(internalMap: InternalMap<K, V>) : super() {
        this.internalMap = internalMap
        this.equality = internalMap.equality
    }

    /**
     * Constructs an empty [HashMap] instance.
     */
    constructor() : this(InternalHashCodeMap(EqualityComparator.HashCode))

    /**
     * Constructs an empty [HashMap] instance.
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    constructor(initialCapacity: Int, loadFactor: Float = 0f) : this() {
        // This implementation of HashMap has no need of load factors or capacities.
        require(initialCapacity >= 0) { "Negative initial capacity" }
        require(loadFactor >= 0) { "Non-positive load factor" }
    }

    /**
     * Constructs an instance of [HashMap] filled with the contents of the specified [original] map.
     */
    constructor(original: Map<out K, V>) : this() {
        this.putAll(original)
    }

    override fun clear() {
        internalMap.clear()
//        structureChanged(this)
    }

    override fun containsKey(key: K): Boolean = internalMap.contains(key)

    override fun containsValue(value: V): Boolean = internalMap.any { equality.equals(it.value, value) }

    private var _entries: MutableSet<MutableMap.MutableEntry<K, V>>? = null
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() {
        if (_entries == null) {
            _entries = createEntrySet()
        }
        return _entries!!
    }

    protected open fun createEntrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = EntrySet()

    override operator fun get(key: K): V? = internalMap.get(key)

    override fun put(key: K, value: V): V? = internalMap.put(key, value)

    override fun remove(key: K): V? = internalMap.remove(key)

    override val size: Int get() = internalMap.size

}

/**
 * Constructs the specialized implementation of [HashMap] with [String] keys, which stores the keys as properties of
 * JS object without hashing them.
 */
public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>(InternalStringMap(EqualityComparator.HashCode)).apply { putAll(pairs) }
}