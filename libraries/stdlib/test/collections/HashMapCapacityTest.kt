/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

/**
 * Storage-management edge cases of the open-addressing hash maps (MapBuilder on JVM, HashMap on
 * Native/Wasm, InternalHashMap on JS) that the existing suites leave unexercised: clearing a
 * gapped table, the capacity-overflow fail-fast, and null keys/values across lookup, hashing,
 * and printing. (Growth, rehash, and gap-compaction paths are already driven by
 * MutableMapRemoveHashAtTest's stress rounds, HashMapCompactTest, and the platform suites.)
 * KT-83662.
 *
 * Keys are plain Ints; see HashMapProbingTest for the slot layout at the default capacity 8.
 */
class HashMapCapacityTest {

    /**
     * clear() releases every live entry of a gapped table (removed entries are skipped by the
     * hash-array cleanup loop), clearing an untouched map walks the empty storage, and the
     * cleared map remains fully usable.
     */
    @Test
    fun clearReleasesGappedStorage() = testOnMaps {
        clear() // clearing an untouched map walks the empty storage
        for (k in intArrayOf(4, 25, 38, 1, 22)) put(k, k)
        remove(25)
        clear()
        assertEquals(0, size)
        assertFalse(containsKey(4))
        put(4, 40)
        assertEquals(40, get(4))
        assertEquals(1, size)
    }

    /**
     * A putAll whose source claims more entries than can exist must fail fast on the capacity
     * overflow instead of corrupting the map, and the map must stay intact and usable. The
     * JVM, Native, and Wasm throw OutOfMemoryError, JS throws RuntimeException -- assertFails
     * covers all of them, so the AssertionError a regressed implementation would raise by
     * reading the endless source is told apart explicitly. buildMap only: on the JVM the plain
     * HashMap/LinkedHashMap are java.util classes with no fail-fast capacity pre-check.
     */
    @Test
    fun capacityOverflowFailsCleanly() {
        val giant = object : AbstractMap<Int, Int>() {
            override val entries: Set<Map.Entry<Int, Int>> = object : AbstractSet<Map.Entry<Int, Int>>() {
                override val size: Int get() = Int.MAX_VALUE
                override fun iterator(): Iterator<Map.Entry<Int, Int>> =
                    fail("the capacity check must fail before any element is read")
            }
        }
        buildMap {
            put(1, 1)
            val failure = assertFails { putAll(giant) }
            assertIsNot<AssertionError>(failure, "the capacity check must fail before any element is read")
            assertEquals(1, size)
            assertEquals(1, get(1))
            put(2, 2)
            assertEquals(2, size)
        }
    }

    /**
     * Null keys and null values are stored, found, hashed (as 0), printed, and removed like any
     * other key or value.
     */
    @Test
    fun nullKeysAndValues() = testOnNullableMaps {
        assertNull(put(null, 1))
        assertEquals(1, put(null, 2))
        assertTrue(containsKey(null))
        assertEquals(2, get(null))
        assertNull(put(4, null))
        assertTrue(containsValue(null))
        assertFalse(containsValue(3))
        assertEquals(setOf(null, 4), keys)
        assertEquals((0 xor 2.hashCode()) + (4.hashCode() xor 0), hashCode(), "null key/value hash as 0")
        assertEquals(0 xor 2.hashCode(), entries.first { it.key == null }.hashCode())
        assertEquals(4.hashCode() xor 0, entries.first { it.value == null }.hashCode())
        assertTrue(toString().contains("null=2"))
        assertTrue(toString().contains("4=null"))
        assertEquals(2, remove(null))
        assertFalse(containsKey(null))
        assertNull(remove(4)) // the entry's value IS null
        assertEquals(0, size)
    }

    private inline fun testOnMaps(performOperations: MutableMap<Int, Int>.() -> Unit) {
        val capacity = 8
        buildMap(capacity) { performOperations() }
        HashMap<Int, Int>(capacity).apply { performOperations() }
        LinkedHashMap<Int, Int>(capacity).apply { performOperations() }
    }

    private inline fun testOnNullableMaps(performOperations: MutableMap<Int?, Int?>.() -> Unit) {
        buildMap<Int?, Int?> { performOperations() }
        HashMap<Int?, Int?>().apply { performOperations() }
        LinkedHashMap<Int?, Int?>().apply { performOperations() }
    }
}
