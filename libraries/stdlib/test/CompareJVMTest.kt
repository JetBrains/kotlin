package test.compare

import java.util.Comparator
import kotlin.test.*
import org.junit.Test

class CompareJVMTest {
    val v1 = Item("wine", 9)
    val v2 = Item("beer", 10)

    Test fun sortUsingComparatorHelperMethod() {
        val c = comparator<Item>({ rating }, { name })
        println("Created comparator $c")

        val diff = c.compare(v1, v2)
        assertTrue(diff < 0)
        val items = arrayListOf(v1, v2)
        items.sortBy(c)
        println("Sorted list in rating order $items")
    }

    Test fun sortUsingCustomComparator() {
        val c = object : Comparator<Item>{
            override fun compare(o1: Item, o2: Item): Int {
                return compareBy(o1, o2, { name }, { rating })
            }
            override fun equals(obj: Any?): Boolean {
                return this == obj
            }
        }
        println("Created comparator $c")

        val diff = c.compare(v1, v2)
        assertTrue(diff > 0)
        val items = arrayListOf(v1, v2)
        items.sortBy(c)
        println("Sorted list in rating order $items")
    }

}
