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
 * Based on GWT InternalHashCodeMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry
import kotlin.collections.AbstractMutableMap.SimpleEntry

/**
 * A simple wrapper around JavaScriptObject to provide [java.util.Map]-like semantics for any
 * key type.
 *
 *
 * Implementation notes:
 *
 *
 * A key's hashCode is the index in backingMap which should contain that key. Since several keys may
 * have the same hash, each value in hashCodeMap is actually an array containing all entries whose
 * keys share the same hash.
 */
internal class InternalHashCodeMap<K, V>(override val equality: EqualityComparator) : InternalMap<K, V> {

    private var backingMap: dynamic = js("Object.create(null)")
    override var size: Int = 0
        private set

    override fun put(key: K, value: V): V? {
        val hashCode = equality.getHashCode(key)
        val chain = getChainOrNull(hashCode)
        if (chain == null) {
            // This is a new chain, put it to the map.
            backingMap[hashCode] = arrayOf(SimpleEntry(key, value))
        }
        else {
            // Chain already exists, perhaps key also exists.
            val entry = chain.findEntryInChain(key)
            if (entry != null) {
                return entry.setValue(value)
            }
            chain.asDynamic().push(SimpleEntry(key, value))
        }
        size++
//        structureChanged(host)
        return null
    }

    override fun remove(key: K): V? {
        val hashCode = equality.getHashCode(key)
        val chain = getChainOrNull(hashCode) ?: return null
        for (index in 0..chain.size-1) {
            val entry = chain[index]
            if (equality.equals(key, entry.key)) {
                if (chain.size == 1) {
                    chain.asDynamic().length = 0
                    // remove the whole array
                    deleteProperty(backingMap, hashCode)
                }
                else {
                    // splice out the entry we're removing
                    chain.asDynamic().splice(index, 1)
                }
                size--
//                structureChanged(host)
                return entry.value
            }
        }
        return null
    }

    override fun clear() {
        backingMap = js("Object.create(null)")
        size = 0
    }

    override fun contains(key: K): Boolean = getEntry(key) != null

    override fun get(key: K): V? = getEntry(key)?.value

    private fun getEntry(key: K): MutableEntry<K, V>? =
            getChainOrNull(equality.getHashCode(key))?.findEntryInChain(key)

    private fun Array<MutableEntry<K, V>>.findEntryInChain(key: K): MutableEntry<K, V>? =
            firstOrNull { entry -> equality.equals(entry.key, key) }

    override fun iterator(): MutableIterator<MutableEntry<K, V>> {

        return object : MutableIterator<MutableEntry<K, V>> {
            var state = -1 // -1 not ready, 0 - ready, 1 - done

            val keys: Array<Int> = js("Object").keys(backingMap)
            var keyIndex = -1

            var chain: Array<MutableEntry<K, V>>? = null
            var itemIndex = -1
            var lastEntry: MutableEntry<K, V>? = null

            private fun computeNext(): Int {
                if (chain != null) {
                    if (++itemIndex < chain!!.size)
                        return 0
                }

                if (++keyIndex < keys.size) {
                    chain = backingMap[keys[keyIndex]]
                    itemIndex = 0
                    return 0
                }
                else {
                    chain = null
                    return 1
                }
            }

            override fun hasNext(): Boolean {
                if (state == -1)
                    state = computeNext()
                return state == 0
            }

            override fun next(): MutableEntry<K, V> {
                if (!hasNext()) throw NoSuchElementException()
                val lastEntry = chain!![itemIndex]
                this.lastEntry = lastEntry
                state = -1
                return lastEntry
            }

            override fun remove() {
                checkNotNull(lastEntry)
                this@InternalHashCodeMap.remove(lastEntry!!.key)
                lastEntry = null
                // the chain being iterated just got modified by InternalHashCodeMap.remove
                itemIndex--
            }
        }
    }

    private fun getChainOrNull(hashCode: Int): Array<MutableEntry<K, V>>? {
        val chain = backingMap[hashCode].unsafeCast<Array<MutableEntry<K, V>>?>()
        return chain.takeIf { it !== undefined }
    }

}
