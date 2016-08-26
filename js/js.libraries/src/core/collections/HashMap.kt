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

 /*
    private inner class EntrySetIterator : Iterator<Entry<K, V>> {
        private val stringMapEntries = stringMap!!.iterator()
        private var current: MutableIterator<Entry<K, V>> = stringMapEntries
        private var last: MutableIterator<Entry<K, V>>? = null
        private var hasNext = computeHasNext()

        init {
            recordLastKnownStructure(this@AbstractHashMap, this)
        }

        override fun hasNext(): Boolean {
            return hasNext
        }

        private fun computeHasNext(): Boolean {
            if (current.hasNext()) {
                return true
            }
            if (current !== stringMapEntries) {
                return false
            }
            current = hashCodeMap!!.iterator()
            return current.hasNext()
        }

        override fun next(): Entry<K, V> {
            checkStructuralChange(this@AbstractHashMap, this)
            checkElement(hasNext())

            last = current
            val rv = current.next()
            hasNext = computeHasNext()

            return rv
        }

        override fun remove() {
            checkState(last != null)
            checkStructuralChange(this@AbstractHashMap, this)

            last!!.remove()
            last = null
            hasNext = computeHasNext()

            recordLastKnownStructure(this@AbstractHashMap, this)
        }
    }*/

    /**
     * A map of integral hashCodes onto entries.
     */
    private val internalMap: InternalMap<K, V>

    private val equality: EqualityComparator

//    /**
//     * A map of Strings onto values.
//     */
//    @Transient private var stringMap: InternalStringMap<K, V>? = null
//
    internal constructor(internalMap: InternalMap<K, V>) : super() {
        this.internalMap = internalMap
        this.equality = internalMap.equality
    }

    constructor() : this(InternalHashCodeMap(EqualityComparator.HashCode))
//    init {
//        reset()
//    }

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
//        reset()
//    }
//
//    private fun reset() {
//        internalMap = internalMapProvider(this)
//        stringMap = InternalStringMap<K, V>(this)
//        structureChanged(this)
    }

//    @SpecializeMethod(params = { String.class }, target = "hasStringValue")
    override fun containsKey(key: K): Boolean {
        return internalMap.contains(key)
//        return if (key is String)
//            hasStringValue(JsUtils.unsafeCastToString(key))
//        else
//            hasHashValue(key)
    }

    override fun containsValue(value: V): Boolean {
        return /*containsValue(value, stringMap) || */ containsValue(value, internalMap)
    }

    private fun containsValue(value: V, entries: Iterable<Entry<K, V>>): Boolean = entries.any { equality.equals(it.value, value) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = EntrySet()

//    @SpecializeMethod(params = { String.class }, target = "getStringValue")
    override operator fun get(key: K): V? {
        return internalMap.get(key)
//        return if (key is String)
//            getStringValue(JsUtils.unsafeCastToString(key))
//        else
//            getHashValue(key)
    }

//    @SpecializeMethod(params = { String.class, Object .class }, target = "putStringValue")
    override fun put(key: K, value: V): V? {
        return internalMap.put(key, value)
//        return if (key is String)
//            putStringValue(JsUtils.unsafeCastToString(key), value)
//        else
//            putHashValue(key, value)
    }

//    @SpecializeMethod(params = { String.class }, target = "removeStringValue")
    override fun remove(key: K): V? {
        return internalMap.remove(key)
//        return if (key is String)
//            removeStringValue(JsUtils.unsafeCastToString(key))
//        else
//            removeHashValue(key)
    }

    override val size: Int get() {
        return internalMap.size /*+ stringMap!!.size()*/
    }

//    /**
//     * Subclasses must override to return a whether or not two keys or values are
//     * equal.
//     */
//    internal abstract fun equals(value1: Any?, value2: Any?): Boolean
//
//    /**
//     * Subclasses must override to return a hash code for a given key. The key is
//     * guaranteed to be non-null and not a String.
//     */
//    internal abstract fun getHashCode(key: K): Int
//
//    /**
//     * Returns the Map.Entry whose key is Object equal to `key`,
//     * provided that `key`'s hash code is `hashCode`;
//     * or `null` if no such Map.Entry exists at the specified
//     * hashCode.
//     */
//    private fun getHashValue(key: K): V? {
//        return hashCodeMap.getEntry(key)?.value
//    }

//    /**
//     * Returns the value for the given key in the stringMap. Returns
//     * `null` if the specified key does not exist.
//     */
//    private fun getStringValue(key: String?): V {
//        return if (key == null) getHashValue(null) else stringMap!!.get(key)
//    }

//    /**
//     * Returns true if the a key exists in the hashCodeMap that is Object equal to
//     * `key`, provided that `key`'s hash code is
//     * `hashCode`.
//     */
//    private fun hasHashValue(key: K): Boolean {
//        return hashCodeMap.getEntry(key) != null
//    }
//
//    /**
//     * Returns true if the given key exists in the stringMap.
//     */
//    private fun hasStringValue(key: String?): Boolean {
//        return if (key == null) hasHashValue(null) else stringMap!!.contains(key)
//    }

//    /**
//     * Sets the specified key to the specified value in the hashCodeMap. Returns
//     * the value previously at that key. Returns `null` if the
//     * specified key did not exist.
//     */
//    private fun putHashValue(key: K, value: V): V? {
//        return hashCodeMap.put(key, value)
//    }

//    /**
//     * Sets the specified key to the specified value in the stringMap. Returns the
//     * value previously at that key. Returns `null` if the specified
//     * key did not exist.
//     */
//    private fun putStringValue(key: String?, value: V): V {
//        return if (key == null) putHashValue(null, value) else stringMap!!.put(key, value)
//    }

//    /**
//     * Removes the pair whose key is Object equal to `key` from
//     * `hashCodeMap`, provided that `key`'s hash code
//     * is `hashCode`. Returns the value that was associated with the
//     * removed key, or null if no such key existed.
//     */
//    private fun removeHashValue(key: K): V? {
//        return hashCodeMap.remove(key)
//    }

//    /**
//     * Removes the specified key from the stringMap and returns the value that was
//     * previously there. Returns `null` if the specified key does not
//     * exist.
//     */
//    private fun removeStringValue(key: String?): V {
//        return if (key == null) removeHashValue(null) else stringMap!!.remove(key)
//    }
}


public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>(InternalStringMap(EqualityComparator.HashCode)).apply { putAll(pairs) }
}