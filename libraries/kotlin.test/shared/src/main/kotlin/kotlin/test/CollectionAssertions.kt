package kotlin.test

import java.util.*

class CollectionAssertionSession<E, C: Iterable<E>>(val collection: C)

inline fun <E, C: Iterable<E>> assert(collection: C, block: CollectionAssertionSession<E, C>.() -> Unit) {
    CollectionAssertionSession(collection).block()
}

fun <C: Collection<*>> CollectionAssertionSession<*, C>.sizeShouldBe(expectedSize: Int, message: String? = null) {
    assertEquals(expectedSize, collection.size, message ?: "collection should have size $expectedSize but it is ${collection.size}")
}

fun <T> CollectionAssertionSession<T, *>.elementAtShouldBe(position: Int, expected: T, message: String? = null) {
    assertEquals(expected, collection.elementAt(position), message ?: "element at $position should be $expected")
}

fun <T, C: Iterable<T>> CollectionAssertionSession<T, C>.elementAtShouldComply(position: Int, message: String? = null, predicate: (T) -> Boolean) {
    assertTrue(message) { predicate(collection.elementAt(position)) }
}

fun <T> CollectionAssertionSession<T, *>.lastElementShouldBe(expected: T, message: String? = null) {
    assertEquals(expected, collection.last(), message ?: "the last element should be $expected")
}

fun <T> CollectionAssertionSession<T, *>.containsAll(vararg elements: T) {
    for (e in elements) {
        assertTrue(message = "Element $e is missing in the collection") { e in collection }
    }
}

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

    throw IllegalStateException()
}

private fun <T> Iterator<T>.remaining(): List<T> {
    val result = ArrayList<T>()
    while (hasNext()) {
        result.add(next())
    }
    return result
}