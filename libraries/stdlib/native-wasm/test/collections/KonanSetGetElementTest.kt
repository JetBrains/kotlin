/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.collections

import kotlin.test.*

/**
 * The KonanSet.getElement lookup backing the Objective-C export bridge (Kotlin_Set_getElement):
 * HashSet and the HashMap keys view must return the STORED instance for an equal element, and
 * the entries view a live entry carrying the stored key instance; absent elements answer null.
 * There is no other path to HashMap.getKey/getEntry, so this pins the interop contract. Null
 * elements are outside the pinned contract: the bridge routes null lookups to contains before
 * ever calling getElement (see ObjCExportCollections.mm). KT-83662.
 */
class KonanSetGetElementTest {

    private data class Box(val value: Int)

    @Suppress("UNCHECKED_CAST")
    private fun <E> Set<E>.getElementViaKonanSet(element: E): E? =
        (this as kotlin.native.internal.KonanSet<E>).getElement(element)

    @Test
    fun hashSetReturnsStoredInstance() {
        val stored = Box(1)
        val set = HashSet<Box>()
        set.add(stored)
        val equalButDistinct = Box(1)
        assertNotSame(stored, equalButDistinct)
        assertSame(stored, set.getElementViaKonanSet(equalButDistinct))
        assertNull(set.getElementViaKonanSet(Box(2)))
    }

    @Test
    fun mapKeysViewReturnsStoredKey() {
        val storedKey = Box(1)
        val map = HashMap<Box, String>()
        map[storedKey] = "v"
        assertSame(storedKey, map.keys.getElementViaKonanSet(Box(1)))
        assertNull(map.keys.getElementViaKonanSet(Box(2)))
    }

    @Test
    fun mapEntriesViewMatchesKeyAndValue() {
        val storedKey = Box(1)
        val map = HashMap<Box, String>()
        map[storedKey] = "v"
        val found = map.entries.getElementViaKonanSet(mutableMapOf(Box(1) to "v").entries.first())
        assertNotNull(found)
        assertSame(storedKey, found.key)
        assertEquals("v", found.value)
        assertNull(map.entries.getElementViaKonanSet(mutableMapOf(Box(1) to "other").entries.first()))
        assertNull(map.entries.getElementViaKonanSet(mutableMapOf(Box(9) to "v").entries.first()))

        // The production caller invokes getElement through the erased KonanSet<Any?> with an
        // element that need not be this map's MutableEntry; a plain Map.Entry must still match.
        @Suppress("UNCHECKED_CAST")
        val erased = map.entries as kotlin.native.internal.KonanSet<Any?>
        val plainEntry = object : Map.Entry<Box, String> {
            override val key: Box get() = Box(1)
            override val value: String get() = "v"
        }
        val viaBridge = erased.getElement(plainEntry)
        assertNotNull(viaBridge)
        assertSame<Any?>(storedKey, (viaBridge as Map.Entry<*, *>).key)
    }
}
