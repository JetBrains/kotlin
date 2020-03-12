package test.collections

import test.collections.behaviors.listBehavior
import test.collections.behaviors.mapBehavior
import test.collections.behaviors.setBehavior
import kotlin.test.*

class ContainerBuilderTest {
    private fun <E> mutableCollectionOperations(e: E) = listOf<MutableCollection<E>.() -> Unit>(
        { add(e) },
        { addAll(listOf(e)) },
        { remove(e) },
        { removeAll(listOf(e)) },
        { retainAll(listOf(e)) },
        { clear() },
        { iterator().apply { next() }.remove() }
    )

    private fun <E> mutableListOperations(e: E) = mutableCollectionOperations(e) + listOf<MutableList<E>.() -> Unit>(
        { add(0, e) },
        { addAll(0, listOf(e)) },
        { removeAt(0) },
        { set(0, e) },
        { listIterator().apply { next() }.remove() },
        { listIterator(0).apply { next() }.remove() },
        { listIterator().add(e) }
    )

    private fun <E> mutableSetOperations(e: E): List<MutableSet<E>.() -> Unit> = mutableCollectionOperations(e)

    private fun <K, V> mutableMapOperations(k: K, v: V) = listOf<MutableMap<K, V>.() -> Unit>(
        { put(k, v) },
        { remove(k) },
        { putAll(mapOf(k to v)) },
        { clear() },
        { entries.first().setValue(v) },
        { entries.iterator().next().setValue(v) }
    )

    @Test
    fun buildList() {
        val x = buildList {
            add('b')
            add('c')
        }

        val y = buildList(4) {
            add('a')
            addAll(x)
            add('d')
        }

        compare(listOf('a', 'b', 'c', 'd'), y) { listBehavior() }
        compare(listOf('a', 'b', 'c', 'd'), y.subList(0, 4)) { listBehavior() }
        compare(listOf('b', 'c'), y.subList(1, 4).subList(0, 2)) { listBehavior() }

        assertEquals(listOf(1), buildList(0) { add(1) })
        assertFailsWith<IllegalArgumentException> {
            buildList(-1) { add(0) }
        }

        assertTrue(y is MutableList<Char>)
        for (operation in mutableListOperations('a')) {
            assertFailsWith<UnsupportedOperationException> { y.operation() }
            assertFailsWith<UnsupportedOperationException> { y.subList(1, 3).operation() }
        }
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

            compare("ab123${"x".repeat(20)}e".toList(), this) { listBehavior() }
            compare("b123${"x".repeat(20)}".toList(), subList) { listBehavior() }
            compare("23${"x".repeat(20)}".toList(), subSubList) { listBehavior() }
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
        for (operation in mutableSetOperations('b')) {
            assertFailsWith<UnsupportedOperationException> { y.operation() }
        }
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
        for (operation in mutableMapOperations('a', 0)) {
            assertFailsWith<UnsupportedOperationException> { y.operation() }
        }
        for (operation in mutableSetOperations('a')) {
            assertFailsWith<UnsupportedOperationException> { y.keys.operation() }
        }
        for (operation in mutableCollectionOperations(1)) {
            assertFailsWith<UnsupportedOperationException> { y.values.operation() }
        }
        for (operation in mutableSetOperations(y.entries.first())) {
            assertFailsWith<UnsupportedOperationException> { y.entries.operation() }
        }
    }
}
