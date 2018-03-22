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

    private var backingMap: dynamic = createJsMap()
    override var size: Int = 0
        private set

    override fun put(key: K, value: V): V? {
        val hashCode = equality.getHashCode(key)
        val chainOrEntry = getChainOrEntryOrNull(hashCode)
        if (chainOrEntry == null) {
            // This is a new chain, put it to the map.
            backingMap[hashCode] = SimpleEntry(key, value)
        }
        else {
            if (chainOrEntry !is Array<*>) {
                // It is an entry
                val entry: SimpleEntry<K, V> = chainOrEntry
                if (equality.equals(entry.key, key)) {
                    return entry.setValue(value)
                }
                else {
                    backingMap[hashCode] = arrayOf(entry, SimpleEntry(key, value))
                    size++
                    return null
                }
            }
            else {
                // Chain already exists, perhaps key also exists.
                val chain: Array<MutableEntry<K, V>> = chainOrEntry
                val entry = chain.findEntryInChain(key)
                if (entry != null) {
                    return entry.setValue(value)
                }
                chain.asDynamic().push(SimpleEntry(key, value))
            }
        }
        size++
//        structureChanged(host)
        return null
    }

    override fun remove(key: K): V? {
        val hashCode = equality.getHashCode(key)
        val chainOrEntry = getChainOrEntryOrNull(hashCode) ?: return null
        if (chainOrEntry !is Array<*>) {
            val entry: MutableEntry<K, V> = chainOrEntry
            if (equality.equals(entry.key, key)) {
                deleteProperty(backingMap, hashCode)
                size--
                return entry.value
            }
            else {
                return null
            }
        }
        else {
            val chain: Array<MutableEntry<K, V>> = chainOrEntry
            for (index in chain.indices) {
                val entry = chain[index]
                if (equality.equals(key, entry.key)) {
                    if (chain.size == 1) {
                        chain.asDynamic().length = 0
                        // remove the whole array
                        deleteProperty(backingMap, hashCode)
                    } else {
                        // splice out the entry we're removing
                        chain.asDynamic().splice(index, 1)
                    }
                    size--
//                structureChanged(host)
                    return entry.value
                }
            }
        }
        return null
    }

    override fun clear() {
        backingMap = createJsMap()
        size = 0
    }

    override fun contains(key: K): Boolean = getEntry(key) != null

    override fun get(key: K): V? = getEntry(key)?.value

    private fun getEntry(key: K): MutableEntry<K, V>? {
        val chainOrEntry = getChainOrEntryOrNull(equality.getHashCode(key)) ?: return null
        if (chainOrEntry !is Array<*>) {
            val entry: MutableEntry<K, V> = chainOrEntry
            if (equality.equals(entry.key, key)) {
                return entry
            }
            else {
                return null
            }
        }
        else {
            val chain: Array<MutableEntry<K, V>> = chainOrEntry
            return chain.findEntryInChain(key)
        }
    }

    private fun Array<MutableEntry<K, V>>.findEntryInChain(key: K): MutableEntry<K, V>? =
            firstOrNull { entry -> equality.equals(entry.key, key) }

    override fun iterator(): MutableIterator<MutableEntry<K, V>> {

        return object : MutableIterator<MutableEntry<K, V>> {
            var state = -1 // -1 not ready, 0 - ready, 1 - done

            val keys: Array<Int> = js("Object").keys(backingMap)
            var keyIndex = -1

            var chainOrEntry: dynamic = null
            var isChain = false
            var itemIndex = -1
            var lastEntry: MutableEntry<K, V>? = null

            private fun computeNext(): Int {
                if (chainOrEntry != null && isChain) {
                    val chainSize: Int = chainOrEntry.unsafeCast<Array<MutableEntry<K, V>>>().size
                    if (++itemIndex < chainSize)
                        return 0
                }

                if (++keyIndex < keys.size) {
                    chainOrEntry = backingMap[keys[keyIndex]]
                    isChain = chainOrEntry is Array<*>
                    itemIndex = 0
                    return 0
                }
                else {
                    chainOrEntry = null
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
                val lastEntry = if (isChain) {
                    chainOrEntry.unsafeCast<Array<MutableEntry<K, V>>>()[itemIndex]
                }
                else {
                    chainOrEntry.unsafeCast<MutableEntry<K, V>>()
                }
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

    private fun getChainOrEntryOrNull(hashCode: Int): dynamic {
        val chainOrEntry = backingMap[hashCode]
        return if (chainOrEntry === undefined) null else chainOrEntry
    }

}
