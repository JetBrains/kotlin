/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

import kotlin.comparisons.*
import kotlin.internal.InlineOnly
import kotlin.random.*

/** Copies typed varargs array to an array of objects */
internal actual fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
        if (isVarargs)
            // if the array came from varargs and already is array of Any, copying isn't required.
            @Suppress("UNCHECKED_CAST") (this as Array<out Any?>)
        else
            @Suppress("UNCHECKED_CAST") (this.copyOfUninitializedElements(this.size) as Array<out Any?>)


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

public actual fun <T> Array<out T>.asList(): List<T> {
    return object : AbstractList<T>(), RandomAccess {
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
public actual fun ByteArray.asList(): List<Byte> {
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
public actual fun ShortArray.asList(): List<Short> {
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
public actual fun IntArray.asList(): List<Int> {
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
public actual fun LongArray.asList(): List<Long> {
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
public actual fun FloatArray.asList(): List<Float> {
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
public actual fun DoubleArray.asList(): List<Double> {
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
public actual fun BooleanArray.asList(): List<Boolean> {
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
public actual fun CharArray.asList(): List<Char> {
    return object : AbstractList<Char>(), RandomAccess {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: Char): Boolean = this@asList.contains(element)
        override fun get(index: Int): Char = this@asList[index]
        override fun indexOf(element: Char): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: Char): Int = this@asList.lastIndexOf(element)
    }
}

/**
 * Reverses elements in the list in-place.
 */
public actual fun <T> MutableList<T>.reverse(): Unit {
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
 * Replaces each element in the list with a result of a transformation specified.
 */
public fun <T> MutableList<T>.replaceAll(transformation: (T) -> T) {
    val it = listIterator()
    while (it.hasNext()) {
        val element = it.next()
        it.set(transformation(element))
    }
}

/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of elements in the group.
 *
 * @sample samples.collections.Collections.Transformations.groupingByEachCount
 */
@SinceKotlin("1.1")
public actual fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> = eachCountTo(mutableMapOf<K, Int>())

// Copied from JS.

/**
 * Fills the list with the provided [value].
 *
 * Each element in the list gets replaced with the [value].
 */
@SinceKotlin("1.2")
public actual fun <T> MutableList<T>.fill(value: T): Unit {
    for (index in 0..lastIndex) {
        this[index] = value
    }
}

/**
 * Randomly shuffles elements in this list.
 *
 * See: https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
 */
@SinceKotlin("1.2")
public actual fun <T> MutableList<T>.shuffle(): Unit {
    for (i in lastIndex downTo 1) {
        val j = Random.nextInt(i + 1)
        val copy = this[i]
        this[i] = this[j]
        this[j] = copy
    }
}

/**
 * Returns a new list with the elements of this list randomly shuffled.
 */
@SinceKotlin("1.2")
public actual fun <T> Iterable<T>.shuffled(): List<T> = toMutableList().apply { shuffle() }

@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkIndexOverflow(index: Int): Int {
    if (index < 0) {
        // TODO: api version check?
        throwIndexOverflow()
    }
    return index
}

@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkCountOverflow(count: Int): Int {
    if (count < 0) {
        // TODO: api version check?
        throwCountOverflow()
    }
    return count
}
