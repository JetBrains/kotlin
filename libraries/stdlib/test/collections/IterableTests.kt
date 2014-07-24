package test.collections

import org.junit.Test
import kotlin.test.*
import java.util.*

fun <T> iterableOf(vararg items : T) : Iterable<T> = IterableWrapper(items.toList())

class IterableWrapper<T>(collection: Iterable<T>) : Iterable<T> {
    private val collection = collection

    override fun iterator(): Iterator<T> {
        return collection.iterator()
    }
}

class IterableTest : IterableTests<Iterable<String>>(iterableOf("foo", "bar"), iterableOf<String>())
class SetTest : IterableTests<Set<String>>(hashSetOf("foo", "bar"), hashSetOf<String>())
class ListTest : OrderedIterableTests<List<String>>(listOf("foo", "bar"), listOf<String>())
class ArrayListTest : OrderedIterableTests<ArrayList<String>>(arrayListOf("foo", "bar"), arrayListOf<String>())

abstract class OrderedIterableTests<T : Iterable<String>>(data: T, empty: T) : IterableTests<T>(data, empty) {
    Test fun indexOf() {
        expect(0) { data.indexOf("foo") }
        expect(-1) { empty.indexOf("foo") }
        expect(1) { data.indexOf("bar") }
        expect(-1) { data.indexOf("zap") }
    }

    Test fun lastIndexOf() {
        expect(0) { data.lastIndexOf("foo") }
        expect(-1) { empty.lastIndexOf("foo") }
        expect(1) { data.lastIndexOf("bar") }
        expect(-1) { data.lastIndexOf("zap") }
    }

    Test fun elementAt() {
        expect("foo") { data.elementAt(0) }
        expect("bar") { data.elementAt(1) }
        fails { data.elementAt(2) }
        fails { data.elementAt(-1) }
        fails { empty.elementAt(0) }
    }

    Test fun first() {
        expect("foo") { data.first() }
        fails {
            data.first { it.startsWith("x") }
        }
        fails {
            empty.first()
        }
        expect("foo") { data.first { it.startsWith("f") } }
    }

    Test fun firstOrNull() {
        expect(null) { data.firstOrNull { it.startsWith("x") } }
        expect(null) { empty.firstOrNull() }

        val f = data.firstOrNull { it.startsWith("f") }
        assertEquals("foo", f)
    }

    Test fun last() {
        assertEquals("bar", data.last())
        fails {
            data.last { it.startsWith("x") }
        }
        fails {
            empty.last()
        }
        expect("foo") { data.last { it.startsWith("f") } }
    }

    Test fun lastOrNull() {
        expect(null) { data.lastOrNull { it.startsWith("x") } }
        expect(null) { empty.lastOrNull() }
        expect("foo") { data.lastOrNull { it.startsWith("f") } }
    }
}

abstract class IterableTests<T : Iterable<String>>(val data: T, val empty: T) {
    Test fun any() {
        expect(true) { data.any() }
        expect(false) { empty.any() }
        expect(true) { data.any { it.startsWith("f") } }
        expect(false) { data.any { it.startsWith("x") } }
        expect(false) { empty.any { it.startsWith("x") } }
    }

    Test fun all() {
        expect(true) { data.all { it.length == 3 } }
        expect(false) { data.all { it.startsWith("b") } }
        expect(true) { empty.all { it.startsWith("b") } }
    }

    Test fun none() {
        expect(false) { data.none() }
        expect(true) { empty.none() }
        expect(false) { data.none { it.length == 3 } }
        expect(false) { data.none { it.startsWith("b") } }
        expect(true) { data.none { it.startsWith("x") } }
        expect(true) { empty.none { it.startsWith("b") } }
    }

    Test fun filter() {
        val foo = data.filter { it.startsWith("f") }
        // TODO uncomment this when KT-2468 will be fixed
        //expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("f") } }
        expect(1) { foo.size }
        assertEquals(listOf("foo"), foo)
    }

    Test fun filterNot() {
        val notFoo = data.filterNot { it.startsWith("f") }
        // TODO uncomment this when KT-2468 will be fixed
        //expect(true) { notFoo is List<String> }
        expect(true) { notFoo.none { it.startsWith("f") } }
        expect(1) { notFoo.size }
        assertEquals(listOf("bar"), notFoo)
    }

    Test fun forEach() {
        var count = 0
        data.forEach { count += it.length }
        assertEquals(6, count)
    }

    Test fun contains() {
        assertTrue(data.contains("foo"))
        assertTrue("bar" in data)
        assertTrue("baz" !in data)
        assertFalse("baz" in empty)
    }

    Test fun single() {
        fails { data.single() }
        fails { empty.single() }
        expect("foo") { data.single { it.startsWith("f") } }
        expect("bar") { data.single { it.startsWith("b") } }
        fails {
            data.single { it.length == 3 }
        }
    }

    Test
    fun singleOrNull() {
        expect(null) { data.singleOrNull() }
        expect(null) { empty.singleOrNull() }
        expect("foo") { data.singleOrNull { it.startsWith("f") } }
        expect("bar") { data.singleOrNull { it.startsWith("b") } }
        expect(null) {
            data.singleOrNull { it.length == 3 }
        }
    }

    Test
    fun map() {
        val lengths = data.map { it.length }
        assertTrue {
            lengths.all { it == 3 }
        }
        assertEquals(2, lengths.size)
        assertEquals(arrayListOf(3, 3), lengths)
    }

    Test
    fun max() {
        expect("foo") { data.max() }
        expect("bar") { data.maxBy { it.last() } }
    }

    Test
    fun min() {
        expect("bar") { data.min() }
        expect("foo") { data.minBy { it.last() } }
    }

    Test
    fun count() {
        expect(2) { data.count() }
        expect(0) { empty.count() }

        expect(1) { data.count { it.startsWith("f") } }
        expect(0) { empty.count { it.startsWith("f") } }

        expect(0) { data.count { it.startsWith("x") } }
        expect(0) { empty.count { it.startsWith("x") } }
    }

    Test
    fun withIndices() {
        var index = 0
        for ((i, d) in data.withIndices()) {
            assertEquals(i, index)
            assertEquals(d, data.elementAt(index))
            index++
        }
        assertEquals(data.count(), index)
    }

    Test
    fun fold() {

    }

    Test
    fun reduce() {

    }


}
