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
 * Based on GWT LinkedHashMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

open class LinkedHashMap<K, V> : HashMap<K, V>, Map<K, V> {

    /**
     * The entry we use includes next/prev pointers for a doubly-linked circular
     * list with a head node. This reduces the special cases we have to deal with
     * in the list operations.

     * Note that we duplicate the key from the underlying hash map so we can find
     * the eldest entry. The alternative would have been to modify HashMap so more
     * of the code was directly usable here, but this would have added some
     * overhead to HashMap, or to reimplement most of the HashMap code here with
     * small modifications. Paying a small storage cost only if you use
     * LinkedHashMap and minimizing code size seemed like a better tradeoff
     */
    private class ChainEntry<K, V>(key: K, value: V) : AbstractMap.SimpleEntry<K, V>(key, value) {
        internal var next: ChainEntry<K, V>? = null
        internal var prev: ChainEntry<K, V>? = null
    }

    private inner class EntrySet : AbstractSet<MutableEntry<K, V>>() {

        private inner class EntryIterator : MutableIterator<MutableEntry<K, V>> {
            // The last entry that was returned from this iterator.
            private var last: ChainEntry<K, V>? = null

            // The next entry to return from this iterator.
            private var next: ChainEntry<K, V>? = null

            init {
                next = head
//                recordLastKnownStructure(map, this)
            }

            override fun hasNext(): Boolean {
                return next !== null
            }

            override fun next(): MutableEntry<K, V> {
//                checkStructuralChange(map, this)
                if (!hasNext()) throw NoSuchElementException()

                val current = next!!
                last = current
                next = current.next
                if (next === head) next = null // satisfying { it != head }
                return current
            }

            override fun remove() {
                check(last != null)
//                checkStructuralChange(map, this)

                last!!.remove()
                map.remove(last!!.key)
//                recordLastKnownStructure(map, this)
                last = null
            }
        }

        override fun clear() {
            this@LinkedHashMap.clear()
        }

        override operator fun contains(element: MutableEntry<K, V>): Boolean = containsEntry(element)

        override operator fun iterator(): MutableIterator<MutableEntry<K, V>> = EntryIterator()

        override fun remove(element: MutableEntry<K, V>): Boolean {
            if (contains(element)) {
                this@LinkedHashMap.remove(element.key)
                return true
            }
            return false
        }

        override val size: Int get() = this@LinkedHashMap.size
    }


    /*
   * The head of the insert order chain, which is a doubly-linked circular
   * list.
   *
   * The most recently inserted node is at the end of the chain, ie.
   * chain.prev.
   */
    private var head: ChainEntry<K, V>? = null

    /**
     * Add this node to the end of the chain.
     */
    private fun ChainEntry<K, V>.addToEnd() {
        // This entry is not in the list.
        check(next == null && prev == null)

        val _head = head
        if (_head == null) {
            head = this
            next = this
            prev = this
        } else {
            // Chain is valid.
            val _tail = checkNotNull(_head.prev)
            // Update me.
            prev = _tail
            next = _head
            // Update my new siblings: current head and old tail
            _head.prev = this
            _tail.next = this
        }
    }

    /**
     * Remove this node from the chain it is a part of.
     */
    private fun ChainEntry<K, V>.remove() {
        if (this.next === this) {
            // if this is single element, remove head
            head = null
        }
        else {
            if (head === this) {
                // if this is first element, move head to next
                head = next
            }
            next!!.prev = prev
            prev!!.next = next
        }
        next = null
        prev = null
    }

    /*
   * The hashmap that keeps track of our entries and the chain. Note that we
   * duplicate the key here to eliminate changes to HashMap and minimize the
   * code here, at the expense of additional space.
   */
    private val map: HashMap<K, ChainEntry<K, V>>

    constructor() : super()  {
        map = HashMap<K, ChainEntry<K, V>>()
    }

    internal constructor(backingMap: HashMap<K, Any>) : super() {
        map = backingMap as HashMap<K, ChainEntry<K, V>>
    }

    constructor(initialCapacity: Int, loadFactor: Float = 0f) : super(initialCapacity, loadFactor) {
        map = HashMap<K, ChainEntry<K, V>>()
    }

    constructor(original: Map<out K, V>) {
        map = HashMap<K, ChainEntry<K, V>>()
        this.putAll(original)
    }

    override fun clear() {
        map.clear()
        head = null
    }


//    override fun clone(): Any {
//        return LinkedHashMap(this)
//    }

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean {
        var node: ChainEntry<K, V> = head ?: return false
        do {
            if (node.value == value) {
                return true
            }
            node = node.next!!
        } while (node !== head)
        return false
    }


    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = EntrySet()

    override operator fun get(key: K): V? = map.get(key)?.value

    override fun put(key: K, value: V): V? {
        val old = map.get(key)
        if (old == null) {
            val newEntry = ChainEntry(key, value)
            map.put(key, newEntry)
            newEntry.addToEnd()
            return null
        }
        else {
            return old.setValue(value)
        }
    }

    override fun remove(key: K): V? {
        val entry = map.remove(key)
        if (entry != null) {
            entry.remove()
            return entry.value
        }
        return null
    }

    override val size: Int get() = map.size

}


public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
    return LinkedHashMap<String, V>(stringMapOf<Any>()).apply { putAll(pairs) }
}