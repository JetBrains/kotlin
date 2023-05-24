/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Implementation of the [MutableMap] interface using the JavaScript built-in [Map](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map) defined in ECMAScript 6.
 *
 * Important: Unlike most other implementations, it compares keys by identity, not equality (i.e. not with [Any.equals]). See [documentation](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map#key_equality) for details of key equality.
 *
 * It keeps the insertion order when enumerating with [keys], [values] and [entries].
 * */
class NativeMap<K, V> : AbstractMutableMap<K, V>, MutableMap<K, V> {
    private val map: Es6Map<K, V>
    private var isReadOnly = false

    /**
     * Constructs an empty [NativeMap] instance.
     */
    constructor() : super() {
        map = Es6Map()
    }

    /**
     * Constructs an instance of [NativeMap] filled with the contents of the specified [original] map.
     */
    constructor(original: Map<out K, V>) : super() {
        if (original is NativeMap<*, *>) {
            map = Es6Map(original.map)
        } else {
            map = Es6Map()
            putAll(original)
        }
    }

    override val size get() = map.size

    private var _entries: EntrySet? = null
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = _entries ?: EntrySet().also { _entries = it }
    private var _keys: KeySet? = null
    override val keys: MutableSet<K>
        get() = _keys ?: KeySet().also { _keys = it }
    private var _values: ValueCollection? = null
    override val values: MutableCollection<V>
        get() = _values ?: ValueCollection().also { _values = it }

    override fun containsKey(key: K) = map.has(key)

    override operator fun get(key: K): V? = map.get(key).takeUnless { it == undefined }

    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * Note: Because of the [MutableMap] interface specification, this function must first lookup the present
     * value for the key before setting new one. To avoid this overhead, you may use [putFast] instead.
     */
    override fun put(key: K, value: V): V? {
        checkIsMutable()
        val prev = this[key]
        map.set(key, value)
        return prev
    }

    /**
     * Associates the specified [value] with the specified [key] in the map.
     * Same as [put] but does not lookup and return the previous value.
     */
    fun putFast(key: K, value: V) {
        checkIsMutable()
        map.set(key, value)
    }

    // To avoid using long-path [put]
    @kotlin.internal.InlineOnly
    inline operator fun set(key: K, value: V) {
        putFast(key, value)
    }

    /**
     * Updates this map with key/value pairs from the specified map [from].
     */
    override fun putAll(from: Map<out K, V>) {
        checkIsMutable()
        for ((key, value) in from.entries) {
            putFast(key, value)
        }
    }

    /**
     * Puts all the given [pairs] into this [NativeMap] with the first component in the pair being the key and the second the value.
     */
    fun putAll(pairs: Array<out Pair<K, V>>) {
        checkIsMutable()
        for ((key, value) in pairs) {
            putFast(key, value)
        }
    }

    /**
     * Puts all the elements of the given collection into this [NativeMap] with the first component in the pair being the key and the second the value.
     */
    fun putAll(pairs: Iterable<Pair<K, V>>) {
        checkIsMutable()
        for ((key, value) in pairs) {
            putFast(key, value)
        }
    }

    /**
     * Puts all the elements of the given sequence into this [NativeMap] with the first component in the pair being the key and the second the value.
     */
    fun putAll(pairs: Sequence<Pair<K, V>>) {
        checkIsMutable()
        for ((key, value) in pairs) {
            putFast(key, value)
        }
    }

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * Note: Because of the [MutableMap] interface specification, this function must first lookup the present
     * value for the key before removing it. To avoid this overhead, you may use [removeFast] instead.
     */
    override fun remove(key: K): V? {
        checkIsMutable()
        val prev = map.get(key)
        if (prev != undefined) {
            map.delete(key)
            return prev
        }
        return null
    }

    /**
     * Removes the specified key and its corresponding value from this map.
     * Same as [remove] but does not lookup and return the previous value.
     *
     * @return true if an element existed and has been removed, or false if the element does not exist.
     */
    fun removeFast(key: K): Boolean {
        checkIsMutable()
        return map.delete(key)
    }

    override fun clear() {
        checkIsMutable()
        map.clear()
    }

    @PublishedApi
    internal fun build(): Map<K, V> {
        checkIsMutable()
        isReadOnly = true
        return this
    }

    override fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    // Note this implementation is quite heavy-weighted as it holds reference to the map and queries the value each time.
    // Thus there is also [ReadonlyEntry] is case we can be sure no more modifications will be made.
    private inner class MutableEntry(override val key: K, value: V) : MutableMap.MutableEntry<K, V> {
        private var lastValue = value
        override val value: V
            get() = this@NativeMap[key]?.also { lastValue = it } ?: lastValue

        override fun setValue(newValue: V): V {
            val oldValue = this.value
            this@NativeMap[key] = newValue
            return oldValue
        }

        override fun hashCode(): Int = entryHashCode(this)
        override fun toString(): String = entryToString(this)
        override fun equals(other: Any?): Boolean = entryEquals(this, other)
    }

    private class ReadonlyEntry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("The map is readonly")

        override fun hashCode(): Int = entryHashCode(this)
        override fun toString(): String = entryToString(this)
        override fun equals(other: Any?): Boolean = entryEquals(this, other)
    }

    private inner class EntrySet : AbstractMutableMap.AbstractEntrySet<MutableMap.MutableEntry<K, V>, K, V>() {
        override val size get() = this@NativeMap.size

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
            throw UnsupportedOperationException("Add is not supported on entries")

        override fun containsEntry(element: Map.Entry<K, V>): Boolean = this@NativeMap.containsEntry(element)

        override fun removeEntry(element: Map.Entry<K, V>): Boolean {
            if (contains(element)) {
                this@NativeMap.removeFast(element.key)
                return true
            }
            return false
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = IteratorImpl()

        private inner class IteratorImpl() : Es6IteratorAdapterWithLast<dynamic>(map.entries().unsafeCast<Es6Iterator<dynamic>>()),
            MutableIterator<MutableMap.MutableEntry<K, V>> {
            override fun next(): MutableMap.MutableEntry<K, V> {
                val entry = super.next()
                val key = entry[0].unsafeCast<K>()
                val value = entry[1].unsafeCast<V>()
                // When we know that the map won't change anymore, return the lightweight entry impl
                // which does not have to keep track of nor allow mutations.
                return if (isReadOnly) ReadonlyEntry(key, value) else MutableEntry(key, value)
            }

            override fun remove() {
                this@NativeMap.removeFast(lastValue!![0].unsafeCast<K>())
            }
        }
    }

    private inner class KeySet : AbstractMutableSet<K>() {
        override val size get() = this@NativeMap.size

        override fun add(element: K): Boolean = throw UnsupportedOperationException("Add is not supported on keys")

        override fun contains(element: K) = containsKey(element)

        override fun remove(element: K): Boolean {
            return this@NativeMap.removeFast(element)
        }

        override fun removeAll(elements: Collection<K>): Boolean {
            var removed = false
            for (element in elements) {
                if (this@NativeMap.removeFast(element)) {
                    removed = true
                }
            }
            return removed
        }

        override fun clear() {
            this@NativeMap.clear()
        }

        override fun iterator(): MutableIterator<K> = IteratorImpl()

        private inner class IteratorImpl() : Es6IteratorAdapterWithLast<K>(map.keys()), MutableIterator<K> {
            override fun remove() {
                this@NativeMap.removeFast(lastValue!!)
            }
        }
    }

    private inner class ValueCollection : AbstractMutableCollection<V>(), MutableCollection<V> {
        override val size get() = this@NativeMap.size

        override fun add(element: V): Boolean = throw UnsupportedOperationException("Add is not supported on values")

        override fun contains(element: V) = containsValue(element)

        override fun clear() {
            this@NativeMap.clear()
        }

        override fun iterator(): MutableIterator<V> = IteratorImpl()

        private inner class IteratorImpl() : Es6IteratorAdapterWithLast<dynamic>(map.entries().unsafeCast<Es6Iterator<dynamic>>()),
            MutableIterator<V> {
            override fun next(): V {
                return super.next()[1].unsafeCast<V>()
            }

            override fun remove() {
                this@NativeMap.removeFast(lastValue!![0].unsafeCast<K>())
            }
        }
    }
}

/**
 * Returns an empty new [NativeMap].
 */
@kotlin.internal.InlineOnly
inline fun <K, V> nativeMapOf() = NativeMap<K, V>()

/**
 * Returns a new [NativeMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 */
fun <K, V> nativeMapOf(vararg pairs: Pair<K, V>) = NativeMap<K, V>().apply { putAll(pairs) }