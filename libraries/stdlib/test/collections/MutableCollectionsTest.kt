package test.collections

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    @test fun addAll() {
        val data = listOf("foo", "bar")

        fun assertAdd(f: MutableList<String>.() -> Unit) = assertEquals(data, arrayListOf<String>().apply(f))

        assertAdd { addAll(data) }
        assertAdd { addAll(data.toTypedArray()) }
        assertAdd { addAll(data.toTypedArray().asIterable()) }
        assertAdd { addAll(data.asSequence()) }
    }

    @test fun removeAll() {
        val content = arrayOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("foo")

        fun assertRemove(f: MutableList<String>.() -> Unit) = assertEquals(expected, content.toArrayList().apply(f))

        assertRemove { removeAll(data) }
        assertRemove { removeAll(data.toTypedArray()) }
        assertRemove { removeAll(data.toTypedArray().asIterable()) }
        assertRemove { removeAll(data.asSequence()) }
        assertRemove { removeAll { it in data } }
        assertRemove { (this as MutableIterable<String>).removeAll { it in data } }

        val predicate = { cs: CharSequence -> cs.first() == 'b' }
        assertRemove { removeAll(predicate) }
    }


    @test fun retainAll() {
        val content = arrayOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("bar", "bar")

        fun assertRetain(f: MutableList<String>.() -> Unit) = assertEquals(expected, content.toArrayList().apply(f))

        assertRetain { retainAll(data) }
        assertRetain { retainAll(data.toTypedArray()) }
        assertRetain { retainAll(data.toTypedArray().asIterable()) }
        assertRetain { retainAll(data.asSequence()) }
        assertRetain { retainAll { it in data } }
        assertRetain { (this as MutableIterable<String>).retainAll { it in data } }

        val predicate = { cs: CharSequence -> cs.first() == 'b' }
        assertRetain { retainAll(predicate) }
    }

}
