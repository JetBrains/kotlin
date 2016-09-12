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

open class HashMap<K, V> : AbstractMap<K, V> {

    private inner class EntrySet : AbstractSet<MutableEntry<K, V>>() {

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

    constructor() : this(InternalHashCodeMap(EqualityComparator.HashCode))

    constructor(initialCapacity: Int, loadFactor: Float = 0f) : this() {
        // This implementation of HashMap has no need of load factors or capacities.
        require(initialCapacity >= 0) { "Negative initial capacity" }
        require(loadFactor >= 0) { "Non-positive load factor" }
    }

    constructor(original: Map<out K, V>) : this() {
        this.putAll(original)
    }

    override fun clear() {
        internalMap.clear()
//        structureChanged(this)
    }

    override fun containsKey(key: K): Boolean = internalMap.contains(key)

    override fun containsValue(value: V): Boolean = internalMap.any { equality.equals(it.value, value) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = EntrySet()

    override operator fun get(key: K): V? = internalMap.get(key)

    override fun put(key: K, value: V): V? = internalMap.put(key, value)

    override fun remove(key: K): V? = internalMap.remove(key)

    override val size: Int get() = internalMap.size

}


public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>(InternalStringMap(EqualityComparator.HashCode)).apply { putAll(pairs) }
}