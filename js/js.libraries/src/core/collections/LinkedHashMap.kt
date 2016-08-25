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
    private inner class ChainEntry @JvmOverloads constructor(key: K? = null, value: V? = null) : AbstractMap.SimpleEntry<K, V>(key, value) {
        @Transient private var next: ChainEntry? = null
        @Transient private var prev: ChainEntry? = null

        /**
         * Add this node to the end of the chain.
         */
        fun addToEnd() {
            val tail = head.prev

            // Chain is valid.
            assert(head != null && tail != null)

            // This entry is not in the list.
            assert(next == null && prev == null)

            // Update me.
            prev = tail
            next = head
            tail!!.next = head.prev = this
        }

        /**
         * Remove this node from any list it may be a part of.
         */
        fun remove() {
            next!!.prev = prev
            prev!!.next = next
            next = prev = null
        }
    }

    private inner class EntrySet : AbstractSet<Entry<K, V>>() {

        private inner class EntryIterator : Iterator<Entry<K, V>> {
            // The last entry that was returned from this iterator.
            private var last: ChainEntry? = null

            // The next entry to return from this iterator.
            private var next: ChainEntry? = null

            init {
                next = head.next
                recordLastKnownStructure(map, this)
            }

            override fun hasNext(): Boolean {
                return next !== head
            }

            override fun next(): Entry<K, V> {
                checkStructuralChange(map, this)
                checkCriticalElement(hasNext())

                last = next
                next = next!!.next
                return last
            }

            override fun remove() {
                checkState(last != null)
                checkStructuralChange(map, this)

                last!!.remove()
                map.remove(last!!.key)
                recordLastKnownStructure(map, this)
                last = null
            }
        }

        override fun clear() {
            this@LinkedHashMap.clear()
        }

        override operator fun contains(o: Any?): Boolean {
            if (o is Entry<*, *>) {
                return containsEntry(o as Entry<*, *>?)
            }
            return false
        }

        override operator fun iterator(): Iterator<Entry<K, V>> {
            return EntryIterator()
        }

        override fun remove(entry: Any?): Boolean {
            if (contains(entry)) {
                val key = (entry as Entry<*, *>).key
                this@LinkedHashMap.remove(key)
                return true
            }
            return false
        }

        override fun size(): Int {
            return this@LinkedHashMap.size
        }
    }

    // True if we should use the access order (ie, for LRU caches) instead of
    // insertion order.
    @Transient private val accessOrder: Boolean

    /*
   * The head of the LRU/insert order chain, which is a doubly-linked circular
   * list. The key and value of head should never be read.
   *
   * The most recently inserted/accessed node is at the end of the chain, ie.
   * chain.prev.
   */
    @Transient private val head = ChainEntry()

    /*
   * The hashmap that keeps track of our entries and the chain. Note that we
   * duplicate the key here to eliminate changes to HashMap and minimize the
   * code here, at the expense of additional space.
   */
    @Transient private val map = HashMap<K, ChainEntry>()

    constructor() {
        resetChainEntries()
    }

    @JvmOverloads constructor(ignored: Int, alsoIgnored: Float = 0f) : super(ignored, alsoIgnored) {
        resetChainEntries()
    }

    constructor(ignored: Int, alsoIgnored: Float, accessOrder: Boolean) : super(ignored, alsoIgnored) {
        this.accessOrder = accessOrder
        resetChainEntries()
    }

    constructor(toBeCopied: Map<out K, V>) {
        resetChainEntries()
        this.putAll(toBeCopied)
    }

    override fun clear() {
        map.clear()
        resetChainEntries()
    }

    private fun resetChainEntries() {
        head.prev = head
        head.next = head
    }

    override fun clone(): Any {
        return LinkedHashMap(this)
    }

    override fun containsKey(key: Any?): Boolean {
        return map.containsKey(key)
    }

    override fun containsValue(value: Any?): Boolean {
        var node: ChainEntry = head.next
        while (node !== head) {
            if (node.value == value) {
                return true
            }
            node = node.next
        }
        return false
    }

    override fun entrySet(): Set<Entry<K, V>> {
        return EntrySet()
    }

    override operator fun get(key: Any?): V? {
        val entry = map.get(key)
        if (entry != null) {
            recordAccess(entry)
            return entry!!.value
        }
        return null
    }

    override fun put(key: K?, value: V?): V? {
        val old = map.get(key)
        if (old == null) {
            val newEntry = ChainEntry(key, value)
            map.put(key, newEntry)
            newEntry.addToEnd()
            val eldest = head.next
            if (removeEldestEntry(eldest)) {
                eldest!!.remove()
                map.remove(eldest.key)
            }
            return null
        }
        else {
            val oldValue = old!!.setValue(value)
            recordAccess(old)
            return oldValue
        }
    }

    override fun remove(key: Any?): V? {
        val entry = map.remove(key)
        if (entry != null) {
            entry!!.remove()
            return entry!!.value
        }
        return null
    }

    override fun size(): Int {
        return map.size
    }

    @SuppressWarnings("unused")
    protected fun removeEldestEntry(eldest: Entry<K, V>): Boolean {
        return false
    }

    private fun recordAccess(entry: ChainEntry) {
        if (accessOrder) {
            // Move to the tail of the chain on access.
            entry.remove()
            entry.addToEnd()
        }
    }
}
