/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

// TODO: consider moving to common stdlib tests.
class KT42428Test {

    private val listOfLetterIndexPairs = ('a'..'z').withIndex().map { (i, c) -> "$c" to i }

    private val mapOfLetterToIndex = listOfLetterIndexPairs.toMap()

    @Test fun testListOfPairsToMapEntriesContainsMapEntry() {
        testMapEntriesContainsMapEntry(listOfLetterIndexPairs.toMap(), "h", 7)
    }

    @Test fun testMapToMutableMapEntriesContainsMapEntry() {
        testMapEntriesContainsMapEntry(mapOfLetterToIndex.toMutableMap(), "h", 7)
    }

    @Test fun testHashMapEntriesContainsMapEntry() {
        testMapEntriesContainsMapEntry(HashMap(mapOfLetterToIndex), "h", 7)
    }

    @Test fun testLinkedHashMapEntriesContainsMapEntry() {
        testMapEntriesContainsMapEntry(LinkedHashMap(mapOfLetterToIndex), "h", 7)
    }

    // Based on https://youtrack.jetbrains.com/issue/KT-42428.
    private fun testMapEntriesContainsMapEntry(map: Map<String, Int>, key: String, value: Int) {
        data class SimpleEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {

            override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value

            override fun hashCode(): Int = key.hashCode() xor value.hashCode()

            override fun toString(): String = "$key=$value"
        }


        assertTrue(map.keys.contains(key))

        // This one requires special efforts to make it work this way.
        // map.entries can in fact be `MutableSet<MutableMap.MutableEntry>`,
        // which [contains] method takes [MutableEntry], so the compiler may generate special bridge
        // returning false for values that aren't [MutableEntry] (including [SimpleEntry]).
        assertTrue(map.entries.contains(SimpleEntry(key, value)))

        assertTrue(map.entries.toSet().contains(SimpleEntry(key, value)))
    }
}
