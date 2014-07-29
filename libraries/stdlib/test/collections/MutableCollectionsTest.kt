package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    test fun fromIterable() {
        val data: Iterable<String> = arrayListOf("foo", "bar")

        val collection = ArrayList<String>()
        collection.addAll(data)

        assertEquals(data, collection)
    }

    test fun fromStream() {
        val list = arrayListOf("foo", "bar")
        val collection = ArrayList<String>()

        collection.addAll(list.stream())

        assertEquals(list, collection)
    }

    test fun plusAssign() {
        var list1 = arrayListOf("1", "2")
        val list2 = list1
        list1 += "3"
        list1 += array("4", "5")
        list1 += arrayListOf("6", "7")
        list1 += array("8", "9").stream()

        assertEquals((1..9).map { x -> x.toString() }, list2)
        assert(list1 identityEquals list2)
    }

    test fun minusAssign() {
        var list1 = arrayListOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
        val list2 = list1
        list1 -= "3"
        list1 -= array("4", "5")
        list1 -= arrayListOf("6", "7")
        list1 -= array("8", "9").stream()

        assertEquals(arrayListOf("1", "2"), list1)
        assert(list1 identityEquals list2)
    }
}
