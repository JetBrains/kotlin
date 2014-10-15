package test.compare

import kotlin.test.*
import org.junit.Test

class Item(val name: String, val rating: Int): Comparable<Item> {
    override fun toString() = "Item($name, $rating)"

    public override fun compareTo(other: Item): Int {
        return compareBy(this, other, { rating }, { name })
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
        val diff = compareBy(v1, v2, { name }, { rating })
        assertTrue(diff > 0)
    }

    Test fun compareByRatingFirst() {
        val diff = compareBy(v1, v2, { rating }, { name })
        assertTrue(diff < 0)
    }

    Test fun compareSameObjectsByRatingFirst() {
        val diff = compareBy(v1, v1, { rating }, { name })
        assertTrue(diff == 0)
    }
}
