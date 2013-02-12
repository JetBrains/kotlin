package test.compare

import java.util.Comparator
import kotlin.test.*
import org.junit.Test

class Item(val name: String, val rating: Int): Comparable<Item> {
    fun toString() = "Item($name, $rating)"

    public override fun compareTo(other: Item): Int {
        var x : Comparable<Nothing> = rating

        return compareBy<Item>(this, other, { rating }, { name })
    }
}

class CompareTest {
    val v1 = Item("wine", 9)
    val v2 = Item("beer", 10)

    Test fun compareByCompareTo() {
        val diff = v1.compareTo(v2)
        assertTrue(diff < 0)
    }

    Test fun compareByNameFirst() {
        val diff = compareBy<Item>(v1, v2, { name }, { rating })
        assertTrue(diff > 0)
    }

    Test fun compareByRatingFirst() {
        val diff = compareBy<Item>(v1, v2, { rating }, { name })
        assertTrue(diff < 0)
    }

    Test fun compareSameObjectsByRatingFirst() {
        val diff = compareBy<Item>(v1, v1, { rating }, { name })
        assertTrue(diff == 0)
    }

    Test fun sortUsingComparatorHelperMethod() {
        val c = comparator<Item>({ rating }, { name })
        println("Created comparator $c")

        val diff = c.compare(v1, v2)
        assertTrue(diff < 0)
        val items = arrayList(v1, v2)
        items.sort(c)
        println("Sorted list in rating order $items")
    }

    Test fun sortUsingCustomComparator() {
        val c = object : Comparator<Item>{
            override fun compare(o1: Item, o2: Item): Int {
                return compareBy<Item>(o1, o2, { name }, { rating })
            }
            override fun equals(obj: Any?): Boolean {
                return this == obj
            }
        }
        println("Created comparator $c")

        val diff = c.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayList(v1, v2)
        items.sort(c)
        println("Sorted list in rating order $items")
    }

}
