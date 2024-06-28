/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

// TODO: Check which parts are already tested in libraries/stdlib/test/collections/MapTest.kt
class HashMapTest {
    @Test fun basic() {
        val m = HashMap<String, String>()
        assertTrue(m.isEmpty())
        assertEquals(0, m.size)

        assertFalse(m.containsKey("1"))
        assertFalse(m.containsValue("a"))
        assertEquals(null, m.get("1"))

        assertEquals(null, m.put("1", "a"))
        assertTrue(m.containsKey("1"))
        assertTrue(m.containsValue("a"))
        assertEquals("a", m.get("1"))
        assertFalse(m.isEmpty())
        assertEquals(1, m.size)

        assertFalse(m.containsKey("2"))
        assertFalse(m.containsValue("b"))
        assertEquals(null, m.get("2"))

        assertEquals(null, m.put("2", "b"))
        assertTrue(m.containsKey("1"))
        assertTrue(m.containsValue("a"))
        assertEquals("a", m.get("1"))
        assertTrue(m.containsKey("2"))
        assertTrue(m.containsValue("b"))
        assertEquals("b", m.get("2"))
        assertFalse(m.isEmpty())
        assertEquals(2, m.size)

        assertEquals("b", m.put("2", "bb"))
        assertTrue(m.containsKey("1"))
        assertTrue(m.containsValue("a"))
        assertEquals("a", m.get("1"))
        assertTrue(m.containsKey("2"))
        assertTrue(m.containsValue("a"))
        assertTrue(m.containsValue("bb"))
        assertEquals("bb", m.get("2"))
        assertFalse(m.isEmpty())
        assertEquals(2, m.size)

        assertEquals("a", m.remove("1"))
        assertFalse(m.containsKey("1"))
        assertFalse(m.containsValue("a"))
        assertEquals(null, m.get("1"))
        assertTrue(m.containsKey("2"))
        assertTrue(m.containsValue("bb"))
        assertEquals("bb", m.get("2"))
        assertFalse(m.isEmpty())
        assertEquals(1, m.size)

        assertEquals("bb", m.remove("2"))
        assertFalse(m.containsKey("1"))
        assertFalse(m.containsValue("a"))
        assertEquals(null, m.get("1"))
        assertFalse(m.containsKey("2"))
        assertFalse(m.containsValue("bb"))
        assertEquals(null, m.get("2"))
        assertTrue(m.isEmpty())
        assertEquals(0, m.size)
    }

    @Test fun equals() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertTrue(m == expected)
        assertTrue(m == mapOf("b" to "2", "c" to "3", "a" to "1"))  // order does not matter
        assertFalse(m == mapOf("a" to "1", "b" to "2", "c" to "4"))
        assertFalse(m == mapOf("a" to "1", "b" to "2", "c" to "5"))
        assertFalse(m == mapOf("a" to "1", "b" to "2"))
        assertEquals(m.keys, expected.keys)
        assertEquals(m.values.toList(), expected.values.toList())
        assertEquals(m.entries, expected.entries)
    }

    @Test fun hashCodeTest() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertEquals(expected.hashCode(), m.hashCode())
        assertEquals(expected.entries.hashCode(), m.entries.hashCode())
        assertEquals(expected.keys.hashCode(), m.keys.hashCode())
    }

    @Test fun toStringTest() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertEquals(expected.toString(), m.toString())
        assertEquals(expected.entries.toString(), m.entries.toString())
        assertEquals(expected.keys.toString(), m.keys.toString())
        assertEquals(expected.values.toString(), m.values.toString())
    }

    @Test fun put() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        val e = expected.entries.iterator().next() as MutableMap.MutableEntry<String, String>
        assertTrue(m.entries.contains(e))
        assertTrue(m.entries.remove(e))
        assertTrue(mapOf("b" to "2", "c" to "3") == m)
        assertEquals(null, m.put(e.key, e.value))
        assertTrue(expected == m)
        assertEquals(e.value, m.put(e.key, e.value))
        assertTrue(expected == m)
    }

    @Test fun removeAll() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertFalse(m.entries.removeAll(mapOf("a" to "2", "b" to "3", "c" to "4").entries))
        assertEquals(expected, m)
        assertTrue(m.entries.removeAll(mapOf("b" to "22", "c" to "3", "d" to "4").entries))
        assertNotEquals(expected, m)
        assertEquals(mapOf("a" to "1", "b" to "2"), m)
    }

    @Test fun retainAll() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertFalse(m.entries.retainAll(expected.entries))
        assertEquals(expected, m)
        assertTrue(m.entries.retainAll(mapOf("b" to "22", "c" to "3", "d" to "4").entries))
        assertEquals(mapOf("c" to "3"), m)
    }

    @Test fun containsAll() {
        val m = HashMap(mapOf("a" to "1", "b" to "2", "c" to "3"))
        assertTrue(m.values.containsAll(listOf("1", "2")))
        assertTrue(m.values.containsAll(listOf("1", "2", "3")))
        assertFalse(m.values.containsAll(listOf("1", "2", "3", "4")))
        assertFalse(m.values.containsAll(listOf("2", "3", "4")))
    }

    @Test fun valuesRemove() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertFalse(m.values.remove("b"))
        assertEquals(expected, m)
        assertTrue(m.values.remove("2"))
        assertEquals(mapOf("a" to "1", "c" to "3"), m)
    }

    @Test fun valuesRemoveAll() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertFalse(m.values.removeAll(listOf("b", "c")))
        assertEquals(expected, m)
        assertTrue(m.values.removeAll(listOf("b", "3")))
        assertEquals(mapOf("a" to "1", "b" to "2"), m)
    }

    @Test fun valuesRetainAll() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        assertFalse(m.values.retainAll(listOf("1", "2", "3")))
        assertEquals(expected, m)
        assertTrue(m.values.retainAll(listOf("1", "2", "c")))
        assertEquals(mapOf("a" to "1", "b" to "2"), m)
    }

    @Test fun iterator() {
        val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
        val m = HashMap(expected)
        val it = m.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            entry.setValue(entry.value + "!")
        }
        assertNotEquals(expected, m)
        assertEquals(mapOf("a" to "1!", "b" to "2!", "c" to "3!"), m)
    }

    // TODO: Is it too slow with aggressive GC?
    @Test fun rehashAndCompact() {
        val m = HashMap<String, String>()
        for (repeat in 1..10) {
            val n = when (repeat) {
                1 -> 1000
                2 -> 10000
                3 -> 10
                else -> 100000
            }
            for (i in 1..n) {
                assertFalse(m.containsKey(i.toString()))
                assertEquals(null, m.put(i.toString(), "val$i"))
                assertTrue(m.containsKey(i.toString()))
                assertEquals(i, m.size)
            }
            for (i in 1..n) {
                assertTrue(m.containsKey(i.toString()))
            }
            for (i in 1..n) {
                assertEquals("val$i", m.remove(i.toString()))
                assertFalse(m.containsKey(i.toString()))
                assertEquals(n - i, m.size)
            }
            assertTrue(m.isEmpty())
        }
    }

    // TODO: Is it too slow with aggressive GC?
    @Test fun clear() {
        val m = HashMap<String, String>()
        for (repeat in 1..10) {
            val n = when (repeat) {
                1 -> 1000
                2 -> 10000
                3 -> 10
                else -> 100000
            }
            for (i in 1..n) {
                assertFalse(m.containsKey(i.toString()))
                assertEquals(null, m.put(i.toString(), "val$i"))
                assertTrue(m.containsKey(i.toString()))
                assertEquals(i, m.size)
            }
            for (i in 1..n) {
                assertTrue(m.containsKey(i.toString()))
            }
            m.clear()
            assertEquals(0, m.size)
            for (i in 1..n) {
                assertFalse(m.containsKey(i.toString()))
            }
        }
    }
}