package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    test fun fromIterable() {

        val data = arrayListOf("foo", "bar") as Iterable<String>

        val collection = ArrayList<String>()
        collection.addAll(data)

        assertTrue {
            data.all { collection.containsItem(it) }
        }
    }

    test fun fromIterator() {
        val list = arrayListOf("foo", "bar")
        val collection = ArrayList<String>()

        collection.addAll(list.iterator())

        println(collection.toString())
        println(list.toString())

        assertTrue {
            collection.containsAll(list)
        }
    }

}