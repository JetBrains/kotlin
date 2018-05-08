/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT InternalStringMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

/**
 * A simple wrapper around JavaScript Map for key type is string.
 *
 * Though this map is instantiated only with K=String, the K type is not fixed to String statically,
 * because we want to have it erased to Any? in order not to generate type-safe override bridges for
 * [get], [contains], [remove] etc, if they ever are generated.
 */
internal class InternalStringMap<K, V>(override val equality: EqualityComparator) : InternalMap<K, V> {

    private var backingMap: dynamic = createJsMap()
    override var size: Int = 0
        private set

//    /**
//     * A mod count to track 'value' replacements in map to ensure that the 'value' that we have in the
//     * iterator entry is guaranteed to be still correct.
//     * This is to optimize for the common scenario where the values are not modified during
//     * iterations where the entries are never stale.
//     */
//    private var valueMod: Int = 0

    override operator fun contains(key: K): Boolean {
        if (key !is String) return false
        return backingMap[key] !== undefined
    }

    override operator fun get(key: K): V? {
        if (key !is String) return null
        val value = backingMap[key]
        return if (value !== undefined) value.unsafeCast<V>() else null
    }


    override fun put(key: K, value: V): V? {
        require(key is String)
        val oldValue = backingMap[key]
        backingMap[key] = value

        if (oldValue === undefined) {
            size++
//            structureChanged(host)
            return null
        } else {
//            valueMod++
            return oldValue.unsafeCast<V>()
        }
    }

    override fun remove(key: K): V? {
        if (key !is String) return null
        val value = backingMap[key]
        if (value !== undefined) {
            deleteProperty(backingMap, key)
            size--
//            structureChanged(host)
            return value.unsafeCast<V>()
        } else {
//            valueMod++
            return null
        }
    }


    override fun clear() {
        backingMap = createJsMap()
        size = 0
    }


    override fun iterator(): MutableIterator<MutableEntry<K, V>> {
        return object : MutableIterator<MutableEntry<K, V>> {
            private val keys: Array<String> = js("Object").keys(backingMap)
            private val iterator = keys.iterator()
            private var lastKey: String? = null

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): MutableEntry<K, V> {
                val key = iterator.next()
                lastKey = key
                @Suppress("UNCHECKED_CAST")
                return newMapEntry(key as K)
            }

            override fun remove() {
                @Suppress("UNCHECKED_CAST")
                this@InternalStringMap.remove(checkNotNull(lastKey) as K)
            }
        }
    }

    private fun newMapEntry(key: K): MutableEntry<K, V> = object : MutableEntry<K, V> {
        override val key: K get() = key
        override val value: V get() = this@InternalStringMap[key].unsafeCast<V>()

        override fun setValue(newValue: V): V = this@InternalStringMap.put(key, newValue).unsafeCast<V>()

        override fun hashCode(): Int = AbstractMap.entryHashCode(this)
        override fun toString(): String = AbstractMap.entryToString(this)
        override fun equals(other: Any?): Boolean = AbstractMap.entryEquals(this, other)
    }
}
