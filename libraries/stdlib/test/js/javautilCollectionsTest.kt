package testPackage

import java.util.Collections
import java.util.ArrayList

import kotlin.test.*
import org.junit.Test as test

fun <T> List<T>.toArrayList() = this.toCollection(ArrayList<T>())

class JavautilCollectionsTest {
    val TEST_LIST = array(2, 0, 9, 7, 1).toList()
    val SORTED_TEST_LIST = array(0, 1, 2, 7, 9).toList()
    val MAX_ELEMENT = 9
    val COMPARATOR = comparator { x: Int, y: Int ->  if (x > y) 1 else if (x < y) -1 else 0 }

    test fun maxWithComparator() {
        assertEquals(MAX_ELEMENT, Collections.max(TEST_LIST, COMPARATOR))
    }

    test fun sort() {
        val list = TEST_LIST.toArrayList()
        Collections.sort(list)
        assertEquals(SORTED_TEST_LIST, list)
    }

    test fun sortWithComparator() {
        val list = TEST_LIST.toArrayList()
        Collections.sort(list, COMPARATOR)
        assertEquals(SORTED_TEST_LIST, list)
    }

    test fun collectionToArray() {
        val array = TEST_LIST.copyToArray()
        assertEquals(array.toList(), TEST_LIST)
    }
}
