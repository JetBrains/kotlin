/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.collections.behaviors.listBehavior
import test.collections.behaviors.mapBehavior
import test.collections.behaviors.setBehavior
import kotlin.test.*

class ContainerBuilderTest {
    private fun <E> mutableCollectionOperations(present: E, absent: E) = listOf<Pair<String, MutableCollection<E>.() -> Unit>>(
        "add(present)"                          to { add(present) },
        "add(absent)"                           to { add(absent) },

        "addAll(listOf(present))"               to { addAll(listOf(present)) },
        "addAll(listOf(absent))"                to { addAll(listOf(absent)) },
        "addAll(emptyList())"                   to { addAll(emptyList()) },

        "remove(present)"                       to { remove(present) },
        "remove(absent)"                        to { remove(absent) },

        "removeAll(listOf(present))"            to { removeAll(listOf(present)) },
        "removeAll(emptyList())"                to { removeAll(emptyList()) },
        "removeAll(this.toList())"              to { removeAll(this.toList()) },

        "retainAll(listOf(present))"            to { retainAll(listOf(present)) },
        "retainAll(emptyList())"                to { retainAll(emptyList()) },
        "retainAll(this.toList())"              to { retainAll(this.toList()) },

        "clear()"                               to { clear() },

        "iterator().apply { next() }.remove()"  to { iterator().apply { next() }.remove() }
    )

    private fun <E> mutableListOperations(present: E, absent: E) = mutableCollectionOperations(present, absent) + listOf<Pair<String, MutableList<E>.() -> Unit>>(
        "add(0, present)"                               to { add(0, present) },
        "addAll(0, listOf(present))"                    to { addAll(0, listOf(present)) },
        "addAll(0, emptyList())"                        to { addAll(0, emptyList()) },
        "removeAt(0)"                                   to { removeAt(0) },
        "set(0, present)"                               to { set(0, present) },
        "listIterator().apply { next() }.remove()"      to { listIterator().apply { next() }.remove() },
        "listIterator(0).apply { next() }.remove()"     to { listIterator(0).apply { next() }.remove() },
        "listIterator().apply { next() }.set(present)"  to { listIterator().apply { next() }.set(present) },
        "listIterator().add(present)"                   to { listIterator().add(present) }
    )

    private fun <E> mutableSetOperations(present: E, absent: E) = mutableCollectionOperations(present, absent) + listOf<Pair<String, MutableSet<E>.() -> Unit>>(
        // check java.util.AbstractSet.removeAll optimisation
        "removeAll(List(this.size) { absent })" to { removeAll(List(this.size) { absent }) }
    )

    private fun <K, V> mutableMapOperations(k: K, v: V) = listOf<Pair<String, MutableMap<K, V>.() -> Unit>>(
        "put(k, v)"                             to { put(k, v) },
        "remove(k)"                             to { remove(k) },
        "putAll(mapOf(k to v))"                 to { putAll(mapOf(k to v)) },
        "putAll(emptyMap())"                    to { putAll(emptyMap()) },
        "clear()"                               to { clear() },
        "entries.first().setValue(v)"           to { entries.first().setValue(v) },
        "entries.iterator().next().setValue(v)" to { entries.iterator().next().setValue(v) }
    )

    private fun <E> emptyCollectionOperations(value: E) = listOf<Pair<String, MutableCollection<E>.() -> Unit>> (
        "add(value)"               to { add(value) },

        "addAll(listOf(value))"    to { addAll(listOf(value)) },
        "addAll(emptyList())"      to { addAll(emptyList()) },

        "remove(value)"            to { remove(value) },

        "removeAll(listOf(value))" to { removeAll(listOf(value)) },
        "removeAll(emptyList())"   to { removeAll(emptyList()) },

        "retainAll(listOf(value))" to { retainAll(listOf(value)) },
        "retailAll(emptyList())"   to { retainAll(emptyList()) },

        "clear()"                  to { clear() }
    )

    private fun <E> emptyListOperations(value: E) = emptyCollectionOperations(value) + listOf<Pair<String, MutableList<E>.() -> Unit>>(
        "add(0, value)"             to { add(0, value) },
        "addAll(0, listOf(value))"  to { addAll(0, listOf(value)) },
        "addAll(0, emptyList())"    to { addAll(0, emptyList()) },
        "listIterator().add(value)" to { listIterator().add(value) }
    )

    private fun <K, V> emptyMapOperations(k: K, v: V) = listOf<Pair<String, MutableMap<K, V>.() -> Unit>>(
        "put(k, v)"             to { put(k, v) },
        "remove(k)"             to { remove(k) },
        "putAll(mapOf(k to v))" to { putAll(mapOf(k to v)) },
        "putAll(emptyMap())"    to { putAll(emptyMap()) },
        "clear()"               to { clear() }
    )

    @Test
    fun buildList() {
        val x = buildList {
            add('b')
            add('c')
        }

        val subList: MutableList<Char>

        val y = buildList<Char>(4) {
            add('a')
            addAll(x)
            add('d')

            subList = subList(0, 4)
        }

        compare(listOf('a', 'b', 'c', 'd'), y) { listBehavior() }
        compare(listOf('a', 'b', 'c', 'd'), y.subList(0, 4)) { listBehavior() }
        compare(listOf('b', 'c'), y.subList(1, 4).subList(0, 2)) { listBehavior() }

        assertEquals(listOf(1), buildList(0) { add(1) })
        assertFailsWith<IllegalArgumentException> {
            buildList(-1) { add(0) }
        }

        assertTrue(y is MutableList<Char>)
        for ((fName, operation) in mutableListOperations('b', 'x')) {
            assertFailsWith<UnsupportedOperationException>("y.$fName") { y.operation() }
            assertFailsWith<UnsupportedOperationException>("y.subList(1, 3).$fName") { y.subList(1, 3).operation() }
            assertFailsWith<UnsupportedOperationException>("subList.$fName") { subList.operation() }
        }
    }

    @Test
    fun buildEmptyList() {
        val empty = buildList<Int> {}
        assertSame(empty, buildList {})
        assertTrue(empty is MutableList<Int>)
        for ((fName, operation) in emptyListOperations(0)) {
            assertFailsWith<UnsupportedOperationException>("empty.$fName") { empty.operation() }
        }

        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())
        assertFalse(empty.contains(42))
        assertFalse(empty.containsAll(listOf(42)))
        assertTrue(empty.containsAll(emptyList()))
    }

    @Test
    fun listBuilderSubList() {
        buildList<Char> {
            addAll(listOf('a', 'b', 'c', 'd', 'e'))

            val subList = subList(1, 4)
            compare(listOf('a', 'b', 'c', 'd', 'e'), this) { listBehavior() }
            compare(listOf('b', 'c', 'd'), subList) { listBehavior() }

            set(2, '1')
            compare(listOf('a', 'b', '1', 'd', 'e'), this) { listBehavior() }
            compare(listOf('b', '1', 'd'), subList) { listBehavior() }

            subList[2] = '2'
            compare(listOf('a', 'b', '1', '2', 'e'), this) { listBehavior() }
            compare(listOf('b', '1', '2'), subList) { listBehavior() }

            subList.add('3')
            compare(listOf('a', 'b', '1', '2', '3', 'e'), this) { listBehavior() }
            compare(listOf('b', '1', '2', '3'), subList) { listBehavior() }

            val subSubList = subList.subList(2, 4)
            // buffer reallocation should happen
            repeat(20) { subSubList.add('x') }
            repeat(20) { subSubList.add(subSubList.size - 2 * it, 'y') }

            val addedChars = "xy".repeat(20)
            compare("ab123${addedChars}e".toList(), this) { listBehavior() }
            compare("b123$addedChars".toList(), subList) { listBehavior() }
            compare("23$addedChars".toList(), subSubList) { listBehavior() }
        }
    }

    @Test
    fun buildSet() {
        val x = buildSet {
            add('b')
            add('c')
        }

        val y = buildSet(4) {
            add('c')
            addAll(x)
            add('d')
        }

        compare(setOf('c', 'b', 'd'), y) { setBehavior() }

        assertEquals(setOf(1), buildSet(0) { add(1) })
        assertFailsWith<IllegalArgumentException> {
            buildSet(-1) { add(0) }
        }

        assertTrue(y is MutableSet<Char>)
        for ((fName, operation) in mutableSetOperations('b', 'x')) {
            assertFailsWith<UnsupportedOperationException>("y.$fName") { y.operation() }
        }
    }

    @Test
    fun buildEmptySet() {
        val empty = buildSet<Int> {}
        assertSame(empty, buildSet {})
        assertTrue(empty is MutableSet<Int>)
        for ((fName, operation) in emptyCollectionOperations(0)) {
            assertFailsWith<UnsupportedOperationException>("empty.$fName") { empty.operation() }
        }

        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())
        assertFalse(empty.contains(42))
        assertFalse(empty.containsAll(listOf(42)))
        assertTrue(empty.containsAll(emptyList()))
    }

    @Test
    fun buildMap() {
        val x = buildMap<Char, Int> {
            put('b', 2)
            put('c', 3)
        }

        val y = buildMap<Char, Int>(4) {
            put('a', 1)
            put('c', 0)
            putAll(x)
            put('d', 4)
        }

        compare(mapOf('a' to 1, 'c' to 3, 'b' to 2, 'd' to 4), y) { mapBehavior() }

        assertEquals(mapOf("a" to 1), buildMap<String, Int>(0) { put("a", 1) })
        assertFailsWith<IllegalArgumentException> {
            buildMap<String, Int>(-1) { put("x", 1) }
        }

        assertTrue(y is MutableMap<Char, Int>)
        for ((fName, operation) in mutableMapOperations('a', 1) + mutableMapOperations('x', 10)) {
            assertFailsWith<UnsupportedOperationException>("y.$fName") { y.operation() }
        }
        for ((fName, operation) in mutableSetOperations('a', 'x')) {
            assertFailsWith<UnsupportedOperationException>("y.keys.$fName") { y.keys.operation() }
        }
        for ((fName, operation) in mutableCollectionOperations(1, 10)) {
            assertFailsWith<UnsupportedOperationException>("y.values.$fName") { y.values.operation() }
        }
        val presentEntry = y.entries.first()
        val absentEntry: MutableMap.MutableEntry<Char, Int> = object : MutableMap.MutableEntry<Char, Int> {
            override val key: Char get() = 'x'
            override val value: Int get() = 10
            override fun setValue(newValue: Int): Int = fail("Unreachable")
        }
        for ((fName, operation) in mutableSetOperations(presentEntry, absentEntry)) {
            assertFailsWith<UnsupportedOperationException>("y.entries.$fName") { y.entries.operation() }
        }
    }

    @Test
    fun testBuildEmptyMap() {
        val empty = buildMap<Char, Int> {}
        assertSame(empty, buildMap {})
        assertTrue(empty is MutableMap<Char, Int>)
        for ((fName, operation) in emptyMapOperations('0', 0)) {
            assertFailsWith<UnsupportedOperationException>("empty.$fName") { empty.operation() }
        }

        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())
        assertFalse(empty.contains('0'))
        assertFalse(empty.containsKey('0'))
        assertFalse(empty.containsValue(0))
    }
}
