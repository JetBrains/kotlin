/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:Suppress("DEPRECATION")
package kotlin.test

import java.util.*

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
class CollectionAssertionSession<E, C: Iterable<E>>(val collection: C)

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
inline fun <E, C: Iterable<E>> assert(collection: C, block: CollectionAssertionSession<E, C>.() -> Unit) {
    CollectionAssertionSession(collection).block()
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <C: Collection<*>> CollectionAssertionSession<*, C>.sizeShouldBe(expectedSize: Int, message: String? = null) {
    assertEquals(expectedSize, collection.size, message ?: "collection should have size $expectedSize but it is ${collection.size}")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T> CollectionAssertionSession<T, *>.elementAtShouldBe(position: Int, expected: T, message: String? = null) {
    assertEquals(expected, collection.elementAt(position), message ?: "element at $position should be $expected")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T, C: Iterable<T>> CollectionAssertionSession<T, C>.elementAtShouldComply(position: Int, message: String? = null, predicate: (T) -> Boolean) {
    assertTrue(message) { predicate(collection.elementAt(position)) }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T> CollectionAssertionSession<T, *>.lastElementShouldBe(expected: T, message: String? = null) {
    assertEquals(expected, collection.last(), message ?: "the last element should be $expected")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T> CollectionAssertionSession<T, *>.containsAll(vararg elements: T) {
    for (e in elements) {
        assertTrue(message = "Element $e is missing in the collection") { e in collection }
    }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T, C: Iterable<T>> CollectionAssertionSession<T, C>.shouldBe(expectedElements: Iterable<T>, message: String? = null) {
    val actual = collection.iterator()
    val expected = expectedElements.iterator()

    while (actual.hasNext() && expected.hasNext()) {
        assertEquals(expected.next(), actual.next(), message)
    }

    if (actual.hasNext()) {
        fail("Actual collection is longer than expected. Extra elements are: ${actual.remaining()}")
    }
    if (expected.hasNext()) {
        fail("Actual collection is shorter than expected. Missing elements are: ${expected.remaining()}")
    }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T, C: Set<T>> CollectionAssertionSession<T, C>.shouldBeSet(other: Set<T>, message: String? = null) {
    for (e in other) {
        if (e !in collection) {
            fail(message ?: "Element $e in not in the collection $collection")
        }
    }
    for (e in collection) {
        if (e !in other) {
            fail(message ?: "Element $e is not expected")
        }
    }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.")
fun <T, C: Set<T>> CollectionAssertionSession<T, C>.shouldBeSet(vararg other: T) {
    val otherSet = HashSet<T>()
    for (e in other) {
        otherSet.add(e)
    }

    shouldBeSet(otherSet)
}

private operator fun <T> Iterable<T>.contains(e: T): Boolean {
    if (this is Set<T>) {
        return contains(e)
    }
    for (it in this) {
        if (it == e) {
            return true
        }
    }
    return false
}

private fun <T> Iterable<T>.last(): T {
    if (this is List<T>) {
        if (this.isEmpty()) {
            throw NoSuchElementException()
        }

        return this[size - 1]
    }

    val it = iterator()
    var result: T = iterator().next()

    while (it.hasNext()) {
        result = it.next()
    }

    return result
}

private fun <T> Iterable<T>.elementAt(position: Int): T {
    if (position < 0) {
        throw IllegalArgumentException("position shouldn't be negative: $position")
    }
    if (this is List<T>) {
        return this[position]
    }

    val iterator = iterator()
    var idx = 0
    do {
        if (!iterator.hasNext()) {
            throw IndexOutOfBoundsException("index $position is out of the collection bounds [0; $idx)")
        }
        val result = iterator.next()
        if (idx == position) {
            return result
        }

        idx++
    } while (true)
}

private fun <T> Iterator<T>.remaining(): List<T> {
    val result = ArrayList<T>()
    while (hasNext()) {
        result.add(next())
    }
    return result
}