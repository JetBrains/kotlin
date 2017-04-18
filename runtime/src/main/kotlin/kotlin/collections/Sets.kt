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

package kotlin.collections


internal object EmptySet : Set<Nothing> {
    override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun iterator(): Iterator<Nothing> = EmptyIterator

    private fun readResolve(): Any = EmptySet
}


/** Returns an empty read-only set.  The returned set is serializable (JVM). */
public fun <T> emptySet(): Set<T> = EmptySet
/**
 * Returns a new read-only set with the given elements.
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> setOf(vararg elements: T): Set<T> = if (elements.size > 0) elements.toSet() else emptySet()

/** Returns an empty read-only set. */
@kotlin.internal.InlineOnly
public inline fun <T> setOf(): Set<T> = emptySet()

/**
 * Returns a new [MutableSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> mutableSetOf(vararg elements: T): MutableSet<T> = elements.toCollection(HashSet<T>(mapCapacity(elements.size)))

/** Returns a new [HashSet] with the given elements. */
public fun <T> hashSetOf(vararg elements: T): HashSet<T> = elements.toCollection(HashSet<T>(mapCapacity(elements.size)))

/**
 * Returns a new [LinkedHashSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> linkedSetOf(vararg elements: T): LinkedHashSet<T> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))

/** Returns this Set if it's not `null` and the empty set otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

// TODO: Add SingletonSet class
/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)


/**
 * Returns a new [SortedSet] with the given elements.
 */
// public fun <T> sortedSetOf(vararg elements: T): TreeSet<T> = elements.toCollection(TreeSet<T>())

/**
 * Returns a new [SortedSet] with the given [comparator] and elements.
 */
//public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg elements: T): TreeSet<T> = elements.toCollection(TreeSet<T>(comparator))


internal fun <T> Set<T>.optimizeReadOnlySet() = when (size) {
    0 -> emptySet()
    1 -> setOf(iterator().next())
    else -> this
}

/**
 * Returns a set containing all elements of the original set except the given [element].
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.minus(element: T): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity(size))
    var removed = false
    return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
}

/**
 * Returns a set containing all elements of the original set except the elements contained in the given [elements] array.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.minus(elements: Array<out T>): Set<T> {
    val result = LinkedHashSet<T>(this)
    result.removeAll(elements)
    return result
}

/**
 * Returns a set containing all elements of the original set except the elements contained in the given [elements] collection.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.minus(elements: Iterable<T>): Set<T> {
    val other = elements.convertToSetForSetOperationWith(this)
    if (other.isEmpty())
        return this.toSet()
    if (other is Set)
        return this.filterNotTo(LinkedHashSet<T>()) { it in other }
    val result = LinkedHashSet<T>(this)
    result.removeAll(other)
    return result
}

/**
 * Returns a set containing all elements of the original set except the elements contained in the given [elements] sequence.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.minus(elements: Sequence<T>): Set<T> {
    val result = LinkedHashSet<T>(this)
    result.removeAll(elements)
    return result
}

/**
 * Returns a set containing all elements of the original set except the given [element].
 *
 * The returned set preserves the element iteration order of the original set.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>.minusElement(element: T): Set<T> {
    return minus(element)
}

/**
 * Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.plus(element: T): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity(size + 1))
    result.addAll(this)
    result.add(element)
    return result
}

/**
 * Returns a set containing all elements of the original set and the given [elements] array,
 * which aren't already in this set.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.plus(elements: Array<out T>): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity(this.size + elements.size))
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a set containing all elements of the original set and the given [elements] collection,
 * which aren't already in this set.
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.plus(elements: Iterable<T>): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity(elements.collectionSizeOrNull()?.let { this.size + it } ?: this.size * 2))
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a set containing all elements of the original set and the given [elements] sequence,
 * which aren't already in this set.
 *
 * The returned set preserves the element iteration order of the original set.
 */
public operator fun <T> Set<T>.plus(elements: Sequence<T>): Set<T> {
    val result = LinkedHashSet<T>(mapCapacity(this.size * 2))
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a set containing all elements of the original set and then the given [element] if it isn't already in this set.
 *
 * The returned set preserves the element iteration order of the original set.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>.plusElement(element: T): Set<T> {
    return plus(element)
}

