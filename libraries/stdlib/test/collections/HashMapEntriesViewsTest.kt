/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

/**
 * Entry, view, and iterator edge cases of the open-addressing hash maps (MapBuilder on JVM,
 * HashMap on Native/Wasm, InternalHashMap on JS): value-sensitive entry operations, foreign
 * elements in bulk queries, write-through view mutations, iterator misuse, and the valueless
 * storage mode shared with the hash sets. (Entries failing fast after a structural
 * modification are covered by MapTest.modifiedBackingMapOfEntry, KT-52181.) KT-83662.
 */
class HashMapEntriesViewsTest {

    /** entries.contains and entries.remove match on both key AND value. */
    @Test
    fun entryOperationsAreValueSensitive() = testOnMaps {
        put(1, 10)
        put(2, 20)
        val asMap: Map<Int, Int> = this // Set<Map.Entry> accepts plain entries (KT-42428)
        assertTrue(asMap.entries.contains(mapEntryOf(1, 10)))
        assertFalse(asMap.entries.contains(mapEntryOf(1, 99)))
        assertFalse(asMap.entries.contains(mapEntryOf(9, 10)))
        assertFalse(entries.remove(mutableEntryOf(1, 99)))
        assertFalse(entries.remove(mutableEntryOf(9, 10)))
        assertEquals(2, size)
        assertTrue(entries.remove(mutableEntryOf(1, 10)))
        assertFalse(containsKey(1))
        assertEquals(1, size)
    }

    /** values.remove drops exactly one entry with a matching value and reports absence. */
    @Test
    fun valuesRemoveDropsSingleMatchingEntry() = testOnMaps {
        put(1, 10)
        put(2, 20)
        put(3, 10) // duplicate value
        assertFalse(values.remove(99))
        assertTrue(values.remove(10))
        assertEquals(2, size)
        assertEquals(1, values.count { it == 10 }, "exactly one of the two value-10 entries is removed")
        assertTrue(values.remove(10))
        assertFalse(values.remove(10))
        assertEquals(listOf(20), values.toList())
    }

    /**
     * Bulk containment queries tolerate foreign elements, reporting absence instead of failing:
     * non-entry objects hit a ClassCastException that the JVM and Native maps catch internally,
     * nulls an explicit null check; JS filters both with a type check.
     */
    @Test
    fun entriesContainsAllToleratesForeignElements() = testOnMaps {
        put(1, 10)
        assertTrue(entries.containsAllUnchecked(emptyList()))
        assertTrue(entries.containsAllUnchecked(listOf(mapEntryOf(1, 10))))
        assertFalse(entries.containsAllUnchecked(listOf(mapEntryOf(1, 99))))
        assertFalse(entries.containsAllUnchecked(listOf(null)))
        assertFalse(entries.containsAllUnchecked(listOf("not an entry")))
        assertFalse(entries.containsAllUnchecked(listOf(mapEntryOf(1, 10), null)))
    }

    /**
     * Structural equality with another map goes through the same entry lookup: maps with equal
     * contents are equal regardless of insertion order; a differing value, a missing key, a
     * plain non-map object, and a larger map are all unequal.
     */
    @Test
    fun mapEqualsComparesContents() = testOnMaps {
        put(4, 1)
        put(25, 2)
        val equal = mapOf(25 to 2, 4 to 1)
        val differentValue = mapOf(4 to 1, 25 to 9)
        val differentKey = mapOf(4 to 1, 38 to 2)
        val bigger = mapOf(4 to 1, 25 to 2, 38 to 3)
        assertTrue(this == equal)
        assertEquals(equal.hashCode(), hashCode())
        assertFalse(this == differentValue)
        assertFalse(this == differentKey)
        assertFalse(this == bigger)
        assertFalse(this.equals("not a map"))
        assertTrue(this == this)
    }

    /** The views report emptiness of a fresh map and follow the first insertion. */
    @Test
    fun viewsOfEmptyMapAreEmpty() = testOnMaps {
        assertTrue(keys.isEmpty())
        assertTrue(values.isEmpty())
        assertTrue(entries.isEmpty())
        assertFalse(keys.iterator().hasNext())
        put(1, 10)
        assertFalse(keys.isEmpty())
        assertFalse(values.isEmpty())
        assertFalse(entries.isEmpty())
    }

    /** Mutations through the keys view write through to the map. */
    @Test
    fun keysViewWritesThrough() = testOnMaps {
        for (k in 1..4) put(k, k * 10)
        assertTrue(keys.remove(1))
        assertFalse(keys.remove(9))
        assertFalse(containsKey(1))
        assertTrue(keys.removeAll(listOf(2, 9)))
        assertFalse(keys.removeAll(listOf(9)))
        assertTrue(keys.retainAll(listOf(3)))
        assertFalse(keys.retainAll(listOf(3)))
        assertEquals(mapOf(3 to 30), toMap())
        keys.clear()
        assertTrue(isEmpty())
        assertFailsWith<UnsupportedOperationException> { keys.add(9) }
        assertFailsWith<UnsupportedOperationException> { keys.addAll(listOf(9)) }
    }

    /** Mutations through the values view write through to the map. */
    @Test
    fun valuesViewWritesThrough() = testOnMaps {
        for (k in 1..4) put(k, k * 10)
        assertTrue(values.removeAll(listOf(10, 99)))
        assertFalse(values.removeAll(listOf(99)))
        assertFalse(containsKey(1))
        assertTrue(values.retainAll(listOf(20, 30)))
        assertFalse(values.retainAll(listOf(20, 30)))
        assertEquals(mapOf(2 to 20, 3 to 30), toMap())
        values.clear()
        assertTrue(isEmpty())
        assertFailsWith<UnsupportedOperationException> { values.add(9) }
        assertFailsWith<UnsupportedOperationException> { values.addAll(listOf(9)) }
    }

    /** Mutations through the entries view write through to the map and stay value-sensitive. */
    @Test
    fun entriesViewWritesThrough() = testOnMaps {
        for (k in 1..4) put(k, k * 10)
        assertTrue(entries.removeAll(listOf(mutableEntryOf(1, 10), mutableEntryOf(2, 99))))
        assertEquals(3, size, "2 stays: its value did not match")
        assertTrue(entries.retainAll(listOf(mutableEntryOf(2, 20), mutableEntryOf(3, 30))))
        assertFalse(entries.retainAll(entries.toList()))
        assertEquals(mapOf(2 to 20, 3 to 30), toMap())
        entries.clear()
        assertTrue(isEmpty())
        assertFailsWith<UnsupportedOperationException> { entries.add(mutableEntryOf(9, 90)) }
        assertFailsWith<UnsupportedOperationException> { entries.addAll(listOf(mutableEntryOf(9, 90))) }
    }

    /** setValue through a live entry writes through and returns the previous value. */
    @Test
    fun entrySetValueWritesThrough() = testOnMaps {
        put(1, 10)
        put(2, 20)
        val entry = entries.first { it.key == 2 }
        assertEquals(20, entry.setValue(21))
        assertEquals(21, entry.value)
        assertEquals(21, get(2))
        assertEquals(2, size)
    }

    /** Entries obey the Map.Entry equality contract: equal to any entry with equal key and value. */
    @Test
    fun entryEqualityContract() = testOnMaps {
        put(1, 10)
        val entry = entries.first()
        assertTrue(entry.equals(mapEntryOf(1, 10)))
        assertFalse(entry.equals(mapEntryOf(1, 11)))
        assertFalse(entry.equals(mapEntryOf(2, 10)))
        assertFalse(entry.equals("not an entry"))
        assertEquals(1.hashCode() xor 10.hashCode(), entry.hashCode())
        assertEquals("1=10", entry.toString())
    }

    /**
     * Iterator misuse fails predictably. The remove-before-next guard lives in the shared base
     * iterator, so one view suffices for it; exhaustion is checked per view.
     */
    @Test
    fun iteratorMisuseThrows() = testOnMaps {
        put(1, 10)
        assertFailsWith<IllegalStateException> { keys.iterator().remove() }
        val it = keys.iterator()
        it.next()
        it.remove()
        assertFailsWith<IllegalStateException>("second remove for one next") { it.remove() }
        assertEquals(0, size)
        put(1, 10)
        val keysIt = keys.iterator()
        val valuesIt = values.iterator()
        val entriesIt = entries.iterator()
        keysIt.next(); valuesIt.next(); entriesIt.next()
        assertFailsWith<NoSuchElementException> { keysIt.next() }
        assertFailsWith<NoSuchElementException> { valuesIt.next() }
        assertFailsWith<NoSuchElementException> { entriesIt.next() }
    }

    /**
     * The hash sets share the map storage in a valueless mode (no values array is ever
     * allocated for them on the JVM, Native, and Wasm; JS sets store a Boolean values array):
     * additions, removals with hole patching, clearing, and growth rehashes must all handle
     * the absent values array.
     */
    @Test
    fun setOperationsOnValuelessStorage() = testOnSets {
        for (k in intArrayOf(4, 25, 38, 1, 22)) assertTrue(add(k))
        assertFalse(add(4))
        assertTrue(remove(25))
        assertFalse(remove(25))
        assertTrue(contains(38))
        for (k in 100..115) add(k) // forces a growth rehash of valueless storage
        assertEquals(20, size)
        assertTrue(containsAll(listOf(4, 38, 1, 22, 100, 115)))
        val it = iterator()
        it.next()
        it.remove() // iterator removal over valueless storage
        assertEquals(19, size)
        clear() // clears gapped valueless storage
        assertEquals(0, size)
        assertTrue(add(4))
        assertEquals(1, size)
    }

    private fun mutableEntryOf(key: Int, value: Int): MutableMap.MutableEntry<Int, Int> =
        (mutableMapOf(key to value).entries).first()

    /** The star-projected receiver lets element-type checking be bypassed for foreign probes. */
    private fun Set<*>.containsAllUnchecked(elements: Collection<Any?>): Boolean =
        containsAll(elements)

    private inline fun testOnMaps(performOperations: MutableMap<Int, Int>.() -> Unit) {
        val capacity = 8
        buildMap(capacity) { performOperations() }
        HashMap<Int, Int>(capacity).apply { performOperations() }
        LinkedHashMap<Int, Int>(capacity).apply { performOperations() }
    }

    private inline fun testOnSets(performOperations: MutableSet<Int>.() -> Unit) {
        val capacity = 8
        buildSet(capacity) { performOperations() }
        HashSet<Int>(capacity).apply { performOperations() }
        LinkedHashSet<Int>(capacity).apply { performOperations() }
    }
}
