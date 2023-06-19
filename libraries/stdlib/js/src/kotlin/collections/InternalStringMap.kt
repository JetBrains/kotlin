/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

private inline fun <E> JsRawArray<E>.getElement(index: Int): E {
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
 * A simple wrapper around JavaScript Map for key type is string.
 *
 * Though this map is instantiated only with K=String, the K type is not fixed to String statically,
 * because we want to have it erased to Any? in order not to generate type-safe override bridges for
 * [get], [contains], [remove] etc, if they ever are generated.
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
    private var values = createJsArray<V>()
    private var keys = createJsArray<K>()

    override val size: Int
        get() = keys.length

    override operator fun contains(key: K): Boolean {
        if (key !is String) return false
        return backingMap[key] !== undefined
    }

    override operator fun get(key: K): V? {
        if (key !is String) return null
        val index = backingMap[key]
        return if (index !== undefined) values.getElement(index.unsafeCast<Int>()) else null
    }

    override fun getEntry(entry: Map.Entry<K, V>): MutableEntry<K, V>? {
        val key = entry.key as? String ?: return null
        val index = backingMap[key]
        if (index === undefined) {
            return null
        }
        val value = values.getElement(index.unsafeCast<Int>())
        if (value == entry.value) {
            return EntryRef(entry.key, value, this)
        }
        return null
    }

    override fun containsValue(value: V): Boolean {
        return values.unsafeCast<Array<V>>().contains(value)
    }

    override fun containsEntry(entry: Map.Entry<K, V>): Boolean {
        return getEntry(entry) != null
    }

    override fun containsOtherEntry(entry: Map.Entry<*, *>): Boolean {
        @Suppress("UNCHECKED_CAST")
        return containsEntry(entry as Map.Entry<K, V>)
    }

    override fun removeEntry(entry: Map.Entry<K, V>): Boolean {
        val key = getEntry(entry)?.key ?: return false
        return remove(key) != null
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
        return null
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun remove(key: K): V? {
        if (key !is String) return null
        val index = backingMap[key]
        if (index === undefined) {
            return null
        }

        val i = index.unsafeCast<Int>()
        val removedValue = values.getElement(i)

        removeKeyIndex(key, i)
        return removedValue
    }

    internal fun removeKeyIndex(key: K, index: Int) {
        jsDeleteProperty(backingMap.unsafeCast<Any>(), key as Any)

        if (index + 1 == size) {
            keys.pop()
            values.pop()
        } else {
            keys.replaceElementAtWithLast(index)
            values.replaceElementAtWithLast(index)
            backingMap[keys.getElement(index)] = index
        }
    }

    override fun clear() {
        backingMap = createJsMap()
        keys = createJsArray()
        values = createJsArray()
    }

    override fun keysIterator(): MutableIterator<K> = KeysItr(this)
    override fun valuesIterator(): MutableIterator<V> = ValuesItr(this)
    override fun entriesIterator(): MutableIterator<MutableEntry<K, V>> = EntriesItr(this)

    private abstract class BaseItr<K, V>(protected val map: InternalStringMap<K, V>) {
        protected var lastIndex = -1
        protected var index = 0

        protected fun goNext() {
            if (index >= map.size) {
                throw NoSuchElementException()
            }
            lastIndex = index++
        }

        fun hasNext(): Boolean = index < map.size

        fun remove() {
            map.removeKeyIndex(map.keys.getElement(lastIndex), lastIndex)
            lastIndex = -1
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

    private class EntryRef<K, V>(
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

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> &&
                    other.key == key &&
                    other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
