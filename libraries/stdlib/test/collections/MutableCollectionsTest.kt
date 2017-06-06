package test.collections

import kotlin.test.*

import org.junit.Test

class MutableCollectionTest {
    fun <T, C: MutableCollection<T>> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean, toMutableCollection: (List<T>) -> C)
            = fun(operation: (C.() -> Boolean)) {
                val list = toMutableCollection(before)
                assertEquals(expectedModified, list.operation())
                assertEquals(toMutableCollection(after), list)
            }

    fun <T> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean)
            = testOperation(before, after, expectedModified, { it.toMutableList() })


    @Test fun addAll() {
        val data = listOf("foo", "bar")

        testOperation(emptyList(), data, true).let { assertAdd ->
            assertAdd { addAll(data) }
            assertAdd { addAll(data.toTypedArray()) }
            assertAdd { addAll(data.toTypedArray().asIterable()) }
            assertAdd { addAll(data.asSequence()) }
        }

        testOperation(data, data, false, { it.toCollection(LinkedHashSet()) }).let { assertAdd ->
            assertAdd { addAll(data) }
            assertAdd { addAll(data.toTypedArray()) }
            assertAdd { addAll(data.toTypedArray().asIterable()) }
            assertAdd { addAll(data.asSequence()) }
        }
    }

    @Test fun removeAll() {
        val content = listOf("foo", "bar", "bar")
        val data = listOf("bar")
        val expected = listOf("foo")

        testOperation(content, expected, true).let { assertRemove ->
            assertRemove { removeAll(data) }
            assertRemove { removeAll(data.toTypedArray()) }
            assertRemove { removeAll(data.toTypedArray().asIterable()) }
            assertRemove { removeAll { it in data } }
            assertRemove { (this as MutableIterable<String>).removeAll { it in data } }
            val predicate = { cs: CharSequence -> cs.first() == 'b' }
            assertRemove { removeAll(predicate) }
        }


        testOperation(content, content, false).let { assertRemove ->
            assertRemove { removeAll(emptyList()) }
            assertRemove { removeAll(emptyArray()) }
            assertRemove { removeAll(emptySequence()) }
            assertRemove { removeAll { false } }
            assertRemove { (this as MutableIterable<String>).removeAll { false } }
        }
    }

    @Test fun retainAll() {
        val content = listOf("foo", "bar", "bar")
        val expected = listOf("bar", "bar")

        testOperation(content, expected, true).let { assertRetain ->
            val data = listOf("bar")
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }

            val predicate = { cs: CharSequence -> cs.first() == 'b' }
            assertRetain { retainAll(predicate) }
        }
        testOperation(content, emptyList(), true).let { assertRetain ->
            val data = emptyList<String>()
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
        }
        testOperation(emptyList<String>(), emptyList(), false).let { assertRetain ->
            val data = emptyList<String>()
            assertRetain { retainAll(data) }
            assertRetain { retainAll(data.toTypedArray()) }
            assertRetain { retainAll(data.toTypedArray().asIterable()) }
            assertRetain { retainAll(data.asSequence()) }
            assertRetain { retainAll { it in data } }
            assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
        }
    }

    @JvmVersion
    @Test fun listFill() {
        val list = MutableList(3) { it }
        list.fill(42)
        assertEquals(listOf(42, 42, 42), list)
    }

    @JvmVersion
    @Test fun shuffled() {
        val list = MutableList(100) { it }
        val shuffled = list.shuffled()

        assertNotEquals(list, shuffled)
        assertEquals(list.toSet(), shuffled.toSet())
        assertEquals(list.size, shuffled.distinct().size)
    }

    @JvmVersion
    @Test fun shuffledRnd() {
        val rnd1 = java.util.Random(42L)
        val rnd2 = java.util.Random(42L)

        val list = MutableList(100) { it }
        val shuffled1 = list.shuffled(rnd1)
        val shuffled2 = list.shuffled(rnd2)


        assertNotEquals(list, shuffled1)
        assertEquals(list.toSet(), shuffled1.toSet())
        assertEquals(list.size, shuffled1.distinct().size)

        assertEquals(shuffled1, shuffled2)
    }


}
