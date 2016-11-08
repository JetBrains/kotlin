/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.collections

import org.junit.Test
import kotlin.test.*

fun <T> iterableOf(vararg items: T): Iterable<T> = Iterable { items.iterator() }
fun <T> Iterable<T>.toIterable(): Iterable<T> = Iterable { this.iterator() }

class IterableTest : OrderedIterableTests<Iterable<String>>(iterableOf("foo", "bar"), iterableOf<String>())
class SetTest : IterableTests<Set<String>>(setOf("foo", "bar"), setOf<String>())
class LinkedSetTest : IterableTests<LinkedHashSet<String>>(linkedSetOf("foo", "bar"), linkedSetOf<String>())
class ListTest : OrderedIterableTests<List<String>>(listOf("foo", "bar"), listOf<String>())
class ArrayListTest : OrderedIterableTests<ArrayList<String>>(arrayListOf("foo", "bar"), arrayListOf<String>())

abstract class OrderedIterableTests<T : Iterable<String>>(data: T, empty: T) : IterableTests<T>(data, empty) {
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

        expect("foo") { data.elementAtOrElse(0, {""} )}
        expect("zoo") { data.elementAtOrElse(-1, { "zoo" })}
        expect("zoo") { data.elementAtOrElse(2, { "zoo" })}
        expect("zoo") { empty.elementAtOrElse(0) { "zoo" }}

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
}

abstract class IterableTests<T : Iterable<String>>(val data: T, val empty: T) {
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
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("f") } }
        expect(1) { foo.size }
        assertEquals(listOf("foo"), foo)
    }

    @Test fun filterIndexed() {
        val result = data.filterIndexed { index, value -> value.first() == ('a' + index) }
        assertEquals(listOf("bar"), result)
    }

    @Test
    fun drop() {
        val foo = data.drop(1)
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size }
        assertEquals(listOf("bar"), foo)
    }

    @Test
    fun dropWhile() {
        val foo = data.dropWhile { it[0] == 'f' }
        expect(true) { foo is List<String> }
        expect(true) { foo.all { it.startsWith("b") } }
        expect(1) { foo.size }
        assertEquals(listOf("bar"), foo)
    }

    @Test
    fun filterNot() {
        val notFoo = data.filterNot { it.startsWith("f") }
        expect(true) { notFoo is List<String> }
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
    fun max() {
        expect("foo") { data.max() }
        expect("bar") { data.maxBy { it.last() } }
    }

    @Test
    fun min() {
        expect("bar") { data.min() }
        expect("foo") { data.minBy { it.last() } }
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
    fun mapAndJoinToString() {
        val result = data.joinToString(separator = "-") { it.toUpperCase() }
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


fun <T> Iterable<T>.assertSorted(isInOrder: (T, T) -> Boolean): Unit { this.iterator().assertSorted(isInOrder) }
fun <T> Iterator<T>.assertSorted(isInOrder: (T, T) -> Boolean) {
    if (!hasNext()) return
    var index = 0
    var prev = next()
    while (hasNext()) {
        index += 1
        val next = next()
        assertTrue(isInOrder(prev, next), "Not in order at position $index, element[${index-1}]: $prev, element[$index]: $next")
        prev = next
    }
    return
}

