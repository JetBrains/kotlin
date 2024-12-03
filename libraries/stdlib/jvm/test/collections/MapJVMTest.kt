/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Function
import kotlin.test.*

class MapJVMTest {
    @Test fun createSortedMap() {
        val map = sortedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(listOf("a", "b", "c"), map.keys.toList())
    }

    @Test fun createSortedMapWithComparator() {
        val map = sortedMapOf(compareBy<String> { it.length }.thenBy { it }, Pair("c", 3), Pair("bc", 2), Pair("bd", 4), Pair("abc", 1))
        assertEquals(1, map["abc"])
        assertEquals(2, map["bc"])
        assertEquals(3, map["c"])
        assertEquals(4, map["bd"])
        assertEquals(listOf("c", "bc", "bd", "abc"), map.keys.toList())
    }

    @Test fun toSortedMap() {
        val map = mapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        val sorted = map.toSortedMap()
        assertEquals(1, sorted["a"])
        assertEquals(2, sorted["b"])
        assertEquals(3, sorted["c"])
        assertEquals(listOf("a", "b", "c"), sorted.keys.toList())
    }

    @Test fun toSortedMapWithComparator() {
        val map = mapOf(Pair("c", 3), Pair("bc", 2), Pair("bd", 4), Pair("abc", 1))
        val sorted = map.toSortedMap(compareBy<String> { it.length }.thenBy { it })
        assertEquals(listOf("c", "bc", "bd", "abc"), sorted.keys.toList())
        assertEquals(1, sorted["abc"])
        assertEquals(2, sorted["bc"])
        assertEquals(3, sorted["c"])
    }

    @Test fun toProperties() {
        val map = mapOf("a" to "A", "b" to "B")
        val prop = map.toProperties()
        assertEquals(2, prop.size)
        assertEquals("A", prop.getProperty("a", "fail"))
        assertEquals("B", prop.getProperty("b", "fail"))
    }

    @Test fun iterateAndRemove() {
        val map = (1..5).associateByTo(linkedMapOf(), { it }, { 'a' + it })
        val iterator = map.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key % 2 == 0)
                iterator.remove()
        }
        assertEquals(listOf(1, 3, 5), map.keys.toList())
        assertEquals(listOf('b', 'd', 'f'), map.values.toList())
    }

    @Test
    fun getOrPutOnConcurrentHashMap() {
        val map = ConcurrentHashMap<String?, String?>()

        assertEquals("v1", map.getOrPut("k1") { "v1" })
        assertEquals("v1", map.getOrPut("k1") { "newV1" })
        // Doesn't throw because defaultValue() wasn't called or its result wasn't tried to be put
        assertEquals("v1", map.getOrPut("k1") { null })

        // Doesn't support null values
        assertFailsWith<NullPointerException> {
            map.getOrPut("k2") { null }
        }
        assertFalse(map.containsKey("k2"))
        assertEquals("v2", map.getOrPut("k2") { "v2" })
        // Doesn't throw because defaultValue() wasn't called or its result wasn't tried to be put
        assertEquals("v2", map.getOrPut("k2") { null })

        // Doesn't support null keys
        assertFailsWith<NullPointerException> {
            map.getOrPut(null) { "v3" }
        }
        // Doesn't support null keys and values
        assertFailsWith<NullPointerException> {
            map.getOrPut(null) { null }
        }

        val expected = setOf(
            "k1" to "v1",
            "k2" to "v2"
        )
        assertEquals(expected, map.entries.map { it.toPair() }.toSet())
    }

    @Test
    fun getOrPutOnConcurrentMap() {
        val map = SimpleConcurrentMap<String?, String?>()

        assertEquals("v1", map.getOrPut("k1") { "v1" })
        assertEquals("v1", map.getOrPut("k1") { "newV1" })
        assertEquals("v1", map.getOrPut("k1") { null })

        assertEquals(null, map.getOrPut("k2") { null })
        assertTrue(map.containsKey("k2"))
        assertEquals("v2", map.getOrPut("k2") { "v2" }) // replace null value
        assertEquals("v2", map.getOrPut("k2") { null })

        assertEquals("v3", map.getOrPut(null) { "v3" })
        assertEquals("v3", map.getOrPut(null) { "newV3" })
        assertEquals("v3", map.getOrPut(null) { null })

        val expected = listOf(
            "k1" to "v1",
            "k2" to "v2",
            null to "v3"
        )
        assertContentEquals(expected, map.entries.map { it.toPair() })
    }
}


// Allows null values. Not actually multi-thread safe.
private class SimpleConcurrentMap<K, V> : AbstractMutableMap<K, V>(), ConcurrentMap<K, V> {
    private val backing = mutableMapOf<K, V>()

    override val size: Int get() = backing.size

    override fun get(key: K): V? =
        backing[key]

    override fun getOrDefault(key: K, defaultValue: V): V =
        if (backing.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            backing[key] as V
        } else {
            defaultValue
        }

    override fun put(key: K, value: V): V? =
        backing.put(key, value)

    override fun putIfAbsent(key: K, value: V): V? =
        if (!backing.containsKey(key)) {
            backing.put(key, value)
        } else {
            backing.get(key)
        }

    // Implementations which support null values must override this default implementation.
    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V =
        backing.get(key) ?: run {
            val newValue = mappingFunction.apply(key)
            if (newValue != null) {
                backing.put(key, newValue)
            }
            newValue
        }

    override fun remove(key: K, value: V): Boolean =
        backing.remove(key, value)

    override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        if (backing.containsKey(key) && backing[key] == oldValue) {
            backing.put(key, newValue)
            true
        } else {
            false
        }

    override fun replace(key: K, value: V): V? =
        if (backing.containsKey(key)) {
            backing.put(key, value)
        } else {
            null
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = backing.entries
}