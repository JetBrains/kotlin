/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections.js

import kotlin.test.*

class NativeMapTest {
    @Test
    fun sizeAndEmpty() {
        val data = nativeMapOf<String, Int>()
        assertTrue { data.none() }
        assertEquals(data.size, 0)
        assertFalse(data.entries.iterator().hasNext())
        assertFalse(data.values.iterator().hasNext())
        assertFalse(data.keys.iterator().hasNext())
    }

    @Test
    fun createFromNativeMap() {
        val src = nativeMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val clone = NativeMap(src)
        src.remove("location")
        assertEquals(clone, mapOf("beverage" to "beer", "location" to "Mells", "name" to "James"))
    }

    @Test
    fun createFromMap() {
        val src = mutableMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val clone = NativeMap(src)
        src.remove("location")
        assertEquals(clone, mapOf("beverage" to "beer", "location" to "Mells", "name" to "James"))
    }

    @Test
    fun iterate() {
        val src = arrayOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val map = nativeMapOf(*src)

        val srcIter = src.iterator()
        val keysIter = map.keys.iterator()
        val valuesIter = map.values.iterator()
        val entryIter = map.entries.iterator()

        while (true) {
            assertEquals(keysIter.hasNext(), srcIter.hasNext())
            assertEquals(valuesIter.hasNext(), srcIter.hasNext())
            assertEquals(entryIter.hasNext(), srcIter.hasNext())

            if (!srcIter.hasNext()) {
                break
            }

            val (key, value) = srcIter.next()
            assertEquals(key, keysIter.next())
            assertEquals(value, valuesIter.next())
            entryIter.next().let {
                assertEquals(key, it.key)
                assertEquals(value, it.value)
            }
        }
    }

    @Test
    fun iterateAndMutateEntries() {
        val map = nativeMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val e1 = map.entries.elementAt(0)
        val e2 = map.entries.elementAt(1)

        val it = map.iterator()
        for (e in it) {
            when (e.key) {
                "beverage" -> e.setValue("juice")
                "location" -> it.remove()
            }
        }

        assertEquals(mapOf("beverage" to "juice", "name" to "James"), map)
        assertEquals(e1.value, "juice")
        assertEquals(e2.value, "Mells")
    }

    @Test
    fun iterateAndMutateKeys() {
        val map = nativeMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val it = map.keys.iterator()
        for (key in it) {
            when (key) {
                "beverage" -> map["beverage"] = "juice"
                "location" -> it.remove()
            }
        }

        assertEquals(mapOf("beverage" to "juice", "name" to "James"), map)
    }

    @Test
    fun iterateAndMutateValues() {
        val map = nativeMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val it = map.values.iterator()
        for (value in it) {
            when (value) {
                "beer" -> map["beverage"] = "juice"
                "Mells" -> it.remove()
            }
        }

        assertEquals(mapOf("beverage" to "juice", "name" to "James"), map)
    }

    @Test
    fun containsKey() {
        val map = nativeMapOf("a" to 1, "b" to 2)
        assertTrue("a" in map)
        assertTrue("a" in map.keys)
        assertTrue("c" !in map)
        assertTrue("c" !in map.keys)
    }

    @Test
    fun containsValue() {
        val map = nativeMapOf("a" to 1, "b" to 2)
        assertTrue(map.containsValue(1))
        assertFalse(map.containsValue(3))
        assertTrue(1 in map.values)
        assertTrue(3 !in map.values)
    }

    @Test
    fun keyIdentity() {
        val k1 = Pair(0, 1)
        val k2 = Pair(0, 1)
        val map = nativeMapOf(k1 to 10)
        assertFalse(k2 in map)
    }

    private value class ValueKey(val value: Int)

    @Test
    fun boxedValueKeyIdentity() {
        val k1 = ValueKey(0)
        val k2 = ValueKey(0)
        val k1Boxed: Any? = k1
        val k2Boxed: Any? = k2
        val map = nativeMapOf(k1 to 10, k2Boxed to 20)
        assertTrue(k1 in map)
        assertTrue(k2 in map)
        assertTrue(k1Boxed in map)
        assertTrue(k2Boxed in map)
        assertEquals(map[k1], 10)
        assertEquals(map[k2], 20)
        assertEquals(map[k1Boxed], 10)
        assertEquals(map[k2Boxed], 20)
    }

    @Test
    fun entriesCovariantContains() {
        // Based on https://youtrack.jetbrains.com/issue/KT-42428.
        fun doTest(implName: String, map: Map<String, Int>, key: String, value: Int) {
            class SimpleEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
                override fun toString(): String = "$key=$value"
                override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value
            }

            val mapDescription = "$implName: ${map::class}"

            assertTrue(map.keys.contains(key), mapDescription)
            assertEquals(value, map[key], mapDescription)
            // This one requires special efforts to make it work this way.
            // map.entries can in fact be `MutableSet<MutableMap.MutableEntry>`,
            // which [contains] method takes [MutableEntry], so the compiler may generate special bridge
            // returning false for values that aren't [MutableEntry] (including [SimpleEntry]).
            assertTrue(map.entries.contains(SimpleEntry(key, value)), mapDescription)
            assertTrue(map.entries.toSet().contains(SimpleEntry(key, value)), "$mapDescription: reference")

            assertFalse(map.entries.contains(null as Any?), "$mapDescription: contains null")
            assertFalse(map.entries.contains("not an entry" as Any?), "$mapDescription: contains not an entry")
        }

        val mapLetterToIndex = ('a'..'z').mapIndexed { i, c -> "$c" to i }.toMap()
        doTest("NativeMap", mapLetterToIndex.toMap(NativeMap()), "c", 2)
    }

    @Test
    fun entriesCovariantRemove() {
        fun doTest(implName: String, map: MutableMap<String, Int>, key: String, value: Int) {
            class SimpleEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
                override fun toString(): String = "$key=$value"
                override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value
            }

            val mapDescription = "$implName: ${map::class}"

            assertTrue(map.entries.toMutableSet().remove(SimpleEntry(key, value) as Map.Entry<*, *>), "$mapDescription: reference")
            assertTrue(map.entries.remove(SimpleEntry(key, value) as Map.Entry<*, *>), mapDescription)

            assertFalse(map.entries.remove(null as Any?), "$mapDescription: remove null")
            assertFalse(map.entries.remove("not an entry" as Any?), "$mapDescription: remove not an entry")
        }

        val mapLetterToIndex = ('a'..'z').mapIndexed { i, c -> "$c" to i }.toMap()
        doTest("NativeMap", mapLetterToIndex.toMap(NativeMap()), "c", 2)
    }

    @Test
    fun put() {
        val map = nativeMapOf("a" to 1)
        assertEquals(map.put("b", 2), null)
        assertEquals(map["b"], 2)
        assertEquals(map.put("b", 3), 2)
        assertEquals(map["b"], 3)
    }

    @Test
    fun putFast() {
        val map = nativeMapOf("a" to 1)
        map.putFast("b", 2)
        assertEquals(map["b"], 2)
        map.putFast("b", 3)
        assertEquals(map["b"], 3)
    }

    @Test
    fun putAll() {
        val map = nativeMapOf('a' to 1)
        map.putAll(mapOf('b' to 2, 'c' to 3))
        map.putAll(listOf('d' to 4, 'e' to 5))
        map.putAll(sequenceOf('f' to 6, 'g' to 7))
        map.putAll(arrayOf('h' to 8, 'j' to 9))
        assertContentEquals(map.entries.map { it.key to it.value }, (1..9).map { i -> 'a' + i to i })
    }

    @Test
    fun remove() {
        val map = nativeMapOf("a" to 1)
        assertEquals(map.remove("b"), null)
        assertEquals(map.remove("a"), 1)
        assertEquals(map.remove("a"), null)
    }

    @Test
    fun removeFast() {
        val map = nativeMapOf("a" to 1)
        assertFalse(map.removeFast("b"))
        assertTrue(map.removeFast("a"))
        assertFalse(map.removeFast("a"))
    }

    @Test
    fun clear() {
        val map = nativeMapOf("a" to 1, "b" to 2)
        map.clear()
        assertEquals(map.size, 0)
    }

    @Test
    fun build(){
        val src = arrayOf("a" to 1, "b" to 2)
        val map = nativeMapOf(*src)
        val built = map.build()
        assertFailsWith<UnsupportedOperationException>() {
            map["c"] = 3
        }

        assertContentEquals(map.entries.map { it.key to it.value }.toTypedArray(), src)
    }
}
