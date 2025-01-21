/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.internal.InlineOnly

internal fun <K : Any, V : Any> Map<K, V>.compact(sharedKeysFrom: CompactMap<K, V>? = null) = CompactMap(this, sharedKeysFrom)

/*
CompactMap uses cuckoo hashing (https://en.wikipedia.org/wiki/Cuckoo_hashing), and tries to pack keys into as small a table as possible,
conservatively only rehashing to an array one element larger at a time. When multiple CompactMaps share the same key set, it is possible to
share the keyTable between them, saving more memory. The magic constants in the hash2 function are chosen so that the keys in
ClassReference.Companion.classFqNames pack perfectly into an array of size equal to the number of keys.

Caveat: CompactMap will not work, if multiple keys in the src map have identical hashCode().
 */

@Suppress("UNCHECKED_CAST")
internal class CompactMap<K : Any, V : Any>(src: Map<K, V>, sharedKeysFrom: CompactMap<K, V>? = null) : Map<K, V> {
    private val keyTable: Array<K?> = sharedKeysFrom?.keyTable ?: buildTable(src)
    private val valueTable: Array<V?> = Array<Any?>(keyTable.size) { src[keyTable[it]] } as Array<V?>

    @InlineOnly
    inline fun hash1(code: Int, mod: Int) = code.and(Int.MAX_VALUE) % mod

    @InlineOnly
    inline fun hash2(code: Int, mod: Int) = (code * -553849527).shr(10).and(Int.MAX_VALUE) % mod

    private fun buildTable(src: Map<K, V>): Array<K?> {
        var size = src.size
        outer@ while (size <= 4 * src.size) {
            val table = Array<Any?>(size) { null } as Array<K?>
            for (key in src.keys) {
                var index = hash1(key.hashCode(), size)
                var cur = key
                var relocations = 0
                while (true) {
                    if (relocations++ > size) {
                        ++size
                        continue@outer
                    }
                    val elm = table[index]
                    if (elm == null) {
                        table[index] = cur
                        break
                    }
                    table[index] = cur
                    cur = elm
                    val code = cur.hashCode()
                    index = hash1(code, size) + hash2(code, size) - index
                }
            }
            return table
        }
        throw IllegalStateException("Could not build CompactMap")
    }

    override val size: Int
        get() = keyTable.count { it != null }

    override fun isEmpty(): Boolean = keyTable.isEmpty()

    override fun containsKey(key: K) = keyTable.contains(key)

    override fun containsValue(value: V) = valueTable.contains(value)

    override operator fun get(key: K): V? {
        val code = key.hashCode()
        val hash1 = hash1(code, keyTable.size)
        if (key == keyTable[hash1]) return valueTable[hash1]
        val hash2 = hash2(code, keyTable.size)
        if (key == keyTable[hash2]) return valueTable[hash2]
        return null
    }

    override val keys: Set<K> get() = keyTable.filterNotNull().toSet()

    override val values: Collection<V> get() = valueTable.filterNotNull().toSet()

    private data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

    override val entries: Set<Map.Entry<K, V>>
        get() = keys.map { Entry(it, get(it)!!) }.toSet()
}
