package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    // TODO: Use apply scope function

    test fun addAll() {
        val data = listOf("foo", "bar")

        fun assertAdd(f: MutableList<String>.() -> Unit) = assertEquals(data, arrayListOf<String>().let { it.f(); it })

        assertAdd { addAll(data) }
        assertAdd { addAll(data.toTypedArray()) }
        assertAdd { addAll(data.toTypedArray().asIterable()) }
        assertAdd { addAll(data.asSequence()) }
    }

    test fun removeAll() {
        val content = arrayOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("foo")

        fun assertRemove(f: MutableList<String>.() -> Unit) = assertEquals(expected, arrayListOf(*content).let { it.f(); it })

        assertRemove { removeAll(data) }
        assertRemove { removeAll(data.toTypedArray()) }
        assertRemove { removeAll(data.toTypedArray().asIterable()) }
        assertRemove { removeAll(data.asSequence()) }
    }


    test fun retainAll() {
        val content = arrayOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("bar", "bar")

        fun assertRetain(f: MutableList<String>.() -> Unit) = assertEquals(expected, arrayListOf(*content).let { it.f(); it })

        assertRetain { retainAll(data) }
        assertRetain { retainAll(data.toTypedArray()) }
        assertRetain { retainAll(data.toTypedArray().asIterable()) }
        assertRetain { retainAll(data.asSequence()) }
    }

}
