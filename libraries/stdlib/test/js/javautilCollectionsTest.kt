package test.collections

import java.util.Collections
import java.util.ArrayList

import kotlin.test.*
import org.junit.Test as test

fun <T> List<T>.toArrayList() = this.toCollection(ArrayList<T>())

class JavautilCollectionsTest {
    val TEST_LIST = arrayOf(2, 0, 9, 7, 1).toList()
    val SORTED_TEST_LIST = arrayOf(0, 1, 2, 7, 9).toList()
    val MAX_ELEMENT = 9
    val COMPARATOR = comparator { x: Int, y: Int ->  if (x > y) 1 else if (x < y) -1 else 0 }

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

    @test fun arrayListDoesNotCreateArrayView() {
        val array = arrayOf(1)
        val list = arrayListOf(*array)
        assertEquals(1, list[0])
        array[0] = 2
        assertEquals(1, list[0])

        val arrayOfAny = arrayOf<Any>("first")
        val listOfAny = arrayListOf(*arrayOfAny)
        assertEquals("first", listOfAny[0])
        arrayOfAny[0] = "last"
        assertEquals("first", listOfAny[0])
    }
}
