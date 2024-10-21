package test.collections

import kotlin.test.*

class CrossJoinTest {
    private val colors = listOf("green", "yellow")
    private val fruits = listOf("apple", "banana")

    @Test fun emptyLeftCollection() {
        val empty = listOf<String>() crossJoin fruits
        assertFalse { empty.iterator().hasNext() }
    }

    @Test fun emptyRightCollection() {
        val empty = colors crossJoin listOf<String>()
        assertFalse { empty.iterator().hasNext() }
    }

    @Test fun emptyBothCollections() {
        val empty = listOf<String>() crossJoin listOf<String>()
        assertFalse { empty.iterator().hasNext() }
    }

    @Test fun crossJoinToPairs() {
        val notEmpty = colors crossJoin fruits
        assertEquals(listOf(
            Pair("green", "apple"),
            Pair("green", "banana"),
            Pair("yellow", "apple"),
            Pair("yellow", "banana"),
        ), notEmpty.toList())
    }

    @Test fun crossJoinWithTranformation() {
        data class Fruit(val color: String, val type: String)

        val notEmpty = crossJoin(colors, fruits) { color, type ->
            Fruit(color, type)
        }

        assertEquals(
            listOf(
                Fruit("green", "apple"),
                Fruit("green", "banana"),
                Fruit("yellow", "apple"),
                Fruit("yellow", "banana")
            ), notEmpty.toList()
        )
    }
}

