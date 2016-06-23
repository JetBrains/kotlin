package test.collections.js

import java.util.*

import kotlin.test.*
import org.junit.Test as test
import kotlin.comparisons.*

fun <T> List<T>.toArrayList() = this.toCollection(ArrayList<T>())

class JsCollectionsTest {
    val TEST_LIST = arrayOf(2, 0, 9, 7, 1).toList()
    val SORTED_TEST_LIST = arrayOf(0, 1, 2, 7, 9).toList()
    val MAX_ELEMENT = 9
    val COMPARATOR = Comparator { x: Int, y: Int ->  if (x > y) 1 else if (x < y) -1 else 0 }

    @test fun maxWithComparator() {
        assertEquals(MAX_ELEMENT, Collections.max(TEST_LIST, COMPARATOR))
    }

    @test fun sort() {
        val list = TEST_LIST.toArrayList()
        Collections.sort(list)
        assertEquals(SORTED_TEST_LIST, list)
    }

    @test fun sortWithComparator() {
        val list = TEST_LIST.toArrayList()
        Collections.sort(list, COMPARATOR)
        assertEquals(SORTED_TEST_LIST, list)
    }

    @test fun collectionToArray() {
        val array = TEST_LIST.toTypedArray()
        assertEquals(array.toList(), TEST_LIST)
    }

    @test fun toListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { it.toList() })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 1), { it.toList() })
    }

    @test fun toMutableListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { it.toMutableList() })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 2), { it.toMutableList() })
    }

    @test fun listOfDoesNotCreateView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { listOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 3), { listOf(*it) })
    }

    @test fun mutableListOfDoesNotCreateView() {
        snapshotDoesNotCreateView(arrayOf("first", "last"), { mutableListOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("item", 4), { mutableListOf(*it) })
    }

    @test fun arrayListDoesNotCreateArrayView() {
        snapshotDoesNotCreateView(arrayOf(1, 2), { arrayListOf(*it) })
        snapshotDoesNotCreateView(arrayOf<Any>("first", "last"), { arrayListOf(*it) })
    }


    private fun <T> snapshotDoesNotCreateView(array: Array<T>, snapshot: (Array<T>) -> List<T>) {
        val first = array.first()
        val last = array.last()

        val list = snapshot(array)
        assertEquals(first, list[0])
        array[0] = last
        assertEquals(first, list[0])
    }
}
