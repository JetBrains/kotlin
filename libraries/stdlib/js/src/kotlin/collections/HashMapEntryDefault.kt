/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal class HashMapKeysDefault<K, V>(private val backingMap: AbstractMutableMap<K, V>) : AbstractMutableSet<K>() {
    override fun add(element: K): Boolean = throw UnsupportedOperationException("Add is not supported on keys")
    override fun clear() = backingMap.clear()
    override operator fun contains(element: K): Boolean = backingMap.containsKey(element)

    override operator fun iterator(): MutableIterator<K> {
        val entryIterator = backingMap.entries.iterator()
        return object : MutableIterator<K> {
            override fun hasNext(): Boolean = entryIterator.hasNext()
            override fun next(): K = entryIterator.next().key
            override fun remove() = entryIterator.remove()
        }
    }

    override fun remove(element: K): Boolean {
        checkIsMutable()
        if (backingMap.containsKey(element)) {
            backingMap.remove(element)
            return true
        }
        return false
    }

    override val size: Int get() = backingMap.size

    override fun checkIsMutable(): Unit = backingMap.checkIsMutable()
}

internal class HashMapValuesDefault<K, V>(private val backingMap: AbstractMutableMap<K, V>) : AbstractMutableCollection<V>() {
    override fun add(element: V): Boolean = throw UnsupportedOperationException("Add is not supported on values")
    override fun clear() = backingMap.clear()

    override operator fun contains(element: V): Boolean = backingMap.containsValue(element)

    override operator fun iterator(): MutableIterator<V> {
        val entryIterator = backingMap.entries.iterator()
        return object : MutableIterator<V> {
            override fun hasNext(): Boolean = entryIterator.hasNext()
            override fun next(): V = entryIterator.next().value
            override fun remove() = entryIterator.remove()
        }
    }

    override val size: Int get() = backingMap.size

    override fun checkIsMutable(): Unit = backingMap.checkIsMutable()
}
