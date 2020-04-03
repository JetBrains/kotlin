/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")
@file:OptIn(kotlin.experimental.ExperimentalTypeInference::class)

package kotlin.collections

import kotlin.contracts.*

internal object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

internal object EmptyList : List<Nothing>, Serializable, RandomAccess {
    private const val serialVersionUID: Long = -7390468764508069838L

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

private class ArrayAsCollection<T>(val values: Array<out T>, val isVarargs: Boolean) : Collection<T> {
    override val size: Int get() = values.size
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun contains(element: T): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): Iterator<T> = values.iterator()
    // override hidden toArray implementation to prevent copying of values array
    public fun toArray(): Array<out Any?> = values.copyToArrayOfAny(isVarargs)
}

/**
 * Returns an empty read-only list.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.emptyReadOnlyList
 */
public fun <T> emptyList(): List<T> = EmptyList

/**
 * Returns a new read-only list of given elements.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.readOnlyList
 */
public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()

/**
 * Returns an empty read-only list.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.emptyReadOnlyList
 */
@kotlin.internal.InlineOnly
public inline fun <T> listOf(): List<T> = emptyList()

/**
 * Returns an empty new [MutableList].
 * @sample samples.collections.Collections.Lists.emptyMutableList
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> mutableListOf(): MutableList<T> = ArrayList()

/**
 * Returns an empty new [ArrayList].
 * @sample samples.collections.Collections.Lists.emptyArrayList
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> arrayListOf(): ArrayList<T> = ArrayList()

/**
 * Returns a new [MutableList] with the given elements.
 * @sample samples.collections.Collections.Lists.mutableList
 */
public fun <T> mutableListOf(vararg elements: T): MutableList<T> =
    if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

/**
 * Returns a new [ArrayList] with the given elements.
 * @sample samples.collections.Collections.Lists.arrayList
 */
public fun <T> arrayListOf(vararg elements: T): ArrayList<T> =
    if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

/**
 * Returns a new read-only list either of single given element, if it is not null, or empty list if the element is null. The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.listOfNotNull
 */
public fun <T : Any> listOfNotNull(element: T?): List<T> = if (element != null) listOf(element) else emptyList()

/**
 * Returns a new read-only list only of those given elements, that are not null.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.listOfNotNull
 */
public fun <T : Any> listOfNotNull(vararg elements: T?): List<T> = elements.filterNotNull()

/**
 * Creates a new read-only list with the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each list element sequentially starting from the first one.
 * It should return the value for a list element given its index.
 *
 * @sample samples.collections.Collections.Lists.readOnlyListFromInitializer
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> List(size: Int, init: (index: Int) -> T): List<T> = MutableList(size, init)

/**
 * Creates a new mutable list with the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each list element sequentially starting from the first one.
 * It should return the value for a list element given its index.
 *
 * @sample samples.collections.Collections.Lists.mutableListFromInitializer
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> MutableList(size: Int, init: (index: Int) -> T): MutableList<T> {
    val list = ArrayList<T>(size)
    repeat(size) { index -> list.add(init(index)) }
    return list
}

/**
 * Builds a new read-only [List] by populating a [MutableList] using the given [builderAction]
 * and returning a read-only list with the same elements.
 *
 * The list passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside of the function produces an unspecified behavior.
 *
 * @sample samples.collections.Builders.Lists.buildListSample
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildList(@BuilderInference builderAction: MutableList<E>.() -> Unit): List<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return ArrayList<E>().apply(builderAction)
}

/**
 * Builds a new read-only [List] by populating a [MutableList] using the given [builderAction]
 * and returning a read-only list with the same elements.
 *
 * The list passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside of the function produces an unspecified behavior.
 *
 * [capacity] is used to hint the expected number of elements added in the [builderAction].
 *
 * @throws IllegalArgumentException if the given [capacity] is negative.
 *
 * @sample samples.collections.Builders.Lists.buildListSample
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
public inline fun <E> buildList(capacity: Int, @BuilderInference builderAction: MutableList<E>.() -> Unit): List<E> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    checkBuilderCapacity(capacity)
    return ArrayList<E>(capacity).apply(builderAction)
}


/**
 * Returns an [IntRange] of the valid indices for this collection.
 * @sample samples.collections.Collections.Collections.indicesOfCollection
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

/**
 * Returns `true` if the collection is not empty.
 * @sample samples.collections.Collections.Collections.collectionIsNotEmpty
 */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.isNotEmpty(): Boolean = !isEmpty()

/**
 * Returns `true` if this nullable collection is either null or empty.
 * @sample samples.collections.Collections.Collections.collectionIsNullOrEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.isEmpty()
}

/**
 * Returns this Collection if it's not `null` and the empty list otherwise.
 * @sample samples.collections.Collections.Collections.collectionOrEmpty
 */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>?.orEmpty(): Collection<T> = this ?: emptyList()

/**
 * Returns this List if it's not `null` and the empty list otherwise.
 * @sample samples.collections.Collections.Lists.listOrEmpty
 */
@kotlin.internal.InlineOnly
public inline fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/**
 * Returns this collection if it's not empty
 * or the result of calling [defaultValue] function if the collection is empty.
 *
 * @sample samples.collections.Collections.Collections.collectionIfEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <C, R> C.ifEmpty(defaultValue: () -> R): R where C : Collection<*>, C : R =
    if (isEmpty()) defaultValue() else this


/**
 * Checks if all elements in the specified collection are contained in this collection.
 *
 * Allows to overcome type-safety restriction of `containsAll` that requires to pass a collection of type `Collection<E>`.
 * @sample samples.collections.Collections.Collections.collectionContainsAll
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // false warning, extension takes precedence in some cases
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes T> Collection<T>.containsAll(elements: Collection<T>): Boolean = this.containsAll(elements)

internal fun <T> List<T>.optimizeReadOnlyList() = when (size) {
    0 -> emptyList()
    1 -> listOf(this[0])
    else -> this
}

/**
 * Searches this list or its range for the provided [element] using the binary search algorithm.
 * The list is expected to be sorted into ascending order according to the Comparable natural ordering of its elements,
 * otherwise the result is undefined.
 *
 * If the list contains multiple elements equal to the specified [element], there is no guarantee which one will be found.
 *
 * `null` value is considered to be less than any non-null value.
 *
 * @return the index of the element, if it is contained in the list within the specified range;
 * otherwise, the inverted insertion point `(-insertion point - 1)`.
 * The insertion point is defined as the index at which the element should be inserted,
 * so that the list (or the specified subrange of list) still remains sorted.
 * @sample samples.collections.Collections.Lists.binarySearchOnComparable
 * @sample samples.collections.Collections.Lists.binarySearchWithBoundaries
 */
public fun <T : Comparable<T>> List<T?>.binarySearch(element: T?, fromIndex: Int = 0, toIndex: Int = size): Int {
    rangeCheck(size, fromIndex, toIndex)

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = compareValues(midVal, element)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

/**
 * Searches this list or its range for the provided [element] using the binary search algorithm.
 * The list is expected to be sorted into ascending order according to the specified [comparator],
 * otherwise the result is undefined.
 *
 * If the list contains multiple elements equal to the specified [element], there is no guarantee which one will be found.
 *
 * `null` value is considered to be less than any non-null value.
 *
 * @return the index of the element, if it is contained in the list within the specified range;
 * otherwise, the inverted insertion point `(-insertion point - 1)`.
 * The insertion point is defined as the index at which the element should be inserted,
 * so that the list (or the specified subrange of list) still remains sorted according to the specified [comparator].
 * @sample samples.collections.Collections.Lists.binarySearchWithComparator
 */
public fun <T> List<T>.binarySearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size): Int {
    rangeCheck(size, fromIndex, toIndex)

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparator.compare(midVal, element)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

/**
 * Searches this list or its range for an element having the key returned by the specified [selector] function
 * equal to the provided [key] value using the binary search algorithm.
 * The list is expected to be sorted into ascending order according to the Comparable natural ordering of keys of its elements.
 * otherwise the result is undefined.
 *
 * If the list contains multiple elements with the specified [key], there is no guarantee which one will be found.
 *
 * `null` value is considered to be less than any non-null value.
 *
 * @return the index of the element with the specified [key], if it is contained in the list within the specified range;
 * otherwise, the inverted insertion point `(-insertion point - 1)`.
 * The insertion point is defined as the index at which the element should be inserted,
 * so that the list (or the specified subrange of list) still remains sorted.
 * @sample samples.collections.Collections.Lists.binarySearchByKey
 */
public inline fun <T, K : Comparable<K>> List<T>.binarySearchBy(
    key: K?,
    fromIndex: Int = 0,
    toIndex: Int = size,
    crossinline selector: (T) -> K?
): Int =
    binarySearch(fromIndex, toIndex) { compareValues(selector(it), key) }

// do not introduce this overload --- too rare
//public fun <T, K> List<T>.binarySearchBy(key: K, comparator: Comparator<K>, fromIndex: Int = 0, toIndex: Int = size(), selector: (T) -> K): Int =
//        binarySearch(fromIndex, toIndex) { comparator.compare(selector(it), key) }


/**
 * Searches this list or its range for an element for which the given [comparison] function returns zero using the binary search algorithm.
 *
 * The list is expected to be sorted so that the signs of the [comparison] function's return values ascend on the list elements,
 * i.e. negative values come before zero and zeroes come before positive values.
 * Otherwise, the result is undefined.
 *
 * If the list contains multiple elements for which [comparison] returns zero, there is no guarantee which one will be found.
 *
 * @param comparison function that returns zero when called on the list element being searched.
 * On the elements coming before the target element, the function must return negative values;
 * on the elements coming after the target element, the function must return positive values.
 *
 * @return the index of the found element, if it is contained in the list within the specified range;
 * otherwise, the inverted insertion point `(-insertion point - 1)`.
 * The insertion point is defined as the index at which the element should be inserted,
 * so that the list (or the specified subrange of list) still remains sorted.
 * @sample samples.collections.Collections.Lists.binarySearchWithComparisonFunction
 */
public fun <T> List<T>.binarySearch(fromIndex: Int = 0, toIndex: Int = size, comparison: (T) -> Int): Int {
    rangeCheck(size, fromIndex, toIndex)

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

/**
 * Checks that `from` and `to` are in
 * the range of [0..size] and throws an appropriate exception, if they aren't.
 */
private fun rangeCheck(size: Int, fromIndex: Int, toIndex: Int) {
    when {
        fromIndex > toIndex -> throw IllegalArgumentException("fromIndex ($fromIndex) is greater than toIndex ($toIndex).")
        fromIndex < 0 -> throw IndexOutOfBoundsException("fromIndex ($fromIndex) is less than zero.")
        toIndex > size -> throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    }
}


@PublishedApi
@SinceKotlin("1.3")
internal expect fun checkIndexOverflow(index: Int): Int

@PublishedApi
@SinceKotlin("1.3")
internal expect fun checkCountOverflow(count: Int): Int


@PublishedApi
@SinceKotlin("1.3")
internal fun throwIndexOverflow() { throw ArithmeticException("Index overflow has happened.") }

@PublishedApi
@SinceKotlin("1.3")
internal fun throwCountOverflow() { throw ArithmeticException("Count overflow has happened.") }

