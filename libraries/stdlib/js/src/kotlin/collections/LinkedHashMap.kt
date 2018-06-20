/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT LinkedHashMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

/**
 * Hash table based implementation of the [MutableMap] interface, which additionally preserves the insertion order
 * of entries during the iteration.
 *
 * The insertion order is preserved by maintaining a doubly-linked list of all of its entries.
 */
public actual open class LinkedHashMap<K, V> : HashMap<K, V>, MutableMap<K, V> {

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
    private class ChainEntry<K, V>(key: K, value: V) : AbstractMutableMap.SimpleEntry<K, V>(key, value) {
        internal var next: ChainEntry<K, V>? = null
        internal var prev: ChainEntry<K, V>? = null
    }

    private inner class EntrySet : AbstractMutableSet<MutableEntry<K, V>>() {

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
                next = current.next.takeIf { it !== head }
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

        override fun add(element: MutableEntry<K, V>): Boolean = throw UnsupportedOperationException("Add is not supported on entries")
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
        } else {
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

    /**
     * Constructs an empty [LinkedHashMap] instance.
     */
    actual constructor() : super() {
        map = HashMap<K, ChainEntry<K, V>>()
    }

    internal constructor(backingMap: HashMap<K, Any>) : super() {
        @Suppress("UNCHECKED_CAST") // expected to work due to erasure
        map = backingMap as HashMap<K, ChainEntry<K, V>>
    }

    /**
     * Constructs an empty [LinkedHashMap] instance.
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    actual constructor(initialCapacity: Int, loadFactor: Float = 0.0f) : super(initialCapacity, loadFactor) {
        map = HashMap<K, ChainEntry<K, V>>()
    }

    actual constructor(initialCapacity: Int) : this(initialCapacity, 0.0f)

    /**
     * Constructs an instance of [LinkedHashMap] filled with the contents of the specified [original] map.
     */
    actual constructor(original: Map<out K, V>) {
        map = HashMap<K, ChainEntry<K, V>>()
        this.putAll(original)
    }

    actual override fun clear() {
        map.clear()
        head = null
    }


//    override fun clone(): Any {
//        return LinkedHashMap(this)
//    }


    actual override fun containsKey(key: K): Boolean = map.containsKey(key)

    actual override fun containsValue(value: V): Boolean {
        var node: ChainEntry<K, V> = head ?: return false
        do {
            if (node.value == value) {
                return true
            }
            node = node.next!!
        } while (node !== head)
        return false
    }


    override fun createEntrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = EntrySet()

    actual override operator fun get(key: K): V? = map.get(key)?.value

    actual override fun put(key: K, value: V): V? {
        val old = map.get(key)
        if (old == null) {
            val newEntry = ChainEntry(key, value)
            map.put(key, newEntry)
            newEntry.addToEnd()
            return null
        } else {
            return old.setValue(value)
        }
    }

    actual override fun remove(key: K): V? {
        val entry = map.remove(key)
        if (entry != null) {
            entry.remove()
            return entry.value
        }
        return null
    }

    actual override val size: Int get() = map.size

}

/**
 * Constructs the specialized implementation of [LinkedHashMap] with [String] keys, which stores the keys as properties of
 * JS object without hashing them.
 */
public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
    return LinkedHashMap<String, V>(stringMapOf<Any>()).apply { putAll(pairs) }
}