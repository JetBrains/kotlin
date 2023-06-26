/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.*
import test.collections.js.linkedStringSetOf
import kotlin.test.*

fun <T> iterableOf(vararg items: T): Iterable<T> = Iterable { items.iterator() }
fun <T> Iterable<T>.toIterable(): Iterable<T> = Iterable { this.iterator() }

class IterableTest : OrderedIterableTests<Iterable<String>>({ iterableOf(*it) }, iterableOf<String>())
class SetTest : IterableTests<Set<String>>({ setOf(*it) }, setOf())
class LinkedSetTest : OrderedIterableTests<LinkedHashSet<String>>({ linkedSetOf(*it) }, linkedSetOf())
class LinkedStringSetTest : OrderedIterableTests<LinkedHashSet<String>>({ linkedStringSetOf(*it) }, linkedStringSetOf())
class ListTest : OrderedIterableTests<List<String>>({ listOf(*it) }, listOf<String>())
class ArrayListTest : OrderedIterableTests<ArrayList<String>>({ arrayListOf(*it) }, arrayListOf<String>())

abstract class OrderedIterableTests<T : Iterable<String>>(createFrom: (Array<out String>) -> T, empty: T) : IterableTests<T>(createFrom, empty) {
    @Test
    fun indexOf() {
        expect(0) { data.indexOf("foo") }
        expect(-1) { empty.indexOf("foo") }
        expect(1) { data.indexOf("bar") }
        expect(-1) { data.indexOf("zap") }
    }

    @Test
    fun lastIndexOf() {
        expect(0) { data.lastIndexOf("foo") }
        expect(-1) { empty.lastIndexOf("foo") }
        expect(1) { data.lastIndexOf("bar") }
        expect(-1) { data.lastIndexOf("zap") }
    }

    @Test
    fun indexOfFirst() {
        expect(-1) { data.indexOfFirst { it.contains("p") } }
        expect(0) { data.indexOfFirst { it.startsWith('f') } }
        expect(-1) { empty.indexOfFirst { it.startsWith('f') } }
    }

    @Test
    fun indexOfLast() {
        expect(-1) { data.indexOfLast { it.contains("p") } }
        expect(1) { data.indexOfLast { it.length == 3 } }
        expect(-1) { empty.indexOfLast { it.startsWith('f') } }
    }

    @Test
    fun elementAt() {
        expect("foo") { data.elementAt(0) }
        expect("bar") { data.elementAt(1) }
        assertFails { data.elementAt(2) }
        assertFails { data.elementAt(-1) }
        assertFails { empty.elementAt(0) }

        expect("foo") { data.elementAtOrElse(0, { "" }) }
        expect("zoo") { data.elementAtOrElse(-1, { "zoo" }) }
        expect("zoo") { data.elementAtOrElse(2, { "zoo" }) }
        expect("zoo") { empty.elementAtOrElse(0) { "zoo" } }

        expect(null) { empty.elementAtOrNull(0) }

    }

    @Test
    fun first() {
        expect("foo") { data.first() }
        assertFails {
            data.first { it.startsWith("x") }
        }
        assertFails {
            empty.first()
        }
        expect("foo") { data.first { it.startsWith("f") } }
    }

    @Test
    fun firstOrNull() {
        expect(null) { data.firstOrNull { it.startsWith("x") } }
        expect(null) { empty.firstOrNull() }

        val f = data.firstOrNull { it.startsWith("f") }
        assertEquals("foo", f)
    }

    @Test
    fun last() {
        assertEquals("bar", data.last())
        assertFails {
            data.last { it.startsWith("x") }
        }
        assertFails {
            empty.last()
        }
        expect("foo") { data.last { it.startsWith("f") } }
    }

    @Test
    fun lastOrNull() {
        expect(null) { data.lastOrNull { it.startsWith("x") } }
        expect(null) { empty.lastOrNull() }
        expect("foo") { data.lastOrNull { it.startsWith("f") } }
    }


    @Test
    fun zipWithNext() {
        val data = createFrom("", "a", "xyz")
        val lengthDeltas = data.zipWithNext { a: String, b: String -> b.length - a.length }
        assertEquals(listOf(1, 2), lengthDeltas)

        assertTrue(empty.zipWithNext { a: String, b: String -> a + b }.isEmpty())
        assertTrue(createFrom("foo").zipWithNext { a: String, b: String -> a + b }.isEmpty())
    }

    @Test
    fun zipWithNextPairs() {
        assertTrue(empty.zipWithNext().isEmpty())
        assertTrue(createFrom("foo").zipWithNext().isEmpty())
        assertEquals(listOf("a" to "b"), createFrom("a", "b").zipWithNext())
        assertEquals(listOf("a" to "b", "b" to "c"), createFrom("a", "b", "c").zipWithNext())
    }

    @Test
    fun chunked() {
        val size = 7
        val data = createFrom(Array(size) { "$it" })
        val result = data.chunked(4)
        assertEquals(listOf(
                listOf("0", "1", "2", "3"),
                listOf("4", "5", "6")
        ), result)

        val result2 = data.chunked(3) { it.joinToString("") }
        assertEquals(listOf("012", "345", "6"), result2)

        data.toList().let { expectedSingleChunk ->
            assertEquals(expectedSingleChunk, data.chunked(size).single())
            assertEquals(expectedSingleChunk, data.chunked(size + 3).single())
            assertEquals(expectedSingleChunk, data.chunked(Int.MAX_VALUE).single())
        }

        createFrom("a", "b").let { iterable ->
            assertEquals(iterable.toList(), iterable.chunked(Int.MAX_VALUE).single())
        }

        assertTrue(empty.chunked(3).isEmpty())

        for (illegalValue in listOf(Int.MIN_VALUE, -1, 0)) {
            assertFailsWith<IllegalArgumentException>("size $illegalValue") { data.chunked(illegalValue) }
        }
    }


    @Test
    fun windowed() {
        val size = 7
        val data = createFrom(Array(size) { "$it" })
        val result = data.windowed(4, 2)
        assertEquals(listOf(
                listOf("0", "1", "2", "3"),
                listOf("2", "3", "4", "5")
        ), result)

        val resultPartial = data.windowed(4, 2, partialWindows = true)
        assertEquals(listOf(
                listOf("0", "1", "2", "3"),
                listOf("2", "3", "4", "5"),
                listOf("4", "5", "6"),
                listOf("6")
        ), resultPartial)


        val result2 = data.windowed(2, 3) { it.joinToString("") }
        assertEquals(listOf("01", "34"), result2)

        val result2partial = data.windowed(2, 3, partialWindows = true) { it.joinToString("") }
        assertEquals(listOf("01", "34", "6"), result2partial)

        assertEquals(data.chunked(2), data.windowed(2, 2, partialWindows = true))

        assertEquals(data.take(2), data.windowed(2, size).single())
        assertEquals(data.take(3), data.windowed(3, size + 3).single())

        assertEquals(data.toList(), data.windowed(size, 1).single())
        assertTrue(data.windowed(size + 1, 1).isEmpty())

        val result3partial = data.windowed(size, 1, partialWindows = true)
        result3partial.forEachIndexed { index, window ->
            assertEquals(size - index, window.size, "size of window#$index")
        }

        assertTrue(empty.windowed(3, 2).isEmpty())

        for (illegalValue in listOf(Int.MIN_VALUE, -1, 0)) {
            assertFailsWith<IllegalArgumentException>("size $illegalValue") { data.windowed(illegalValue, 1) }
            assertFailsWith<IllegalArgumentException>("step $illegalValue") { data.windowed(1, illegalValue) }
        }

        // index overflow tests
        for (partialWindows in listOf(true, false)) {

            val windowed1 = data.windowed(5, Int.MAX_VALUE, partialWindows)
            assertEquals(data.take(5), windowed1.single())
            val windowed2 = data.windowed(Int.MAX_VALUE, 5, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toList(), listOf("5", "6")) else listOf(), windowed2)
            val windowed3 = data.windowed(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows)
            assertEquals(if (partialWindows) listOf(data.toList()) else listOf(), windowed3)

            val windowedTransform1 = data.windowed(5, Int.MAX_VALUE, partialWindows) { it.joinToString("") }
            assertEquals("01234", windowedTransform1.single())
            val windowedTransform2 = data.windowed(Int.MAX_VALUE, 5, partialWindows) { it.joinToString("") }
            assertEquals(if (partialWindows) listOf("0123456", "56") else listOf(), windowedTransform2)
            val windowedTransform3 = data.windowed(Int.MAX_VALUE, Int.MAX_VALUE, partialWindows) { it.joinToString("") }
            assertEquals(if (partialWindows) listOf("0123456") else listOf(), windowedTransform3)
        }
    }


}

abstract class IterableTests<T : Iterable<String>>(val createFrom: (Array<out String>) -> T, val empty: T) {
    fun createFrom(vararg items: String): T = createFrom(items)

    val data = createFrom("foo", "bar")

    @Test
    fun any() {
        expect(true) { data.any() }
        expect(false) { empty.any() }
        expect(true) { data.any { it.startsWith("f") } }
        expect(false) { data.any { it.startsWith("x") } }
        expect(false) { empty.any { it.startsWith("x") } }
    }

    @Test
    fun all() {
        expect(true) { data.all { it.length == 3 } }
        expect(false) { data.all { it.startsWith("b") } }
        expect(true) { empty.all { it.startsWith("b") } }
    }

    @Test
    fun none() {
        expect(false) { data.none() }
        expect(true) { empty.none() }
        expect(false) { data.none { it.length == 3 } }
        expect(false) { data.none { it.startsWith("b") } }
        expect(true) { data.none { it.startsWith("x") } }
        expect(true) { empty.none { it.startsWith("b") } }
    }

    @Test
    fun filter() {
        val foo = data.filter { it.startsWith("f") }
        assertStaticAndRuntimeTypeIs<List<String>>(foo)
        expect(true) { foo.all { it.startsWith("f") } }
        expect(1) { foo.size }
        assertEquals(listOf("foo"), foo)
    }

    @Test
    fun filterIndexed() {
        val result = data.filterIndexed { index, value -> value.first() == ('a' + index) }
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun drop() {
        val foo = data.drop(1)
        assertStaticAndRuntimeTypeIs<List<String>>(foo)
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size }
        assertEquals(listOf("bar"), foo)
    }

    @Test
    fun dropWhile() {
        val foo = data.dropWhile { it[0] == 'f' }
        assertStaticAndRuntimeTypeIs<List<String>>(foo)
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size }
        assertEquals(listOf("bar"), foo)
    }

    @Test
    fun filterNot() {
        val notFoo = data.filterNot { it.startsWith("f") }
        assertStaticAndRuntimeTypeIs<List<String>>(notFoo)
        expect(true) { notFoo.none { it.startsWith("f") } }
        expect(1) { notFoo.size }
        assertEquals(listOf("bar"), notFoo)
    }

    @Test
    fun forEach() {
        var count = 0
        data.forEach { count += it.length }
        assertEquals(6, count)
    }

    @Test
    fun onEach() {
        var count = 0
        val newData = data.onEach { count += it.length }
        assertEquals(6, count)
        assertTrue(data === newData)

        // static types test
        assertStaticTypeIs<ArrayList<Int>>(arrayListOf(1, 2, 3).onEach { })
    }

    @Test
    fun onEachIndexed() {
        var count = 0
        val newData = data.onEachIndexed { i, e -> count += i + e.length }
        assertEquals(7, count)
        assertSame(data, newData)

        // static types test
        assertStaticTypeIs<ArrayList<Int>>(arrayListOf(1, 2, 3).onEachIndexed { _, _ -> })
    }

    @Test
    fun contains() {
        assertTrue(data.contains("foo"))
        assertTrue("bar" in data)
        assertTrue("baz" !in data)
        assertFalse("baz" in empty)
    }

    @Test
    fun single() {
        assertFails { data.single() }
        assertFails { empty.single() }
        expect("foo") { data.single { it.startsWith("f") } }
        expect("bar") { data.single { it.startsWith("b") } }
        assertFails {
            data.single { it.length == 3 }
        }
    }

    @Test
    fun singleOrNull() {
        expect(null) { data.singleOrNull() }
        expect(null) { empty.singleOrNull() }
        expect("foo") { data.singleOrNull { it.startsWith("f") } }
        expect("bar") { data.singleOrNull { it.startsWith("b") } }
        expect(null) {
            data.singleOrNull { it.length == 3 }
        }
    }

    @Test
    fun map() {
        val lengths = data.map { it.length }
        assertTrue {
            lengths.all { it == 3 }
        }
        assertEquals(2, lengths.size)
        assertEquals(listOf(3, 3), lengths)
    }

    @Test
    fun flatten() {
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), data.map { 0..it.length }.flatten())
    }

    @Test
    fun mapIndexed() {
        val shortened = data.mapIndexed { index, value -> value.substring(0..index) }
        assertEquals(2, shortened.size)
        assertEquals(listOf("f", "ba"), shortened)
    }

    @Test
    fun withIndex() {
        val indexed = data.withIndex().map { it.value.substring(0..it.index) }
        assertEquals(2, indexed.size)
        assertEquals(listOf("f", "ba"), indexed)
    }

    @Test
    fun mapNotNull() {
        assertEquals(listOf('o'), data.mapNotNull { it.firstOrNull { c -> c in "oui" } })
    }

    @Test
    fun mapIndexedNotNull() {
        assertEquals(listOf('b'), data.mapIndexedNotNull { index, s -> s.getOrNull(index - 1) })
    }

    @Test
    fun maxOrNull() {
        expect("foo") { data.maxOrNull() }
        expect("bar") { data.maxByOrNull { it.last() } }
    }

    @Test
    fun minOrNull() {
        expect("bar") { data.minOrNull() }
        expect("foo") { data.minByOrNull { it.last() } }
    }

    @Test
    fun count() {
        expect(2) { data.count() }
        expect(0) { empty.count() }

        expect(1) { data.count { it.startsWith("f") } }
        expect(0) { empty.count { it.startsWith("f") } }

        expect(0) { data.count { it.startsWith("x") } }
        expect(0) { empty.count { it.startsWith("x") } }
    }

    @Suppress("DEPRECATION")
    @Test
    fun sumBy() {
        expect(6) { data.sumBy { it.length } }
        expect(0) { empty.sumBy { it.length } }

        expect(3.0) { data.sumByDouble { it.length.toDouble() / 2 } }
        expect(0.0) { empty.sumByDouble { it.length.toDouble() / 2 } }
    }

    @Test
    fun withIndices() {
        var index = 0
        for ((i, d) in data.withIndex()) {
            assertEquals(i, index)
            assertEquals(d, data.elementAt(index))
            index++
        }
        assertEquals(data.count(), index)
    }

    @Test
    fun fold() {
        expect(231) { data.fold(1, { a, b -> a + if (b == "foo") 200 else 30 }) }
    }

    @Test
    fun reduce() {
        val reduced = data.reduce { a, b -> a + b }
        assertEquals(6, reduced.length)
        assertTrue(reduced == "foobar" || reduced == "barfoo")
    }

    @Test
    fun scan() {
        val accumulators = data.scan("baz") { acc, e -> acc + e }
        assertEquals(3, accumulators.size)
        assertEquals("baz", accumulators.first())
        assertTrue(accumulators.elementAt(1) in listOf("bazfoo", "bazbar"))
        assertTrue(accumulators.last() in listOf("bazfoobar", "bazbarfoo"))
    }

    @Test
    fun scanIndexed() {
        val accumulators = data.scanIndexed("baz") { i, acc, e -> acc + i + e }
        assertEquals(3, accumulators.size)
        assertEquals("baz", accumulators.first())
        assertTrue(accumulators.elementAt(1) in listOf("baz0foo", "baz0bar"))
        assertTrue(accumulators.last() in listOf("baz0foo1bar", "baz0bar1foo"))
    }

    @Test
    fun runningReduce() {
        val accumulators = data.runningReduce { acc, e -> acc + e }
        assertEquals(2, accumulators.size)
        assertTrue(accumulators.first() in listOf("foo", "bar"))
        assertTrue(accumulators.last() in listOf("foobar", "barfoo"))
    }

    @Test
    fun runningReduceIndexed() {
        val accumulators = data.runningReduceIndexed { i, acc, e -> acc + i + e }
        assertEquals(2, accumulators.size)
        assertTrue(accumulators.first() in listOf("foo", "bar"))
        assertTrue(accumulators.last() in listOf("foo1bar", "bar1foo"))
    }

    @Test
    fun mapAndJoinToString() {
        val result = data.joinToString(separator = "-") { it.uppercase() }
        assertEquals("FOO-BAR", result)
    }

    fun testPlus(doPlus: (Iterable<String>) -> List<String>) {
        val result: List<String> = doPlus(data)
        assertEquals(listOf("foo", "bar", "zoo", "g"), result)
        assertFalse(result === data)
    }

    @Test
    fun plusElement() = testPlus { it + "zoo" + "g" }
    @Test
    fun plusCollection() = testPlus { it + listOf("zoo", "g") }
    @Test
    fun plusArray() = testPlus { it + arrayOf("zoo", "g") }
    @Test
    fun plusSequence() = testPlus { it + sequenceOf("zoo", "g") }

    @Test
    fun plusAssign() {
        // lets use a mutable variable
        var result: Iterable<String> = data
        result += "foo"
        result += listOf("beer")
        result += arrayOf("cheese", "wine")
        result += sequenceOf("zoo", "g")
        assertEquals(listOf("foo", "bar", "foo", "beer", "cheese", "wine", "zoo", "g"), result)
    }

    @Test
    fun minusElement() {
        val result = data - "foo" - "g"
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun minusCollection() {
        val result = data - listOf("foo", "g")
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun minusArray() {
        val result = data - arrayOf("foo", "g")
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun minusSequence() {
        val result = data - sequenceOf("foo", "g")
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun minusAssign() {
        // lets use a mutable variable
        var result: Iterable<String> = data
        result -= "foo"
        assertEquals(listOf("bar"), result as? List)
        result = data
        result -= listOf("beer", "bar")
        assertEquals(listOf("foo"), result as? List)
        result = data
        result -= arrayOf("bar", "foo")
        assertEquals(emptyList<String>(), result as? List)
        result = data
        result -= sequenceOf("foo", "g")
        assertEquals(listOf("bar"), result as? List)
    }

}


fun <T> Iterable<T>.assertSorted(isInOrder: (T, T) -> Boolean) {
    this.iterator().assertSorted(isInOrder)
}

fun <T> Iterator<T>.assertSorted(isInOrder: (T, T) -> Boolean) {
    if (!hasNext()) return
    var index = 0
    var prev = next()
    while (hasNext()) {
        index += 1
        val next = next()
        assertTrue(isInOrder(prev, next), "Not in order at position $index, element[${index - 1}]: $prev, element[$index]: $next")
        prev = next
    }
    return
}

data class Sortable<K : Comparable<K>>(val key: K, val index: Int) : Comparable<Sortable<K>> {
    override fun compareTo(other: Sortable<K>): Int = this.key compareTo other.key
}


fun <K : Comparable<K>> Iterator<Sortable<K>>.assertStableSorted(descending: Boolean = false) {
    assertSorted { a, b ->
        val relation = a.key compareTo b.key
        (if (descending) relation > 0 else relation < 0) || relation == 0 && a.index < b.index
    }
}

fun <K : Comparable<K>> Iterable<Sortable<K>>.assertStableSorted(descending: Boolean = false) =
    iterator().assertStableSorted(descending = descending)
