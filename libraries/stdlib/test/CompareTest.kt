package test.compare

import kotlin.test.*
import java.util.*
import org.junit.Test


class Item(val name: String, val rating: Int) {
}

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
        val c = comparator<Item>({(i: Item) -> i.rating}, {(i: Item) -> i.name})
        println("Created comparator $c")

        todo {
            // TODO needs KT-729 before this code works
            val diff = c.compare(v1, v2)
            assertTrue(diff < 0)
            val items = arrayList(v1, v2)
            items.sort(c)
            println("Sorted list in rating order $items")
        }
    }
}