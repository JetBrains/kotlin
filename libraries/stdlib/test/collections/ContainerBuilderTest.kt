package test.collections

import kotlin.test.*

class ContainerBuilderTest {
    @Test fun buildList() {
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
    }

    @Test fun exceptionIsThrownIfExpectedListSizeIsNegative() {
        assertFailsWith(IllegalArgumentException::class) {
            buildList<Any?>(-1) {}
        }
    }

    @Test fun buildSet() {
        val x = buildSet {
            add('b')
            add('c')
        }

        val y = buildSet(4) {
            add('a')
            addAll(x)
            add('d')
        }

        assertEquals(setOf('a', 'b', 'c', 'd'), y)
    }

    @Test fun exceptionIsThrownIfExpectedSetSizeIsNegative() {
        assertFailsWith(IllegalArgumentException::class) {
            buildSet<Any?>(-1) {}
        }
    }

    @Test fun buildMap() {
        val x = buildMap<Char, Int> {
            put('b', 2)
            put('c', 3)
        }

        val y = buildMap<Char, Int>(4) {
            put('a', 1)
            putAll(x)
            put('d', 4)
        }

        assertEquals(mapOf('a' to 1, 'b' to 2, 'c' to 3, 'd' to 4), y)
    }

    @Test fun exceptionIsThrownIfExpectedMapSizeIsNegative() {
        assertFailsWith(IllegalArgumentException::class) {
            buildMap<Any?, Any?>(-1) {}
        }
    }
}
