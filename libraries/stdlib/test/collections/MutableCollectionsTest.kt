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

}
