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

import kotlin.comparisons.*

internal object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

internal object EmptyList : List<Nothing>/*, RandomAccess */ {

    override fun equals(other: Any?): Boolean = other is List<*> && other.isEmpty()
    override fun hashCode(): Int = 1
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun get(index: Int): Nothing = throw IndexOutOfBoundsException("Empty list doesn't contain element at index $index.")
    override fun indexOf(element: Nothing): Int = -1
    override fun lastIndexOf(element: Nothing): Int = -1

    override fun iterator(): Iterator<Nothing> = EmptyIterator
    override fun listIterator(): ListIterator<Nothing> = EmptyIterator
    override fun listIterator(index: Int): ListIterator<Nothing> {
        if (index != 0) throw IndexOutOfBoundsException("Index: $index")
        return EmptyIterator
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> {
        if (fromIndex == 0 && toIndex == 0) return this
        throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex")
    }

    private fun readResolve(): Any = EmptyList
}

internal fun <T> Array<out T>.asCollection(): Collection<T> = ArrayAsCollection(this, isVarargs = false)

private class ArrayAsCollection<T>(val values: Array<out T>, val isVarargs: Boolean): Collection<T> {
    override val size: Int get() = values.size
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun contains(element: T): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): Iterator<T> = values.iterator()
    // override hidden toArray implementation to prevent copying of values array
    public fun toArray(): Array<out Any?> = values.copyToArrayOfAny(isVarargs)
}

/** Returns an empty read-only list.  */
public fun <T> emptyList(): List<T> = EmptyList

/** Returns a new read-only list of given elements. */
public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()

/** Returns an empty read-only list. */
@kotlin.internal.InlineOnly
public inline fun <T> listOf(): List<T> = emptyList()

/** Returns a new [MutableList] with the given elements. */
public fun <T> mutableListOf(vararg elements: T): MutableList<T>
        = if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

// This part is from generated _Collections.kt.

/** Returns a new [ArrayList] with the given elements. */
public fun <T> arrayListOf(vararg elements: T): ArrayList<T>
        = if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

/** Returns a new read-only list either of single given element, if it is not null,
 * or empty list it the element is null.*/
public fun <T : Any> listOfNotNull(element: T?): List<T> =
        if (element != null) listOf(element) else emptyList()

/** Returns a new read-only list only of those given elements, that are not null. */
public fun <T : Any> listOfNotNull(vararg elements: T?): List<T> = elements.filterNotNull()

/**
 * Returns an [IntRange] of the valid indices for this collection.
 */
public val Collection<*>.indices: IntRange
    get() = 0..size - 1

/**
 * Returns the index of the last item in the list or -1 if the list is empty.
 *
 * @sample samples.collections.Collections.Lists.lastIndexOfList
 */
public val <T> List<T>.lastIndex: Int
    get() = this.size - 1

/** Returns `true` if the collection is not empty. */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.isNotEmpty(): Boolean = !isEmpty()

/** Returns this Collection if it's not `null` and the empty list otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>?.orEmpty(): Collection<T> = this ?: emptyList()

@kotlin.internal.InlineOnly
public inline fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/**
 * Checks if all elements in the specified collection are contained in this collection.
 *
 * Allows to overcome type-safety restriction of `containsAll` that requires to pass a collection of type `Collection<E>`.
 */
@kotlin.internal.InlineOnly
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline fun <@kotlin.internal.OnlyInputTypes T> Collection<T>.containsAll(
        elements: Collection<T>): Boolean = this.containsAll(elements)

// copies typed varargs array to array of objects
// TODO: generally wrong, wrt specialization.
@FixmeSpecialization
private fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<Any?> =
        if (isVarargs)
            // if the array came from varargs and already is array of Any, copying isn't required.
            @Suppress("UNCHECKED_CAST") (this as Array<Any?>)
        else
            @Suppress("UNCHECKED_CAST") (this.copyOfUninitializedElements(this.size) as Array<Any?>)


/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over.
 * @param T the type of element being iterated over.
 */
public interface Iterable<out T> {
    /**
     * Returns an iterator over the elements of this object.
     */
    public operator fun iterator(): Iterator<T>
}

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over and that supports removing elements during iteration.
 */
public interface MutableIterable<out T> : Iterable<T> {
    /**
     * Returns an iterator over the elementrs of this sequence that supports removing elements during iteration.
     */
    override fun iterator(): MutableIterator<T>
}

public fun <T> Array<out T>.asList(): List<T> {
    return object : AbstractList<T>() {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: T): Boolean = this@asList.contains(element)
        override fun get(index: Int): T = this@asList[index]
        override fun indexOf(element: T): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun ByteArray.asList(): List<Byte> {
    return object : AbstractList<Byte>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Byte): Boolean = this@asList.contains(element)
        override fun get(index: Int): Byte = this@asList[index]
        override fun indexOf(element: Byte): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Byte): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun ShortArray.asList(): List<Short> {
    return object : AbstractList<Short>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Short): Boolean = this@asList.contains(element)
        override fun get(index: Int): Short = this@asList[index]
        override fun indexOf(element: Short): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Short): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun IntArray.asList(): List<Int> {
    return object : AbstractList<Int>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Int): Boolean = this@asList.contains(element)
        override fun get(index: Int): Int = this@asList[index]
        override fun indexOf(element: Int): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Int): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun LongArray.asList(): List<Long> {
    return object : AbstractList<Long>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Long): Boolean = this@asList.contains(element)
        override fun get(index: Int): Long = this@asList[index]
        override fun indexOf(element: Long): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Long): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun FloatArray.asList(): List<Float> {
    return object : AbstractList<Float>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Float): Boolean = this@asList.contains(element)
        override fun get(index: Int): Float = this@asList[index]
        override fun indexOf(element: Float): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Float): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun DoubleArray.asList(): List<Double> {
    return object : AbstractList<Double>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Double): Boolean = this@asList.contains(element)
        override fun get(index: Int): Double = this@asList[index]
        override fun indexOf(element: Double): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Double): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun BooleanArray.asList(): List<Boolean> {
    return object : AbstractList<Boolean>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Boolean): Boolean = this@asList.contains(element)
        override fun get(index: Int): Boolean = this@asList[index]
        override fun indexOf(element: Boolean): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Boolean): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Returns a [List] that wraps the original array.
 */
public fun CharArray.asList(): List<Char> {
    return object : AbstractList<Char>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Char): Boolean = this@asList.contains(element)
        override fun get(index: Int): Char = this@asList[index]
        override fun indexOf(element: Char): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Char): Int = this@asList.lastIndexOf(element)
    }
}


@FixmeVariance
public fun <T, C : MutableCollection</*in */T>> Iterable<T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

@Fixme
internal fun <T> List<T>.optimizeReadOnlyList() = this

// From generated _Collections.kt.
/////////

/**
 * Returns 1st *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> List<T>.component1(): T {
    return get(0)
}

/**
 * Returns 2nd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> List<T>.component2(): T {
    return get(1)
}

/**
 * Returns 3rd *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> List<T>.component3(): T {
    return get(2)
}

/**
 * Returns 4th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> List<T>.component4(): T {
    return get(3)
}

/**
 * Returns 5th *element* from the collection.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> List<T>.component5(): T {
    return get(4)
}

/**
 * Returns `true` if [element] is found in the collection.
 */
public operator fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.contains(element: T): Boolean {
    if (this is Collection)
        return contains(element)
    return indexOf(element) >= 0
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this collection.
 */
public fun <T> Iterable<T>.elementAt(index: Int): T {
    if (this is List)
        return get(index)
    return elementAtOrElse(index) { throw IndexOutOfBoundsException("Collection doesn't contain element at index $index.") }
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this list.
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>.elementAt(index: Int): T {
    return get(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this collection.
 */
public fun <T> Iterable<T>.elementAtOrElse(index: Int, defaultValue: (Int) -> T): T {
    if (this is List)
        return this.getOrElse(index, defaultValue)
    if (index < 0)
        return defaultValue(index)
    val iterator = iterator()
    var count = 0
    while (iterator.hasNext()) {
        val element = iterator.next()
        if (index == count++)
            return element
    }
    return defaultValue(index)
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this list.
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>.elementAtOrElse(index: Int, defaultValue: (Int) -> T): T {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this collection.
 */
public fun <T> Iterable<T>.elementAtOrNull(index: Int): T? {
    if (this is List)
        return this.getOrNull(index)
    if (index < 0)
        return null
    val iterator = iterator()
    var count = 0
    while (iterator.hasNext()) {
        val element = iterator.next()
        if (index == count++)
            return element
    }
    return null
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this list.
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>.elementAtOrNull(index: Int): T? {
    return this.getOrNull(index)
}

/**
 * Returns the first element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.find(predicate: (T) -> Boolean): T? {
    return firstOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.findLast(predicate: (T) -> Boolean): T? {
    return lastOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>.findLast(predicate: (T) -> Boolean): T? {
    return lastOrNull(predicate)
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the collection is empty.
 */
public fun <T> Iterable<T>.first(): T {
    when (this) {
        is List -> return this.first()
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Collection is empty.")
            return iterator.next()
        }
    }
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the list is empty.
 */
public fun <T> List<T>.first(): T {
    if (isEmpty())
        throw NoSuchElementException("List is empty.")
    return this[0]
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Iterable<T>.first(predicate: (T) -> Boolean): T {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Collection contains no element matching the predicate.")
}

/**
 * Returns the first element, or `null` if the collection is empty.
 */
public fun <T> Iterable<T>.firstOrNull(): T? {
    when (this) {
        is List -> {
            if (isEmpty())
                return null
            else
                return this[0]
        }
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            return iterator.next()
        }
    }
}

/**
 * Returns the first element, or `null` if the list is empty.
 */
public fun <T> List<T>.firstOrNull(): T? {
    return if (isEmpty()) null else this[0]
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun <T> Iterable<T>.firstOrNull(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this list.
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>.getOrElse(index: Int, defaultValue: (Int) -> T): T {
    return if (index >= 0 && index <= lastIndex) get(index) else defaultValue(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this list.
 */
public fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index >= 0 && index <= lastIndex) get(index) else null
}

/**
 * Returns first index of [element], or -1 if the collection does not contain element.
 */
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.indexOf(element: T): Int {
    if (this is List) return this.indexOf(element)
    var index = 0
    for (item in this) {
        if (element == item)
            return index
        index++
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the list does not contain element.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public fun <@kotlin.internal.OnlyInputTypes T> List<T>.indexOf(element: T): Int {
    return indexOf(element)
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Iterable<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the list does not contain such element.
 */
public inline fun <T> List<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Iterable<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    var lastIndex = -1
    var index = 0
    for (item in this) {
        if (predicate(item))
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the list does not contain such element.
 */
public inline fun <T> List<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        if (predicate(iterator.previous())) {
            return iterator.nextIndex()
        }
    }
    return -1
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the collection is empty.
 */
public fun <T> Iterable<T>.last(): T {
    when (this) {
        is List -> return this.last()
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Collection is empty.")
            var last = iterator.next()
            while (iterator.hasNext())
                last = iterator.next()
            return last
        }
    }
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the list is empty.
 */
public fun <T> List<T>.last(): T {
    if (isEmpty())
        throw NoSuchElementException("List is empty.")
    return this[lastIndex]
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Iterable<T>.last(predicate: (T) -> Boolean): T {
    var last: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            last = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Collection contains no element matching the predicate.")
    return last as T
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> List<T>.last(predicate: (T) -> Boolean): T {
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        val element = iterator.previous()
        if (predicate(element)) return element
    }
    throw NoSuchElementException("List contains no element matching the predicate.")
}

/**
 * Returns last index of [element], or -1 if the collection does not contain element.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.lastIndexOf(element: T): Int {
    if (this is List) return this.lastIndexOf(element)
    var lastIndex = -1
    var index = 0
    for (item in this) {
        if (element == item)
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns last index of [element], or -1 if the list does not contain element.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public fun <@kotlin.internal.OnlyInputTypes T> List<T>.lastIndexOf(element: T): Int {
    return lastIndexOf(element)
}

/**
 * Returns the last element, or `null` if the collection is empty.
 */
public fun <T> Iterable<T>.lastOrNull(): T? {
    when (this) {
        is List -> return if (isEmpty()) null else this[size - 1]
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            var last = iterator.next()
            while (iterator.hasNext())
                last = iterator.next()
            return last
        }
    }
}

/**
 * Returns the last element, or `null` if the list is empty.
 */
public fun <T> List<T>.lastOrNull(): T? {
    return if (isEmpty()) null else this[size - 1]
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Iterable<T>.lastOrNull(predicate: (T) -> Boolean): T? {
    var last: T? = null
    for (element in this) {
        if (predicate(element)) {
            last = element
        }
    }
    return last
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> List<T>.lastOrNull(predicate: (T) -> Boolean): T? {
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        val element = iterator.previous()
        if (predicate(element)) return element
    }
    return null
}

/**
 * Returns the single element, or throws an exception if the collection is empty or has more than one element.
 */
public fun <T> Iterable<T>.single(): T {
    when (this) {
        is List -> return this.single()
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                throw NoSuchElementException("Collection is empty.")
            val single = iterator.next()
            if (iterator.hasNext())
                throw IllegalArgumentException("Collection has more than one element.")
            return single
        }
    }
}

/**
 * Returns the single element, or throws an exception if the list is empty or has more than one element.
 */
public fun <T> List<T>.single(): T {
    return when (size) {
        0 -> throw NoSuchElementException("List is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("List has more than one element.")
    }
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun <T> Iterable<T>.single(predicate: (T) -> Boolean): T {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Collection contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Collection contains no element matching the predicate.")
    return single as T
}

/**
 * Returns single element, or `null` if the collection is empty or has more than one element.
 */
public fun <T> Iterable<T>.singleOrNull(): T? {
    when (this) {
        is List -> return if (size == 1) this[0] else null
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext())
                return null
            val single = iterator.next()
            if (iterator.hasNext())
                return null
            return single
        }
    }
}

/**
 * Returns single element, or `null` if the list is empty or has more than one element.
 */
public fun <T> List<T>.singleOrNull(): T? {
    return if (size == 1) this[0] else null
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun <T> Iterable<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns a list containing all elements except first [n] elements.
 */
public fun <T> Iterable<T>.drop(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return toList()
    val list: ArrayList<T>
    if (this is Collection<*>) {
        val resultSize = size - n
        if (resultSize <= 0)
            return emptyList()
        if (resultSize == 1)
            return listOf(last())
        list = ArrayList<T>(resultSize)
        if (this is List<T>) {
            if (this is RandomAccess) {
                for (index in n..size - 1)
                    list.add(this[index])
            } else {
                for (item in this.listIterator(n))
                    list.add(item)
            }
            return list
        }
    }
    else {
        list = ArrayList<T>()
    }
    var count = 0
    for (item in this) {
        if (count++ >= n) list.add(item)
    }
    return list.optimizeReadOnlyList()
}

/**
 * Returns a list containing all elements except last [n] elements.
 */
public fun <T> List<T>.dropLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 */
public inline fun <T> List<T>.dropLastWhile(predicate: (T) -> Boolean): List<T> {
    if (!isEmpty()) {
        val iterator = this.listIterator(size)
        while (iterator.hasPrevious()) {
            if (!predicate(iterator.previous())) {
                return take(iterator.nextIndex() + 1)
            }
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 */
public inline fun <T> Iterable<T>.dropWhile(predicate: (T) -> Boolean): List<T> {
    var yielding = false
    val list = ArrayList<T>()
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <T> Iterable<T>.filterIndexed(predicate: (Int, T) -> Boolean): List<T> {
    return filterIndexedTo(ArrayList<T>(), predicate)
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterIndexedTo(destination: C, predicate: (Int, T) -> Boolean): C {
    forEachIndexed { index, element ->
        if (predicate(index, element)) destination.add(element)
    }
    return destination
}

/**
 * Returns a list containing all elements that are instances of specified type parameter R.
 */
/*
@FixmeReified
public inline fun <reified R> Iterable<*>.filterIsInstance(): List<@kotlin.internal.NoInfer R> {
    return filterIsInstanceTo(ArrayList<R>())
}

/**
 * Appends all elements that are instances of specified type parameter R to the given [destination].
 */
public inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceTo(destination: C): C {
    for (element in this) if (element is R) destination.add(element)
    return destination
}
*/

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun <T> Iterable<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all elements that are not `null`.
 */
public fun <T : Any> Iterable<T?>.filterNotNull(): List<T> {
    return filterNotNullTo(ArrayList<T>())
}

/**
 * Appends all elements that are not `null` to the given [destination].
 */
public fun <C : MutableCollection<in T>, T : Any> Iterable<T?>.filterNotNullTo(destination: C): C {
    for (element in this) if (element != null) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun <T> List<T>.slice(indices: IntRange): List<T> {
    if (indices.isEmpty()) return listOf()
    return this.subList(indices.start, indices.endInclusive + 1).toList()
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun <T> List<T>.slice(indices: Iterable<Int>): List<T> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = ArrayList<T>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list
}

/**
 * Returns a list containing first [n] elements.
 */
public fun <T> Iterable<T>.take(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (this is Collection<T>) {
        if (n >= size) return toList()
        if (n == 1) return listOf(first())
    }
    var count = 0
    val list = ArrayList<T>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list.optimizeReadOnlyList()
}

/**
 * Returns a list containing last [n] elements.
 */
public fun <T> List<T>.takeLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(last())
    val list = ArrayList<T>(n)
    if (this is RandomAccess) {
        for (index in size - n .. size - 1)
            list.add(this[index])
    } else {
        for (item in this.listIterator(n))
            list.add(item)
    }
    return list
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 */
public inline fun <T> List<T>.takeLastWhile(predicate: (T) -> Boolean): List<T> {
    if (isEmpty())
        return emptyList()
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        if (!predicate(iterator.previous())) {
            iterator.next()
            val expectedSize = size - iterator.nextIndex()
            if (expectedSize == 0) return emptyList()
            return ArrayList<T>(expectedSize).apply {
                while (iterator.hasNext())
                    add(iterator.next())
            }
        }
    }
    return toList()
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 */
public inline fun <T> Iterable<T>.takeWhile(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}

/**
 * Reverses elements in the list in-place.
 */
public fun <T> MutableList<T>.reverse(): Unit {
    val median = size / 2
    var leftIndex = 0
    var rightIndex = size - 1
    while (leftIndex < median) {
        val tmp = this[leftIndex]
        this[leftIndex] = this[rightIndex]
        this[rightIndex] = tmp
        leftIndex++
        rightIndex--
    }

}

/**
 * Returns a list with elements in reversed order.
 */
public fun <T> Iterable<T>.reversed(): List<T> {
    if (this is Collection && size <= 1) return toList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Sorts elements in the list in-place according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> MutableList<T>.sortBy(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareBy(selector))
}

/**
 * Sorts elements in the list in-place descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> MutableList<T>.sortByDescending(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareByDescending(selector))
}

/**
 * Sorts elements in the list in-place descending according to their natural sort order.
 */
public fun <T : Comparable<T>> MutableList<T>.sortDescending(): Unit {
    sortWith(reverseOrder())
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Iterable<T>.sorted(): List<T> {
    if (this is Collection) {
        if (size <= 1) return this.toList()
       @Suppress("UNCHECKED_CAST")
        return (toTypedArray<Comparable<T>>() as Array<T>).apply { sort() }.asList()
    }
    return toMutableList().apply { sort() }
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.sortedBy(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescending(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Iterable<T>.sortedDescending(): List<T> {
    return sortedWith(reverseOrder())
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun <T> Iterable<T>.sortedWith(comparator: Comparator<in T>): List<T> {
    if (this is Collection) {
        if (size <= 1) return this.toList()
        @Suppress("UNCHECKED_CAST")
        return (toTypedArray<Any?>() as Array<T>).apply { sortWith(comparator) }.asList()
    }
    return toMutableList().apply { sortWith(comparator) }
}

/**
 * Returns an array of Boolean containing all of the elements of this collection.
 */
public fun Collection<Boolean>.toBooleanArray(): BooleanArray {
    val result = BooleanArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Byte containing all of the elements of this collection.
 */
public fun Collection<Byte>.toByteArray(): ByteArray {
    val result = ByteArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Char containing all of the elements of this collection.
 */
public fun Collection<Char>.toCharArray(): CharArray {
    val result = CharArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Double containing all of the elements of this collection.
 */
public fun Collection<Double>.toDoubleArray(): DoubleArray {
    val result = DoubleArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Float containing all of the elements of this collection.
 */
public fun Collection<Float>.toFloatArray(): FloatArray {
    val result = FloatArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Int containing all of the elements of this collection.
 */
public fun Collection<Int>.toIntArray(): IntArray {
    val result = IntArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Long containing all of the elements of this collection.
 */
public fun Collection<Long>.toLongArray(): LongArray {
    val result = LongArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns an array of Short containing all of the elements of this collection.
 */
public fun Collection<Short>.toShortArray(): ShortArray {
    val result = ShortArray(size)
    var index = 0
    for (element in this)
        result[index++] = element
    return result
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K, V> Iterable<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> {
    val capacity = @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing the elements from the given collection indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K> Iterable<T>.associateBy(keySelector: (T) -> K): Map<K, T> {
    val capacity = @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
            mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given collection.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K, V> Iterable<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, V> {
    val capacity = @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given collection
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, M : MutableMap<in K, in T>> Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given collection.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Iterable<T>.associateTo(destination: M, transform: (T) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}

/**
 * Appends all elements to the given [destination] collection.
 */
public fun <T, C : MutableCollection<in T>> Iterable<T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun <T> Iterable<T>.toHashSet(): HashSet<T> {
    return toCollection(HashSet<T>(mapCapacity(collectionSizeOrDefault(12))))
}

/**
 * Returns a [List] containing all elements.
 */
public fun <T> Iterable<T>.toList(): List<T> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyList()
            1 -> listOf(if (this is List) get(0) else iterator().next())
            else -> this.toMutableList()
        }
    }
    return this.toMutableList().optimizeReadOnlyList()
}

/**
 * Returns a [MutableList] filled with all elements of this collection.
 */
public fun <T> Iterable<T>.toMutableList(): MutableList<T> {
    if (this is Collection<T>)
        return this.toMutableList()
    return toCollection(ArrayList<T>())
}

/**
 * Returns a [MutableList] filled with all elements of this collection.
 */
public fun <T> Collection<T>.toMutableList(): MutableList<T> {
    return ArrayList(this)
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public fun <T> Iterable<T>.toSet(): Set<T> {
    if (this is Collection) {
        return when (size) {
            0 -> emptySet()
            1 -> setOf(if (this is List) this[0] else iterator().next())
            else -> toCollection(LinkedHashSet<T>(mapCapacity(size)))
        }
    }
    return toCollection(LinkedHashSet<T>()).optimizeReadOnlySet()
}

/**
 * Returns a [SortedSet] of all elements.
 */
//public fun <T: Comparable<T>> Iterable<T>.toSortedSet(): SortedSet<T> {
//    return toCollection(TreeSet<T>())
//}

/**
 * Returns a [SortedSet] of all elements.
 *
 * Elements in the set returned are sorted according to the given [comparator].
 */
//public fun <T> Iterable<T>.toSortedSet(comparator: Comparator<in T>): SortedSet<T> {
//    return toCollection(TreeSet<T>(comparator))
//}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original collection.
 */
public inline fun <T, R> Iterable<T>.flatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original collection, to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapTo(destination: C, transform: (T) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Groups elements of the original collection by the key returned by the given [keySelector] function
 * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original collection.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <T, K> Iterable<T>.groupBy(keySelector: (T) -> K): Map<K, List<T>> {
    return groupByTo(LinkedHashMap<K, MutableList<T>>(), keySelector)
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original collection
 * by the key returned by the given [keySelector] function applied to the element
 * and returns a map where each group key is associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original collection.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <T, K, V> Iterable<T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, List<V>> {
    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
}

/**
 * Groups elements of the original collection by the key returned by the given [keySelector] function
 * applied to each element and puts to the [destination] map each group key associated with a list of corresponding elements.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupBy
 */
public inline fun <T, K, M : MutableMap<in K, MutableList<T>>> Iterable<T>.groupByTo(destination: M, keySelector: (T) -> K): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<T>() }
        list.add(element)
    }
    return destination
}

/**
 * Groups values returned by the [valueTransform] function applied to each element of the original collection
 * by the key returned by the given [keySelector] function applied to the element
 * and puts to the [destination] map each group key associated with a list of corresponding values.
 *
 * @return The [destination] map.
 *
 * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
 */
public inline fun <T, K, V, M : MutableMap<in K, MutableList<V>>> Iterable<T>.groupByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<V>() }
        list.add(valueTransform(element))
    }
    return destination
}

/**
 * Creates a [Grouping] source from a collection to be used later with one of group-and-fold operations
 * using the specified [keySelector] function to extract a key from each element.
 *
 * @sample samples.collections.Collections.Transformations.groupingByEachCount
 */
@SinceKotlin("1.1")
public inline fun <T, K> Iterable<T>.groupingBy(crossinline keySelector: (T) -> K): Grouping<T, K> {
    return object : Grouping<T, K> {
        override fun sourceIterator(): Iterator<T> = this@groupingBy.iterator()
        override fun keyOf(element: T): K = keySelector(element)
    }
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return mapTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original collection.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R> Iterable<T>.mapIndexed(transform: (Int, T) -> R): List<R> {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return mapIndexedTo(ArrayList<R>(collectionSizeOrDefault(10)), transform)
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element and its index in the original collection.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R : Any> Iterable<T>.mapIndexedNotNull(transform: (Int, T) -> R?): List<R> {
    return mapIndexedNotNullTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each element and its index in the original collection
 * and appends only the non-null results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R : Any, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullTo(destination: C, transform: (Int, T) -> R?): C {
    forEachIndexed { index, element -> transform(index, element)?.let { destination.add(it) } }
    return destination
}

/**
 * Applies the given [transform] function to each element and its index in the original collection
 * and appends the results to the given [destination].
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapIndexedTo(destination: C, transform: (Int, T) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <T, R : Any> Iterable<T>.mapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each element in the original collection
 * and appends only the non-null results to the given [destination].
 */
public inline fun <T, R : Any, C : MutableCollection<in R>> Iterable<T>.mapNotNullTo(destination: C, transform: (T) -> R?): C {
    forEach { element -> transform(element)?.let { destination.add(it) } }
    return destination
}

/**
 * Applies the given [transform] function to each element of the original collection
 * and appends the results to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Returns a lazy [Iterable] of [IndexedValue] for each element of the original collection.
 */
public fun <T> Iterable<T>.withIndex(): Iterable<IndexedValue<T>> {
    return IndexingIterable { iterator() }
}

/**
 * Returns a list containing only distinct elements from the given collection.
 *
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun <T> Iterable<T>.distinct(): List<T> {
    return this.toMutableSet().toList()
}

/**
 * Returns a list containing only elements from the given collection
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <T, K> Iterable<T>.distinctBy(selector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val list = ArrayList<T>()
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public infix fun <T> Iterable<T>.intersect(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set
}

/**
 * Returns a set containing all elements that are contained by this collection and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public infix fun <T> Iterable<T>.subtract(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set
}

/**
 * Returns a mutable set containing all distinct elements from the given collection.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public fun <T> Iterable<T>.toMutableSet(): MutableSet<T> {
    return when (this) {
        is Collection<T> -> LinkedHashSet(this)
        else -> toCollection(LinkedHashSet<T>())
    }
}

/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original collection.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun <T> Iterable<T>.union(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun <T> Iterable<T>.all(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if collection has at least one element.
 */
public fun <T> Iterable<T>.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun <T> Iterable<T>.any(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements in this collection.
 * Returns the number of elements in this collection.
 */
public fun <T> Iterable<T>.count(): Int {
    var count = 0
    for (element in this) count++
    return count
}

/**
 * Returns the number of elements in this collection.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.count(): Int {
    return size
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun <T> Iterable<T>.count(predicate: (T) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <T, R> Iterable<T>.fold(initial: R, operation: (R, T) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original collection.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself, and calculates the next accumulator value.
 */
public inline fun <T, R> Iterable<T>.foldIndexed(initial: R, operation: (Int, R, T) -> R): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <T, R> List<T>.foldRight(initial: R, operation: (T, R) -> R): R {
    var accumulator = initial
    if (!isEmpty()) {
        val iterator = this.listIterator(size)
        while (iterator.hasPrevious()) {
            accumulator = operation(iterator.previous(), accumulator)
        }
    }
    return accumulator
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from right to left
 * to each element with its index in the original list and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <T, R> List<T>.foldRightIndexed(initial: R, operation: (Int, T, R) -> R): R {
    var accumulator = initial
    if (!isEmpty()) {
        val iterator = this.listIterator(size)
        while (iterator.hasPrevious()) {
            val index = iterator.previousIndex()
            accumulator = operation(index, iterator.previous(), accumulator)
        }
    }
    return accumulator
}

/**
 * Performs the given [action] on each element.
 */
@kotlin.internal.HidesMembers
public inline fun <T> Iterable<T>.forEach(action: (T) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Performs the given [action] on each element, providing sequential index with the element.
 * @param [action] function that takes the index of an element and the element itself
 * and performs the desired action on the element.
 */
public inline fun <T> Iterable<T>.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Iterable<Double>.max(): Double? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    if (max.isNaN()) return max
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Iterable<Float>.max(): Float? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    if (max.isNaN()) return max
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (e.isNaN()) return e
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Iterable<T>.max(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.maxBy(selector: (T) -> R): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var maxElem = iterator.next()
    var maxValue = selector(maxElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun <T> Iterable<T>.maxWith(comparator: Comparator<in T>): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (comparator.compare(max, e) < 0) max = e
    }
    return max
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Iterable<Double>.min(): Double? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    if (min.isNaN()) return min
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * If any of elements is `NaN` returns `NaN`.
 */
@SinceKotlin("1.1")
public fun Iterable<Float>.min(): Float? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    if (min.isNaN()) return min
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (e.isNaN()) return e
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Iterable<T>.min(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.minBy(selector: (T) -> R): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var minElem = iterator.next()
    var minValue = selector(minElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
 */
public fun <T> Iterable<T>.minWith(comparator: Comparator<in T>): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (comparator.compare(min, e) > 0) min = e
    }
    return min
}

/**
 * Returns `true` if the collection has no elements.
 */
public fun <T> Iterable<T>.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun <T> Iterable<T>.none(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Performs the given [action] on each element and returns the collection itself afterwards.
 */
@SinceKotlin("1.1")
public inline fun <T, C : Iterable<T>> C.onEach(action: (T) -> Unit): C {
    return apply { for (element in this) action(element) }
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <S, T: S> Iterable<T>.reduce(operation: (S, T) -> S): S {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }
    return accumulator
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element with its index in the original collection.
 * @param [operation] function that takes the index of an element, current accumulator value
 * and the element itself and calculates the next accumulator value.
 */
public inline fun <S, T: S> Iterable<T>.reduceIndexed(operation: (Int, S, T) -> S): S {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
    var index = 1
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(index++, accumulator, iterator.next())
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left to each element and current accumulator value.
 */
public inline fun <S, T: S> List<T>.reduceRight(operation: (T, S) -> S): S {
    val iterator = listIterator(size)
    if (!iterator.hasPrevious())
        throw UnsupportedOperationException("Empty list can't be reduced.")
    var accumulator: S = iterator.previous()
    while (iterator.hasPrevious()) {
        accumulator = operation(iterator.previous(), accumulator)
    }
    return accumulator
}

/**
 * Accumulates value starting with last element and applying [operation] from right to left
 * to each element with its index in the original list and current accumulator value.
 * @param [operation] function that takes the index of an element, the element itself
 * and current accumulator value, and calculates the next accumulator value.
 */
public inline fun <S, T: S> List<T>.reduceRightIndexed(operation: (Int, T, S) -> S): S {
    val iterator = this.listIterator(size)
    if (!iterator.hasPrevious())
        throw UnsupportedOperationException("Empty list can't be reduced.")
    var accumulator: S = iterator.previous()
    while (iterator.hasPrevious()) {
        val index = iterator.previousIndex()
        accumulator = operation(index, iterator.previous(), accumulator)
    }
    return accumulator
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
public inline fun <T> Iterable<T>.sumBy(selector: (T) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
public inline fun <T> Iterable<T>.sumByDouble(selector: (T) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements.
 */
public fun <T : Any> Iterable<T?>.requireNoNulls(): Iterable<T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this.")
        }
    }
    @Suppress("UNCHECKED_CAST")
    return this as Iterable<T>
}

/**
 * Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements.
 */
public fun <T : Any> List<T?>.requireNoNulls(): List<T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this.")
        }
    }
    @Suppress("UNCHECKED_CAST")
    return this as List<T>
}

/**
 * Returns a list containing all elements of the original collection without the first occurrence of the given [element].
 */
public operator fun <T> Iterable<T>.minus(element: T): List<T> {
    val result = ArrayList<T>(collectionSizeOrDefault(10))
    var removed = false
    return this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] array.
 */
public operator fun <T> Iterable<T>.minus(elements: Array<out T>): List<T> {
    if (elements.isEmpty()) return this.toList()
    val other = elements.toHashSet()
    return this.filterNot { it in other }
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] collection.
 */
public operator fun <T> Iterable<T>.minus(elements: Iterable<T>): List<T> {
    val other = elements.convertToSetForSetOperationWith(this)
    if (other.isEmpty())
        return this.toList()
    return this.filterNot { it in other }
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] sequence.
 */
public operator fun <T> Iterable<T>.minus(elements: Sequence<T>): List<T> {
    val other = elements.toHashSet()
    if (other.isEmpty())
        return this.toList()
    return this.filterNot { it in other }
}

/**
 * Returns a list containing all elements of the original collection without the first occurrence of the given [element].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.minusElement(element: T): List<T> {
    return minus(element)
}

/**
 * Splits the original collection into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun <T> Iterable<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
public operator fun <T> Iterable<T>.plus(element: T): List<T> {
    if (this is Collection) return this.plus(element)
    val result = ArrayList<T>()
    result.addAll(this)
    result.add(element)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
public operator fun <T> Collection<T>.plus(element: T): List<T> {
    val result = ArrayList<T>(size + 1)
    result.addAll(this)
    result.add(element)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] array.
 */
public operator fun <T> Iterable<T>.plus(elements: Array<out T>): List<T> {
    if (this is Collection) return this.plus(elements)
    val result = ArrayList<T>()
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] array.
 */
public operator fun <T> Collection<T>.plus(elements: Array<out T>): List<T> {
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
public operator fun <T> Iterable<T>.plus(elements: Iterable<T>): List<T> {
    if (this is Collection) return this.plus(elements)
    val result = ArrayList<T>()
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
public operator fun <T> Collection<T>.plus(elements: Iterable<T>): List<T> {
    if (elements is Collection) {
        val result = ArrayList<T>(this.size + elements.size)
        result.addAll(this)
        result.addAll(elements)
        return result
    } else {
        val result = ArrayList<T>(this)
        result.addAll(elements)
        return result
    }
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence.
 */
public operator fun <T> Iterable<T>.plus(elements: Sequence<T>): List<T> {
    val result = ArrayList<T>()
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence.
 */
public operator fun <T> Collection<T>.plus(elements: Sequence<T>): List<T> {
    val result = ArrayList<T>(this.size + 10)
    result.addAll(this)
    result.addAll(elements)
    return result
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.plusElement(element: T): List<T> {
    return plus(element)
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.plusElement(element: T): List<T> {
    return plus(element)
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <T, R> Iterable<T>.zip(other: Array<out R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

@Suppress("NOTHING_TO_INLINE")
inline fun min(x1: Int, x2: Int) = if (x1 < x2) x1 else x2

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <T, R, V> Iterable<T>.zip(other: Array<out R>, transform: (T, R) -> V): List<V> {
    val arraySize = other.size
    val list = ArrayList<V>(min(collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in this) {
        if (i >= arraySize) break
        list.add(transform(element, other[i++]))
    }
    return list
}

/**
 * Returns a list of pairs built from elements of both collections with same indexes. List has length of shortest collection.
 */
public infix fun <T, R> Iterable<T>.zip(other: Iterable<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from elements of both collections with same indexes using provided [transform]. List has length of shortest collection.
 */
public inline fun <T, R, V> Iterable<T>.zip(other: Iterable<R>, transform: (T, R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(min(collectionSizeOrDefault(10), other.collectionSizeOrDefault(10)))
    while (first.hasNext() && second.hasNext()) {
        list.add(transform(first.next(), second.next()))
    }
    return list
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T, A : Appendable> Iterable<T>.joinTo(buffer: A, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null)
                buffer.append(transform(element))
            else
                buffer.append(if (element == null) "null" else element.toString())
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T> Iterable<T>.joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Returns this collection as an [Iterable].
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.asIterable(): Iterable<T> {
    return this
}

/**
 * Creates a [Sequence] instance that wraps the original collection returning its elements when being iterated.
 */
public fun <T> Iterable<T>.asSequence(): Sequence<T> {
    return Sequence { this.iterator() }
}

/**
 * Returns a list containing all elements that are instances of specified class.
 */
//public fun <R> Iterable<*>.filterIsInstance(klass: Class<R>): List<R> {
//    return filterIsInstanceTo(ArrayList<R>(), klass)
//}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Byte>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Short>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Int>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Long>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Float>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
public fun Iterable<Double>.average(): Double {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        sum += element
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Byte>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Short>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Int>.sum(): Int {
    var sum: Int = 0
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Long>.sum(): Long {
    var sum: Long = 0L
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Float>.sum(): Float {
    var sum: Float = 0.0f
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
public fun Iterable<Double>.sum(): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += element
    }
    return sum
}
