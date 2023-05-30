/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections.builders

import java.io.Externalizable
import java.io.InvalidObjectException
import java.io.NotSerializableException

internal class MapBuilder<K, V> private constructor(
    // keys in insert order
    private var keysArray: Array<K>,
    // values in insert order, allocated only when actually used, always null in pure HashSet
    private var valuesArray: Array<V>?,
    // hash of a key by its index, -1 if a key at that index was removed
    private var presenceArray: IntArray,
    // (index + 1) of a key by its hash, 0 if there is no key with that hash, -1 if collision chain continues to the hash-1
    private var hashArray: IntArray,
    // max length of a collision chain
    private var maxProbeDistance: Int,
    // index of the next key to be inserted
    private var length: Int
) : MutableMap<K, V>, Serializable {
    private var hashShift: Int = computeShift(hashSize)

    override var size: Int = 0
        private set

    private var keysView: MapBuilderKeys<K>? = null
    private var valuesView: MapBuilderValues<V>? = null
    private var entriesView: MapBuilderEntries<K, V>? = null

    internal var isReadOnly: Boolean = false
        private set

    // ---------------------------- functions ----------------------------

    constructor() : this(INITIAL_CAPACITY)

    constructor(initialCapacity: Int) : this(
        arrayOfUninitializedElements(initialCapacity),
        null,
        IntArray(initialCapacity),
        IntArray(computeHashSize(initialCapacity)),
        INITIAL_MAX_PROBE_DISTANCE,
        0)

    fun build(): Map<K, V> {
        checkIsMutable()
        isReadOnly = true
        @Suppress("UNCHECKED_CAST")
        return if (size > 0) this else (Empty as Map<K, V>)
    }

    private fun writeReplace(): Any =
        if (isReadOnly)
            SerializedMap(this)
        else
            throw NotSerializableException("The map cannot be serialized while it is being built.")

    override fun isEmpty(): Boolean = size == 0
    override fun containsKey(key: K): Boolean = findKey(key) >= 0
    override fun containsValue(value: V): Boolean = findValue(value) >= 0

    override operator fun get(key: K): V? {
        val index = findKey(key)
        if (index < 0) return null
        return valuesArray!![index]
    }

    override fun put(key: K, value: V): V? {
        checkIsMutable()
        val index = addKey(key)
        val valuesArray = allocateValuesArray()
        if (index < 0) {
            val oldValue = valuesArray[-index - 1]
            valuesArray[-index - 1] = value
            return oldValue
        } else {
            valuesArray[index] = value
            return null
        }
    }

    override fun putAll(from: Map<out K, V>) {
        checkIsMutable()
        putAllEntries(from.entries)
    }

    override fun remove(key: K): V? {
        val index = removeKey(key)  // mutability gets checked here
        if (index < 0) return null
        val valuesArray = valuesArray!!
        val oldValue = valuesArray[index]
        valuesArray.resetAt(index)
        return oldValue
    }

    override fun clear() {
        checkIsMutable()
        // O(length) implementation for hashArray cleanup
        for (i in 0..length - 1) {
            val hash = presenceArray[i]
            if (hash >= 0) {
                hashArray[hash] = 0
                presenceArray[i] = TOMBSTONE
            }
        }
        keysArray.resetRange(0, length)
        valuesArray?.resetRange(0, length)
        size = 0
        length = 0
    }

    override val keys: MutableSet<K> get() {
        val cur = keysView
        return if (cur == null) {
            val new = MapBuilderKeys(this)
            keysView = new
            new
        } else cur
    }

    override val values: MutableCollection<V> get() {
        val cur = valuesView
        return if (cur == null) {
            val new = MapBuilderValues(this)
            valuesView = new
            new
        } else cur
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() {
        val cur = entriesView
        return if (cur == null) {
            val new = MapBuilderEntries(this)
            entriesView = new
            return new
        } else cur
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is Map<*, *>) &&
                contentEquals(other)
    }

    override fun hashCode(): Int {
        var result = 0
        val it = entriesIterator()
        while (it.hasNext()) {
            result += it.nextHashCode()
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder(2 + size * 3)
        sb.append("{")
        var i = 0
        val it = entriesIterator()
        while (it.hasNext()) {
            if (i > 0) sb.append(", ")
            it.nextAppendString(sb)
            i++
        }
        sb.append("}")
        return sb.toString()
    }

    // ---------------------------- private ----------------------------

    // Declared internal for testing
    internal val capacity: Int get() = keysArray.size
    private val hashSize: Int get() = hashArray.size

    internal fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    private fun ensureExtraCapacity(n: Int) {
        if (shouldCompact(extraCapacity = n)) {
            rehash(hashSize)
        } else {
            ensureCapacity(length + n)
        }
    }

    private fun shouldCompact(extraCapacity: Int): Boolean {
        val spareCapacity = this.capacity - length
        val gaps = length - size
        return spareCapacity < extraCapacity                // there is no room for extraCapacity entries
                && gaps + spareCapacity >= extraCapacity    // removing gaps prevents capacity expansion
                && gaps >= this.capacity / 4                // at least 25% of current capacity is occupied by gaps
    }

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity < 0) throw OutOfMemoryError()    // overflow
        if (minCapacity > this.capacity) {
            val newSize = AbstractList.newCapacity(this.capacity, minCapacity)
            keysArray = keysArray.copyOfUninitializedElements(newSize)
            valuesArray = valuesArray?.copyOfUninitializedElements(newSize)
            presenceArray = presenceArray.copyOf(newSize)
            val newHashSize = computeHashSize(newSize)
            if (newHashSize > hashSize) rehash(newHashSize)
        }
    }

    private fun allocateValuesArray(): Array<V> {
        val curValuesArray = valuesArray
        if (curValuesArray != null) return curValuesArray
        val newValuesArray = arrayOfUninitializedElements<V>(capacity)
        valuesArray = newValuesArray
        return newValuesArray
    }

    private fun hash(key: K) = (key.hashCode() * MAGIC) ushr hashShift

    private fun compact() {
        var i = 0
        var j = 0
        val valuesArray = valuesArray
        while (i < length) {
            if (presenceArray[i] >= 0) {
                keysArray[j] = keysArray[i]
                if (valuesArray != null) valuesArray[j] = valuesArray[i]
                j++
            }
            i++
        }
        keysArray.resetRange(j, length)
        valuesArray?.resetRange(j, length)
        length = j
        //check(length == size) { "Internal invariant violated during compact: length=$length != size=$size" }
    }

    private fun rehash(newHashSize: Int) {
        if (length > size) compact()
        if (newHashSize != hashSize) {
            hashArray = IntArray(newHashSize)
            hashShift = computeShift(newHashSize)
        } else {
            hashArray.fill(0, 0, hashSize)
        }
        var i = 0
        while (i < length) {
            if (!putRehash(i++)) {
                throw IllegalStateException("This cannot happen with fixed magic multiplier and grow-only hash array. " +
                                                    "Have object hashCodes changed?")
            }
        }
    }

    private fun putRehash(i: Int): Boolean {
        var hash = hash(keysArray[i])
        var probesLeft = maxProbeDistance
        while (true) {
            val index = hashArray[hash]
            if (index == 0) {
                hashArray[hash] = i + 1
                presenceArray[i] = hash
                return true
            }
            if (--probesLeft < 0) return false
            if (hash-- == 0) hash = hashSize - 1
        }
    }

    private fun findKey(key: K): Int {
        var hash = hash(key)
        var probesLeft = maxProbeDistance
        while (true) {
            val index = hashArray[hash]
            if (index == 0) return TOMBSTONE
            if (index > 0 && keysArray[index - 1] == key) return index - 1
            if (--probesLeft < 0) return TOMBSTONE
            if (hash-- == 0) hash = hashSize - 1
        }
    }

    private fun findValue(value: V): Int {
        var i = length
        while (--i >= 0) {
            if (presenceArray[i] >= 0 && valuesArray!![i] == value)
                return i
        }
        return TOMBSTONE
    }

    internal fun addKey(key: K): Int {
        checkIsMutable()
        retry@ while (true) {
            var hash = hash(key)
            // put is allowed to grow maxProbeDistance with some limits (resize hash on reaching limits)
            val tentativeMaxProbeDistance = (maxProbeDistance * 2).coerceAtMost(hashSize / 2)
            var probeDistance = 0
            while (true) {
                val index = hashArray[hash]
                if (index <= 0) { // claim or reuse hash slot
                    if (length >= capacity) {
                        ensureExtraCapacity(1)
                        continue@retry
                    }
                    val putIndex = length++
                    keysArray[putIndex] = key
                    presenceArray[putIndex] = hash
                    hashArray[hash] = putIndex + 1
                    size++
                    if (probeDistance > maxProbeDistance) maxProbeDistance = probeDistance
                    return putIndex
                }
                if (keysArray[index - 1] == key) {
                    return -index
                }
                if (++probeDistance > tentativeMaxProbeDistance) {
                    rehash(hashSize * 2) // cannot find room even with extra "tentativeMaxProbeDistance" -- grow hash
                    continue@retry
                }
                if (hash-- == 0) hash = hashSize - 1
            }
        }
    }

    internal fun removeKey(key: K): Int {
        checkIsMutable()
        val index = findKey(key)
        if (index < 0) return TOMBSTONE
        removeKeyAt(index)
        return index
    }

    private fun removeKeyAt(index: Int) {
        keysArray.resetAt(index)
        removeHashAt(presenceArray[index])
        presenceArray[index] = TOMBSTONE
        size--
    }

    private fun removeHashAt(removedHash: Int) {
        var hash = removedHash
        var hole = removedHash // will try to patch the hole in hash array
        var probeDistance = 0
        var patchAttemptsLeft = (maxProbeDistance * 2).coerceAtMost(hashSize / 2) // don't spend too much effort
        while (true) {
            if (hash-- == 0) hash = hashSize - 1
            if (++probeDistance > maxProbeDistance) {
                // too far away -- can release the hole, bad case will not happen
                hashArray[hole] = 0
                return
            }
            val index = hashArray[hash]
            if (index == 0) {
                // end of chain -- can release the hole, bad case will not happen
                hashArray[hole] = 0
                return
            }
            if (index < 0) {
                // TOMBSTONE FOUND
                //   - <--- [ TS ] ------ [hole] ---> +
                //             \------------/
                //             probeDistance
                // move tombstone into the hole
                hashArray[hole] = TOMBSTONE
                hole = hash
                probeDistance = 0
            } else {
                val otherHash = hash(keysArray[index - 1])
                // Bad case:
                //   - <--- [hash] ------ [hole] ------ [otherHash] ---> +
                //             \------------/
                //             probeDistance
                if ((otherHash - hash) and (hashSize - 1) >= probeDistance) {
                    // move otherHash into the hole, move the hole
                    hashArray[hole] = index
                    presenceArray[index - 1] = hole
                    hole = hash
                    probeDistance = 0
                }
            }
            // check how long we're patching holes
            if (--patchAttemptsLeft < 0) {
                // just place tombstone into the hole
                hashArray[hole] = TOMBSTONE
                return
            }
        }
    }

    internal fun containsEntry(entry: Map.Entry<K, V>): Boolean {
        val index = findKey(entry.key)
        if (index < 0) return false
        return valuesArray!![index] == entry.value
    }

    private fun contentEquals(other: Map<*, *>): Boolean = size == other.size && containsAllEntries(other.entries)

    internal fun containsAllEntries(m: Collection<*>): Boolean {
        val it = m.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            try {
                @Suppress("UNCHECKED_CAST") // todo: get rid of unchecked cast here somehow
                if (entry == null || !containsEntry(entry as Map.Entry<K, V>))
                    return false
            } catch (e: ClassCastException) {
                return false
            }
        }
        return true
    }

    private fun putEntry(entry: Map.Entry<K, V>): Boolean {
        val index = addKey(entry.key)
        val valuesArray = allocateValuesArray()
        if (index >= 0) {
            valuesArray[index] = entry.value
            return true
        }
        val oldValue = valuesArray[-index - 1]
        if (entry.value != oldValue) {
            valuesArray[-index - 1] = entry.value
            return true
        }
        return false
    }

    private fun putAllEntries(from: Collection<Map.Entry<K, V>>): Boolean {
        if (from.isEmpty()) return false
        ensureExtraCapacity(from.size)
        val it = from.iterator()
        var updated = false
        while (it.hasNext()) {
            if (putEntry(it.next()))
                updated = true
        }
        return updated
    }

    internal fun removeEntry(entry: Map.Entry<K, V>): Boolean {
        checkIsMutable()
        val index = findKey(entry.key)
        if (index < 0) return false
        if (valuesArray!![index] != entry.value) return false
        removeKeyAt(index)
        return true
    }

    internal fun removeValue(element: V): Boolean {
        checkIsMutable()
        val index = findValue(element)
        if (index < 0) return false
        removeKeyAt(index)
        return true
    }

    internal fun keysIterator() = KeysItr(this)
    internal fun valuesIterator() = ValuesItr(this)
    internal fun entriesIterator() = EntriesItr(this)

    internal companion object {
        private const val MAGIC = -1640531527 // 2654435769L.toInt(), golden ratio
        private const val INITIAL_CAPACITY = 8
        private const val INITIAL_MAX_PROBE_DISTANCE = 2
        private const val TOMBSTONE = -1

        internal val Empty = MapBuilder<Nothing, Nothing>(0).also { it.isReadOnly = true }

        private fun computeHashSize(capacity: Int): Int = (capacity.coerceAtLeast(1) * 3).takeHighestOneBit()

        private fun computeShift(hashSize: Int): Int = hashSize.countLeadingZeroBits() + 1
    }

    internal open class Itr<K, V>(
        internal val map: MapBuilder<K, V>
    ) {
        internal var index = 0
        internal var lastIndex: Int = -1

        init {
            initNext()
        }

        internal fun initNext() {
            while (index < map.length && map.presenceArray[index] < 0)
                index++
        }

        fun hasNext(): Boolean = index < map.length

        fun remove() {
            check(lastIndex != -1) { "Call next() before removing element from the iterator." }
            map.checkIsMutable()
            map.removeKeyAt(lastIndex)
            lastIndex = -1
        }
    }

    internal class KeysItr<K, V>(map: MapBuilder<K, V>) : Itr<K, V>(map), MutableIterator<K> {
        override fun next(): K {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.keysArray[lastIndex]
            initNext()
            return result
        }

    }

    internal class ValuesItr<K, V>(map: MapBuilder<K, V>) : Itr<K, V>(map), MutableIterator<V> {
        override fun next(): V {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.valuesArray!![lastIndex]
            initNext()
            return result
        }
    }

    internal class EntriesItr<K, V>(map: MapBuilder<K, V>) : Itr<K, V>(map),
        MutableIterator<MutableMap.MutableEntry<K, V>> {
        override fun next(): EntryRef<K, V> {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = EntryRef(map, lastIndex)
            initNext()
            return result
        }

        internal fun nextHashCode(): Int {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val result = map.keysArray[lastIndex].hashCode() xor map.valuesArray!![lastIndex].hashCode()
            initNext()
            return result
        }

        fun nextAppendString(sb: StringBuilder) {
            if (index >= map.length) throw NoSuchElementException()
            lastIndex = index++
            val key = map.keysArray[lastIndex]
            if (key == map) sb.append("(this Map)") else sb.append(key)
            sb.append('=')
            val value = map.valuesArray!![lastIndex]
            if (value == map) sb.append("(this Map)") else sb.append(value)
            initNext()
        }
    }

    internal class EntryRef<K, V>(
        private val map: MapBuilder<K, V>,
        private val index: Int
    ) : MutableMap.MutableEntry<K, V> {
        override val key: K
            get() = map.keysArray[index]

        override val value: V
            get() = map.valuesArray!![index]

        override fun setValue(newValue: V): V {
            map.checkIsMutable()
            val valuesArray = map.allocateValuesArray()
            val oldValue = valuesArray[index]
            valuesArray[index] = newValue
            return oldValue
        }

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> &&
                    other.key == key &&
                    other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}

internal class MapBuilderKeys<E> internal constructor(
    private val backing: MapBuilder<E, *>
) : MutableSet<E>, AbstractMutableSet<E>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: E): Boolean = backing.containsKey(element)
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = backing.removeKey(element) >= 0
    override fun iterator(): MutableIterator<E> = backing.keysIterator()

    override fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

internal class MapBuilderValues<V> internal constructor(
    val backing: MapBuilder<*, V>
) : MutableCollection<V>, AbstractMutableCollection<V>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: V): Boolean = backing.containsValue(element)
    override fun add(element: V): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<V>): Boolean = throw UnsupportedOperationException()
    override fun clear() = backing.clear()
    override fun iterator(): MutableIterator<V> = backing.valuesIterator()
    override fun remove(element: V): Boolean = backing.removeValue(element)

    override fun removeAll(elements: Collection<V>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

// intermediate abstract class to workaround KT-43321
internal abstract class AbstractMapBuilderEntrySet<E : Map.Entry<K, V>, K, V> : AbstractMutableSet<E>() {
    final override fun contains(element: E): Boolean = containsEntry(element)
    abstract fun containsEntry(element: Map.Entry<K, V>): Boolean
}

internal class MapBuilderEntries<K, V> internal constructor(
    val backing: MapBuilder<K, V>
) : AbstractMapBuilderEntrySet<MutableMap.MutableEntry<K, V>, K, V>() {

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun containsEntry(element: Map.Entry<K, V>): Boolean = backing.containsEntry(element)
    override fun clear() = backing.clear()
    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean = backing.removeEntry(element)
    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = backing.entriesIterator()
    override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = backing.containsAllEntries(elements)

    override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

private class SerializedMap(
    private var map: Map<*, *>
) : Externalizable {

    constructor() : this(emptyMap<Any?, Any?>()) // for deserialization

    override fun writeExternal(output: java.io.ObjectOutput) {
        output.writeByte(0) // flags
        output.writeInt(map.size)
        for (entry in map) {
            output.writeObject(entry.key)
            output.writeObject(entry.value)
        }
    }

    override fun readExternal(input: java.io.ObjectInput) {
        val flags = input.readByte().toInt()
        if (flags != 0) {
            throw InvalidObjectException("Unsupported flags value: $flags")
        }
        val size = input.readInt()
        if (size < 0) throw InvalidObjectException("Illegal size value: $size.")
        map = buildMap<Any?, Any?>(size) {
            repeat(size) {
                val key = input.readObject()
                val value = input.readObject()
                put(key, value)
            }
        }
    }

    private fun readResolve(): Any = map

    companion object {
        private const val serialVersionUID: Long = 0L
    }
}
