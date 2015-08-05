package test.compare

import java.util.*
import kotlin.test.*
import org.junit.Test

data class Item(val name: String, val rating: Int) : Comparable<Item> {
    public override fun compareTo(other: Item): Int {
        return compareValuesBy(this, other, { it.rating }, { it.name })
    }
}

class OrderingTest {
    val v1 = Item("wine", 9)
    val v2 = Item("beer", 10)

    Test fun compareByCompareTo() {
        val diff = v1.compareTo(v2)
        assertTrue(diff < 0)
    }

    Test fun compareByNameFirst() {
        val diff = compareValuesBy(v1, v2, { it.name }, { it.rating })
        assertTrue(diff > 0)
    }

    Test fun compareByRatingFirst() {
        val diff = compareValuesBy(v1, v2, { it.rating }, { it.name })
        assertTrue(diff < 0)
    }

    Test fun compareSameObjectsByRatingFirst() {
        val diff = compareValuesBy(v1, v1, { it.rating }, { it.name })
        assertTrue(diff == 0)
    }

    Test fun compareNullables() {
        val v1: Item? = this.v1
        val v2: Item? = null
        val diff = compareValuesBy(v1, v2) { it?.rating }
        assertTrue(diff > 0)
        val diff2 = nullsLast(compareBy<Item> { it.rating }.thenBy { it.name }).compare(v1, v2)
        assertTrue(diff2 < 0)
    }

    Test fun sortComparatorThenComparator() {
        val comparator = comparator<Item> { a, b -> a.name.compareTo(b.name) } thenComparator { a, b -> a.rating.compareTo(b.rating) }

        val diff = comparator.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayListOf(v1, v2).sortedWith(comparator)
        assertEquals(v2, items[0])
        assertEquals(v1, items[1])
    }

    Test fun sortByThenBy() {
        val comparator = compareBy<Item> { it.rating } thenBy { it.name }

        val diff = comparator.compare(v1, v2)
        assertTrue(diff < 0)
        val items = arrayListOf(v1, v2).sortedWith(comparator)
        assertEquals(v1, items[0])
        assertEquals(v2, items[1])
    }

    Test fun sortByThenByDescending() {
        val comparator = compareBy<Item> { it.rating } thenByDescending { it.name }

        val diff = comparator.compare(v1, v2)
        assertTrue(diff < 0)
        val items = arrayListOf(v1, v2).sortedWith(comparator)
        assertEquals(v1, items[0])
        assertEquals(v2, items[1])
    }

    Test fun sortUsingFunctionalComparator() {
        val comparator = compareBy<Item>({ it.name }, { it.rating })
        val diff = comparator.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayListOf(v1, v2).sortedWith(comparator)
        assertEquals(v2, items[0])
        assertEquals(v1, items[1])
    }

    Test fun sortUsingCustomComparator() {
        val comparator = object : Comparator<Item> {
            override fun compare(o1: Item, o2: Item): Int {
                return compareValuesBy(o1, o2, { it.name }, { it.rating })
            }

            override fun equals(other: Any?): Boolean {
                return this == other
            }
        }
        val diff = comparator.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayListOf(v1, v2).sortedWith(comparator)
        assertEquals(v2, items[0])
        assertEquals(v1, items[1])
    }

}
