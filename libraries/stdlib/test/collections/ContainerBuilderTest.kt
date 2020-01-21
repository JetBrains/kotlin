package test.collections

import kotlin.test.*

class ContainerBuilderTest {
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

        assertEquals(listOf('a', 'b', 'c', 'd'), y)

        assertEquals(listOf(1), buildList(0) { add(1) })
        assertFailsWith<IllegalArgumentException> {
            buildList(-1) { add(0) }
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

        assertEquals(setOf('c', 'b', 'd'), y)

        assertEquals(setOf(1), buildSet(0) { add(1) })
        assertFailsWith<IllegalArgumentException> {
            buildSet(-1) { add(0) }
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

        assertEquals(mapOf('a' to 1, 'c' to 3, 'b' to 2, 'd' to 4), y)

        assertEquals(mapOf("a" to 1), buildMap<String, Int>(0) { put("a", 1) })
        assertFailsWith<IllegalArgumentException> {
            buildMap<String, Int>(-1) { put("x", 1) }
        }
    }
}
