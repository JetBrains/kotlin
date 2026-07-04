/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

/**
 * Probe-sequence edge cases of the open-addressing hash maps (MapBuilder on JVM, HashMap on
 * Native/Wasm, InternalHashMap on JS): search failing by probe-budget exhaustion rather than by
 * hitting an empty slot, and the growth rehash failing fast on mutated key hash codes.
 * (Deletion patching and zero-slot wraparound are exercised by MutableMapRemoveHashAtTest's
 * directed cases and seeded stress rounds.) KT-83662.
 *
 * Keys are plain Ints (Int.hashCode() == value on every platform). At the default capacity 8
 * the hash array has 16 slots and `hash(key) = (key * -1640531527) ushr 28`; the initial
 * maxProbeDistance is 2, and probing walks DOWN from the home slot, wrapping from 0 to 15.
 *
 * | slot |   keys     |      | slot |   keys     |
 * +------+------------+      +------+------------+
 * |    0 | 0, 13, 34  |      |    8 | 9, 17, 30  |
 * |    1 | 5, 18, 26  |      |    9 | 1, 22, 43  |
 * |    2 | 10, 31, 52 |      |   10 | 14, 27, 35 |
 * |    3 | 2, 23, 36  |      |   11 | 6, 19, 40  |
 * |    4 | 15, 28, 49 |      |   12 | 11, 32, 45 |
 * |    5 | 7, 20, 41  |      |   13 | 3, 24, 37  |
 * |    6 | 12, 33, 46 |      |   14 | 16, 29, 50 |
 * |    7 | 4, 25, 38  |      |   15 | 8, 21, 42  |
 */
class HashMapProbingTest {

    /**
     * A search for an absent key must give up after maxProbeDistance probes even when every
     * probed slot is occupied: 4, 25, 38 all home at slot 7 and land at slots 7, 6, 5; the
     * absent key 59 also homes at slot 7, so its search walks the fully occupied run 7, 6, 5
     * and exhausts the probe budget without ever meeting an empty slot.
     */
    @Test
    fun searchExhaustsProbeBudgetOnFullRun() = testOnMaps {
        put(4, 1)
        put(25, 2)
        put(38, 3) // placed at probe distance 2 == maxProbeDistance
        assertEquals(3, get(38))
        assertFalse(containsKey(59))
        assertNull(get(59))
        assertNull(remove(59))
        assertEquals(3, size)
    }

    /**
     * A growth rehash refuses to rebuild the table when key hash codes changed after insertion,
     * failing fast with IllegalStateException instead of silently corrupting the map. Mutating
     * a key used in a hash map is a documented contract violation; this pins the fail-fast
     * behavior. The eight keys start at eight distinct home slots (so maxProbeDistance stays at
     * its initial 2); after all of them collapse onto one hash code, the growth rehash
     * triggered by the ninth insertion cannot place the fourth same-hash key within the probe
     * budget. buildMap only: on the JVM the plain HashMap/LinkedHashMap are java.util classes,
     * which degrade silently instead of failing fast.
     */
    @Test
    fun rehashFailsFastOnMutatedKeys() {
        assertFailsWith<IllegalStateException> {
            buildMap<MutableHashKey, Int>(8) {
                val keys = intArrayOf(0, 5, 10, 2, 15, 7, 12, 4).map { MutableHashKey(it) }
                keys.forEachIndexed { i, k -> put(k, i) }
                keys.forEach { it.h = 13 }
                put(MutableHashKey(9), 8)
            }
        }
    }

    private class MutableHashKey(var h: Int) {
        override fun hashCode(): Int = h
    }

    private inline fun testOnMaps(performOperations: MutableMap<Int, Int>.() -> Unit) {
        val capacity = 8
        buildMap(capacity) { performOperations() }
        HashMap<Int, Int>(capacity).apply { performOperations() }
        LinkedHashMap<Int, Int>(capacity).apply { performOperations() }
    }
}
