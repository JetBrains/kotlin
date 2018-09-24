/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")
package kotlin.test

import java.util.*

// TODO: Drop entirely in 1.4

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
class CollectionAssertionSession<E, C: Iterable<E>>(val collection: C)

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
inline fun <E, C: Iterable<E>> assert(collection: C, block: CollectionAssertionSession<E, C>.() -> Unit) {
    CollectionAssertionSession(collection).block()
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <C: Collection<*>> CollectionAssertionSession<*, C>.sizeShouldBe(expectedSize: Int, message: String? = null) {
    assertEquals(expectedSize, collection.size, message ?: "collection should have size $expectedSize but it is ${collection.size}")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <T> CollectionAssertionSession<T, *>.elementAtShouldBe(position: Int, expected: T, message: String? = null) {
    assertEquals(expected, collection.elementAt(position), message ?: "element at $position should be $expected")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <T, C: Iterable<T>> CollectionAssertionSession<T, C>.elementAtShouldComply(position: Int, message: String? = null, predicate: (T) -> Boolean) {
    assertTrue(message) { predicate(collection.elementAt(position)) }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <T> CollectionAssertionSession<T, *>.lastElementShouldBe(expected: T, message: String? = null) {
    assertEquals(expected, collection.last(), message ?: "the last element should be $expected")
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <T> CollectionAssertionSession<T, *>.containsAll(vararg elements: T) {
    for (e in elements) {
        assertTrue(message = "Element $e is missing in the collection") { e in collection }
    }
}

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
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

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
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

@Deprecated("This is an experimental part of the API. It may be changed or removed in newer releases.", level = DeprecationLevel.ERROR)
fun <T, C: Set<T>> CollectionAssertionSession<T, C>.shouldBeSet(vararg other: T) {
    val otherSet = HashSet<T>()
    for (e in other) {
        otherSet.add(e)
    }

    shouldBeSet(otherSet)
}

private fun <T> Iterator<T>.remaining(): List<T> {
    val result = ArrayList<T>()
    while (hasNext()) {
        result.add(next())
    }
    return result
}