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

import java.util.ConcurrentModificationDetector.structureChanged

import java.util.AbstractMap.SimpleEntry

import javaemul.internal.ArrayHelper

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
private class InternalHashCodeMap<K, V>(private val host: AbstractHashMap<K, V>) : Iterable<Entry<K, V>> {

    private val backingMap = InternalJsMapFactory.newJsMap()
    private var size: Int = 0

    fun put(key: K, value: V): V? {
        val hashCode = hash(key)
        val chain = getChainOrEmpty(hashCode)

        if (chain.size == 0) {
            // This is a new chain, put it to the map.
            backingMap.set(hashCode, chain)
        }
        else {
            // Chain already exists, perhaps key also exists.
            val entry = findEntryInChain(key, chain)
            if (entry != null) {
                return entry!!.setValue(value)
            }
        }
        chain[chain.size] = SimpleEntry<K, V>(key, value)
        size++
        structureChanged(host)
        return null
    }

    fun remove(key: Any): V? {
        val hashCode = hash(key)
        val chain = getChainOrEmpty(hashCode)
        for (i in chain.indices) {
            val entry = chain[i]
            if (host.equals(key, entry.key)) {
                if (chain.size == 1) {
                    ArrayHelper.setLength(chain, 0)
                    // remove the whole array
                    backingMap.delete(hashCode)
                }
                else {
                    // splice out the entry we're removing
                    ArrayHelper.removeFrom(chain, i, 1)
                }
                size--
                structureChanged(host)
                return entry.value
            }
        }
        return null
    }

    fun getEntry(key: Any): Entry<K, V> {
        return findEntryInChain(key, getChainOrEmpty(hash(key)))
    }

    private fun findEntryInChain(key: Any, chain: Array<Entry<K, V>>): Entry<K, V>? {
        for (entry in chain) {
            if (host.equals(key, entry.key)) {
                return entry
            }
        }
        return null
    }

    fun size(): Int {
        return size
    }

    override fun iterator(): Iterator<Entry<K, V>> {
        return object : Iterator<Entry<K, V>> {
            internal val chains = backingMap.entries()
            internal var itemIndex = 0
            internal var chain = newEntryChain()
            internal var lastEntry: Entry<K, V>? = null

            override fun hasNext(): Boolean {
                if (itemIndex < chain.size) {
                    return true
                }
                val current = chains.next()
                if (!current.done) {
                    // Move to the beginning of next chain
                    chain = unsafeCastToArray(current.getValue())
                    itemIndex = 0
                    return true
                }
                return false
            }

            override fun next(): Entry<K, V> {
                lastEntry = chain[itemIndex++]
                return lastEntry
            }

            override fun remove() {
                this@InternalHashCodeMap.remove(lastEntry!!.key)
                // Unless we are in a new chain, all items have shifted so our itemIndex should as well...
                if (itemIndex != 0) {
                    itemIndex--
                }
            }
        }
    }

    private fun getChainOrEmpty(hashCode: Int): Array<Entry<K, V>> {
        val chain = unsafeCastToArray(backingMap.get(hashCode))
        return chain ?: newEntryChain()
    }

    private fun newEntryChain(/*-{
    return [];
  }-*/): Array<Entry<K, V>>

    private fun unsafeCastToArray(arr: Any /*-{
    return arr;
  }-*/): Array<Entry<K, V>>?

    /**
     * Returns hash code of the key as calculated by [AbstractHashMap.getHashCode] but
     * also handles null keys as well.
     */
    private fun hash(key: Any?): Int {
        return if (key == null) 0 else host.getHashCode(key)
    }
}
