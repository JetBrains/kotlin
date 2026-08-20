/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class StoredKeyIdentityTest {

    @Test
    fun mapRetainsStoredKeys() = testOnMaps {
        val stored = storedKeys()
        for (key in stored) put(key, "first")

        for (key in stored) assertEquals("first", put(key.copy(), "second"))
        keys.assertRetained(stored)

        putAll(stored.associate { it.copy() to "third" })
        keys.assertRetained(stored)

        assertEquals(stored.size, size)
        for (key in stored) assertEquals("third", get(key))
    }

    @Test
    fun setRetainsStoredElements() = testOnSets {
        val stored = storedKeys()
        addAll(stored)

        for (element in stored) assertFalse(add(element.copy()))
        assertRetained(stored)

        assertFalse(addAll(stored.map { it.copy() }))
        assertRetained(stored)

        assertEquals(stored.size, size)
    }

    private inline fun testOnMaps(operations: MutableMap<Key, String>.() -> Unit) {
        HashMap<Key, String>().apply { operations() }
        LinkedHashMap<Key, String>().apply { operations() }
        buildMap { operations() }
    }

    private inline fun testOnSets(operations: MutableSet<Key>.() -> Unit) {
        HashSet<Key>().apply { operations() }
        LinkedHashSet<Key>().apply { operations() }
        buildSet { operations() }
    }

    private fun storedKeys(): List<Key> =
        List(12) { Key(it, LONG_COLLISION_HASH) } +
            List(3) { Key(50 + it, SHORT_COLLISION_HASH) } +
            List(4) { Key(100 + it) }

    private fun Set<Key>.assertRetained(expected: List<Key>) {
        for (key in expected) assertSame(key, first { it == key }, "the instance already stored must be retained")
    }

    private companion object {
        const val LONG_COLLISION_HASH = 0x5EED
        const val SHORT_COLLISION_HASH = 0x1CE
    }
}

private class Key(val id: Int, private val hash: Int = id) {
    fun copy(): Key = Key(id, hash)

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean = other is Key && other.id == id

    override fun toString(): String = "Key($id)"
}
