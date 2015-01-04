package test.collections

import org.junit.Test
import java.util.ArrayList
import java.util.LinkedHashSet
import kotlin.test.*

fun <T> iterableOf(vararg items: T): Iterable<T> = IterableWrapper(items.toList())

class IterableWrapper<T>(collection: Iterable<T>) : Iterable<T> {
    private val collection = collection

    override fun iterator(): Iterator<T> {
        return collection.iterator()
    }
}

class IterableTest : IterableTests<Iterable<String>>(iterableOf("foo", "bar"), iterableOf<String>())
class SetTest : IterableTests<Set<String>>(setOf("foo", "bar"), setOf<String>())
class LinkedSetTest : IterableTests<LinkedHashSet<String>>(linkedSetOf("foo", "bar"), linkedSetOf<String>())
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

    Test fun indexOfFirst() {
        expect(-1) { data.indexOfFirst { it.contains("p") } }
        expect(0) { data.indexOfFirst { it.startsWith('f') } }
        expect(-1) { empty.indexOfFirst { it.startsWith('f') } }
    }

    Test fun indexOfLast() {
        expect(-1) { data.indexOfLast { it.contains("p") } }
        expect(1) { data.indexOfLast { it.length() == 3 } }
        expect(-1) { empty.indexOfLast { it.startsWith('f') } }
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
        expect(true) { data.all { it.length() == 3 } }
        expect(false) { data.all { it.startsWith("b") } }
        expect(true) { empty.all { it.startsWith("b") } }
    }

    Test fun none() {
        expect(false) { data.none() }
        expect(true) { empty.none() }
        expect(false) { data.none { it.length() == 3 } }
        expect(false) { data.none { it.startsWith("b") } }
        expect(true) { data.none { it.startsWith("x") } }
        expect(true) { empty.none { it.startsWith("b") } }
    }

    Test fun filter() {
        val foo = data.filter { it.startsWith("f") }
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("f") } }
        expect(1) { foo.size() }
        assertEquals(listOf("foo"), foo)
    }

    Test fun drop() {
        val foo = data.drop(1)
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size() }
        assertEquals(listOf("bar"), foo)
    }

    Test fun dropWhile() {
        val foo = data.dropWhile { it[0] == 'f' }
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size() }
        assertEquals(listOf("bar"), foo)
    }

    Test fun filterNot() {
        val notFoo = data.filterNot { it.startsWith("f") }
        expect(true) { notFoo is List<String> }
        expect(true) { notFoo.none { it.startsWith("f") } }
        expect(1) { notFoo.size() }
        assertEquals(listOf("bar"), notFoo)
    }

    Test fun forEach() {
        var count = 0
        data.forEach { count += it.length() }
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
            data.single { it.length() == 3 }
        }
    }

    Test
    fun singleOrNull() {
        expect(null) { data.singleOrNull() }
        expect(null) { empty.singleOrNull() }
        expect("foo") { data.singleOrNull { it.startsWith("f") } }
        expect("bar") { data.singleOrNull { it.startsWith("b") } }
        expect(null) {
            data.singleOrNull { it.length() == 3 }
        }
    }

    Test
    fun map() {
        val lengths = data.map { it.length() }
        assertTrue {
            lengths.all { it == 3 }
        }
        assertEquals(2, lengths.size())
        assertEquals(listOf(3, 3), lengths)
    }

    Test
    fun flatten() {
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), data.map { 0..it.length() }.flatten())
    }

    Test
    fun mapIndexed() {
        val shortened = data.mapIndexed {(index, value) -> value.substring(0..index) }
        assertEquals(2, shortened.size())
        assertEquals(listOf("f", "ba"), shortened)
    }

    Test
    fun withIndex() {
        val indexed = data.withIndex().map { it.value.substring(0..it.index) }
        assertEquals(2, indexed.size())
        assertEquals(listOf("f", "ba"), indexed)
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
    fun sumBy() {
        expect(6) { data.sumBy { it.length() } }
        expect(0) { empty.sumBy { it.length() } }

        expect(3.0) { data.sumByDouble { it.length().toDouble() / 2 } }
        expect(0.0) { empty.sumByDouble { it.length().toDouble() / 2 } }
    }

    Test
    fun withIndices() {
        var index = 0
        for ((i, d) in data.withIndex()) {
            assertEquals(i, index)
            assertEquals(d, data.elementAt(index))
            index++
        }
        assertEquals(data.count(), index)
    }

    Test
    fun fold() {
        expect(231) { data.fold(1, { a, b -> a + if (b == "foo") 200 else 30 }) }
    }

    Test
    fun reduce() {
        val reduced = data.reduce { a, b -> a + b }
        assertEquals(6, reduced.length())
        assertTrue(reduced == "foobar" || reduced == "barfoo")
    }
}
