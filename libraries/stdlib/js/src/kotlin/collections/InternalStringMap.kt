/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT InternalStringMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

internal external interface JsRawArray<E> {
    fun push(element: E)
    fun pop(): E

    var length: Int
}

internal inline fun <E> JsRawArray<E>.getElement(index: Int): E {
    return (this.asDynamic()[index]).unsafeCast<E>()
}

private inline fun <E> JsRawArray<E>.setElement(index: Int, element: E) {
    this.asDynamic()[index] = element
}

private inline fun <E> JsRawArray<E>.replaceElementAtWithLast(index: Int) {
    setElement(index, getElement(length - 1))
    pop()
}

/**
 * A simple wrapper around JavaScript Object for key type is string.
 *
 * Though this map is instantiated only with K=String, the K type is not fixed to String statically,
 * because we want to have it erased to Any? in order not to generate type-safe override bridges for
 * [get], [contains], [remove] etc, if they ever are generated.
 *
 * The string map has the following structure:
 * A JavaScript Object keeps a mapping from a string key to an integer index:
 *      { <key 0>: 0, <key 1>: 1, ..., <key n>: n }
 *
 * Two separate JavaScript Arrays store key-value pairs corresponding to their indexes:
 *      [<key 0>, <key 1>, ..., <key n>]
 *      [<val 0>, <val 1>, ..., <val n>]
 *
 * When adding a new key-value pair, we append them to the end of the Arrays:
 *      [<key 0>, <key 1>, ..., <key n>, <key n+1>]
 *      [<val 0>, <val 1>, ..., <val n>, <val n+1>]
 * and then set the new key with the last index in the Object:
 *      { <key 0>: 0, <key 1>: 1, ..., <key n>: n, <key n+1>: n + 1 }
 *
 * When removing a pair, we retrieve the index from the Object:
 *      { <key 0>: 0, [[[<key 1>: 1]]], ..., <key n>: n, <key n+1>: n + 1 }
 *                    ^remove <key 1>^
 * and replace the removing pair with the last pair:
 *      [<key 0>, <key n+1>, ..., <key n>]
 *      [<val 0>, <val n+1>, ..., <val n>]
 * After that, we update the moved key's index in the Object:
 *      { <key 0>: 0, <key n+1>: 1, ..., <key n>: n }
 */
internal open class InternalStringMap<K, V> : InternalMap<K, V> {
    private fun createJsMap(): dynamic {
        val result = js("Object.create(null)")
        // force to switch object representation to dictionary mode
        result["foo"] = 1
        jsDeleteProperty(result.unsafeCast<Any>(), "foo")
        return result
    }

    private fun <E> createJsArray(): JsRawArray<E> {
        return js("[]").unsafeCast<JsRawArray<E>>()
    }

    private var backingMap: dynamic = createJsMap()
    internal var values = createJsArray<V>()
    internal var keys = createJsArray<K>()

    /**
     * The number of times this map is structurally modified.
     *
     * A modification is considered to be structural if it changes the map size,
     * or otherwise changes it in a way that iterations in progress may return incorrect results.
     *
     * This value can be used by iterators of the [keys], [values] and [entries] views
     * to provide fail-fast behavior when a concurrent modification is detected during iteration.
     * [ConcurrentModificationException] will be thrown in this case.
     */
    internal var modCount: Int = 0

    private fun registerModification() {
        modCount += 1
    }

    override val size: Int
        get() = keys.length

    private fun findKeyIndex(key: K): Int? {
        if (key !is String) return null
        val index = backingMap[key]
        return if (index !== undefined) index.unsafeCast<Int>() else null
    }

    override operator fun contains(key: K): Boolean {
        return findKeyIndex(key) != null
    }

    override operator fun get(key: K): V? {
        val index = findKeyIndex(key) ?: return null
        return values.getElement(index)
    }

    override fun containsValue(value: V): Boolean {
        return values.unsafeCast<Array<V>>().contains(value)
    }

    override fun containsEntry(entry: Map.Entry<K, V>): Boolean {
        val index = findKeyIndex(entry.key) ?: return false
        return values.getElement(index) == entry.value
    }

    override fun containsOtherEntry(entry: Map.Entry<*, *>): Boolean {
        @Suppress("UNCHECKED_CAST")
        return containsEntry(entry as Map.Entry<K, V>)
    }

    override fun removeEntry(entry: Map.Entry<K, V>): Boolean {
        val index = findKeyIndex(entry.key) ?: return false
        if (values.getElement(index) == entry.value) {
            removeKeyIndex(keys.getElement(index), index)
            return true
        }
        return false
    }

    override fun removeValue(value: V): Boolean {
        val index = values.unsafeCast<Array<V>>().indexOf(value)
        if (index < 0) {
            return false
        }
        removeKeyIndex(keys.getElement(index), index)
        return true
    }

    override fun put(key: K, value: V): V? {
        require(key is String)
        val index = backingMap[key]
        if (index !== undefined) {
            val i = index.unsafeCast<Int>()
            val oldValue = values.getElement(i)
            values.setElement(i, value)
            return oldValue
        }

        backingMap[key] = size
        keys.push(key)
        values.push(value)
        registerModification()
        return null
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun remove(key: K): V? {
        val index = findKeyIndex(key) ?: return null
        val removingValue = values.getElement(index)
        removeKeyIndex(key, index)
        return removingValue
    }

    override fun removeKey(key: K): Boolean {
        val index = findKeyIndex(key) ?: return false
        removeKeyIndex(key, index)
        return true
    }

    internal open fun removeKeyIndex(key: K, removingIndex: Int) {
        jsDeleteProperty(backingMap.unsafeCast<Any>(), key as Any)

        if (removingIndex + 1 == size) {
            keys.pop()
            values.pop()
        } else {
            keys.replaceElementAtWithLast(removingIndex)
            values.replaceElementAtWithLast(removingIndex)
            backingMap[keys.getElement(removingIndex)] = removingIndex
        }
        registerModification()
    }

    override fun clear() {
        backingMap = createJsMap()
        keys = createJsArray()
        values = createJsArray()
        registerModification()
    }

    override fun build() {
        // Feel free to implement later if it is required
        throw UnsupportedOperationException("build method is not implemented")
    }

    override fun checkIsMutable() {}

    override fun keysIterator(): MutableIterator<K> = KeysItr(this)
    override fun valuesIterator(): MutableIterator<V> = ValuesItr(this)
    override fun entriesIterator(): MutableIterator<MutableEntry<K, V>> = EntriesItr(this)

    private abstract class BaseItr<K, V>(protected val map: InternalStringMap<K, V>) {
        protected var lastIndex = -1
        protected var index = 0
        private var expectedModCount = map.modCount

        protected fun goNext() {
            checkForComodification()
            if (index >= map.size) {
                throw NoSuchElementException()
            }
            lastIndex = index++
        }

        fun hasNext(): Boolean = index < map.size

        fun remove() {
            checkForComodification()
            check(lastIndex != -1) { "Call next() before removing element from the iterator." }
            map.removeKeyIndex(map.keys.getElement(lastIndex), lastIndex)
            index = lastIndex
            lastIndex = -1
            expectedModCount = map.modCount
        }

        private fun checkForComodification() {
            if (map.modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
        }
    }

    private abstract class Itr<I, K, V>(
        private val iterableArray: JsRawArray<I>,
        map: InternalStringMap<K, V>,
    ) : MutableIterator<I>, BaseItr<K, V>(map) {
        override fun next(): I {
            goNext()
            return iterableArray.getElement(lastIndex)
        }
    }

    private class KeysItr<K, V>(map: InternalStringMap<K, V>) : Itr<K, K, V>(map.keys, map)
    private class ValuesItr<K, V>(map: InternalStringMap<K, V>) : Itr<V, K, V>(map.values, map)

    private class EntriesItr<K, V>(map: InternalStringMap<K, V>) : MutableIterator<MutableEntry<K, V>>, BaseItr<K, V>(map) {
        override fun next(): MutableEntry<K, V> {
            goNext()
            val key = map.keys.getElement(lastIndex)
            val value = map.values.getElement(lastIndex)
            return EntryRef(key, value, map)
        }
    }

    protected class EntryRef<K, V>(
        override val key: K,
        override var value: V,
        private val map: InternalStringMap<K, V>,
    ) : MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val prevValue = value
            map.put(key, newValue)
            value = newValue
            return prevValue
        }

        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && other.key == key && other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
