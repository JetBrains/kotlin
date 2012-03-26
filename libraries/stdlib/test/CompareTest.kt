package test

import org.junit.Test

import kotlin.test.*

class Item(val name: String, val rating: Int)

/**
 */
class CompareTest {
    val v1 = Item("wine", 9)
    val v2 = Item("beer", 10)

    Test fun compareByNameFirst() {
        val diff = compareBy(v1, v2, {(i: Item) -> i.name}, {(i: Item) -> i.rating})
        assertTrue(diff > 0)
    }

    Test fun compareByRatingFirst() {
        val diff = compareBy(v1, v2, {(i: Item) -> i.rating}, {(i: Item) -> i.name})
        assertTrue(diff < 0)
    }

    Test fun compareSameObjectsByRatingFirst() {
        val diff = compareBy(v1, v1, {(i: Item) -> i.rating}, {(i: Item) -> i.name})
        assertTrue(diff == 0)
    }

    Test fun createComparator() {
        val c = comparator({(i: Item) -> i.rating}, {(i: Item) -> i.name})
        println("Created comparator $c")
/*
        val items = arrayList(v1, v2)
        items.sort(c)
        println("Sorted list in rating order $items")
*/

    }
}