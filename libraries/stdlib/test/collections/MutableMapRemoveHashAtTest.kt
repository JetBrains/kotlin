/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class MutableMapRemoveHashAtTest {

    // Reproducer from KT-82783
    @Test
    fun buildMapDuplicatesReproducer() = testOnMaps {
        put(148961824, 1)
        put(148962400, 1)
        put(148963552, 1)
        put(148964704, 1)
        put(148965856, 1)
        put(148967008, 1)
        put(148968160, 1)
        put(148969312, 1)
        put(148970464, 1)
        put(148971616, 1)
        put(148972768, 1)
        put(148973920, 1)
        put(148975072, 1)
        put(148976224, 1)
        put(148977376, 1)
        put(148978528, 1)
        remove(148961824)
        put(148978528, 1)
    }

    @Test
    fun buildMapDuplicatesReproducerReduced() = testOnMaps {
        put(59, 1)
        put(38, 1)
        put(67, 1)
        put(54, 1)
        put(70, 1)
        put(44, 1)
        put(65, 1)
        remove(59)
        put(65, 1) // it will be duplicated
    }

    /**
     * | hash |   keys    |
     * +------+-----------+
     * |    0 | 0, 13     |
     * |    1 | 5, 18, 26 |
     * |    2 | 10, 31    |
     * |    3 | 2, 23     |
     * |    4 | 15, 28    |
     * |    5 | 7, 20     |
     * |    6 | 12, 33    |
     * |    7 | 4, 25     |
     * |    8 | 9, 17, 30 |
     * |    9 | 1, 22     |
     * |   10 | 14, 27    |
     * |   11 | 6, 19     |
     * |   12 | 11, 32    |
     * |   13 | 3, 24     |
     * |   14 | 16, 29    |
     * |   15 | 8, 21     |
     */
    @Test
    fun buildMapDuplicatesMinimalProbeDistance() = testOnMaps {
        put(4, 1)   // hash == 7
        put(25, 1)  // hash == 7
        put(12, 1)  // hash == 6
        put(33, 1)  // hash == 6
        put(7, 1)   // hash == 5
        put(15, 1)  // hash == 4
        put(2, 1)   // hash == 3
        remove(4)
        put(2, 1)
    }

    @Test
    fun removeHashAtReturnByProbeDistance() = testOnMaps {
        put(7, 1)   // hash == 5
        put(15, 1)  // hash == 4
        put(2, 1)   // hash == 3
        put(10, 1)  // hash == 2
        remove(7)

        assertEquals(3, size)
        assertFalse(containsKey(7))
        assertEquals(1, get(15))
        assertEquals(1, get(2))
        assertEquals(1, get(10))
    }

    @Test
    fun removeHashAtStressTest() {
        val random = Random(0xcafebabe)
        repeat(50_000) {
            testOnMaps {
                val inserted = mutableSetOf<Int>()

                repeat(8) {
                    val key = random.nextInt(50)
                    put(key, key)
                    inserted.add(key)
                }

                val toDelete = inserted.shuffled(random).take(random.nextInt(inserted.size))
                for (key in toDelete) {
                    remove(key)
                    inserted.remove(key)
                }

                assertEquals(inserted.size, size)
                for (key in inserted) {
                    assertEquals(key, get(key))
                }

                repeat(toDelete.size) {
                    val key = random.nextInt(50)
                    put(key, key)
                }
            }
        }
    }

    private inline fun testOnMaps(performOperations: MutableMap<Int, Int>.() -> Unit) {
        val capacity = 8
        buildMap(capacity) { performOperations() }.assertNoDuplicateKeys()
        HashMap<Int, Int>(capacity).apply { performOperations() }.assertNoDuplicateKeys()
        LinkedHashMap<Int, Int>(capacity).apply { performOperations() }.assertNoDuplicateKeys()
    }

    private fun Map<Int, Int>.assertNoDuplicateKeys() {
        val uniqueKeys = HashSet(keys)
        if (uniqueKeys.size != keys.size) {
            val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue(duplicates.isEmpty(), "Found duplicates: $duplicates")
        }
    }
}
