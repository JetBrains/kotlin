/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind
import kotlin.concurrent.atomics.*

/**
 * An [IntArray] in which elements are always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray instead.", replaceWith = ReplaceWith("kotlin.concurrent.atomics.AtomicIntArray", "kotlin.concurrent.atomics.AtomicIntArray"))
public class AtomicIntArray {
    private val array: IntArray

    /**
     * Creates a new [AtomicIntArray] of the given [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public constructor(size: Int) {
        array = IntArray(size)
    }

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     */
    @PublishedApi
    internal constructor(array: IntArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.size instead.", replaceWith = ReplaceWith("this.size"))
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.loadAt instead.", replaceWith = ReplaceWith("this.loadAt(index)"))
    public operator fun get(index: Int): Int {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.storeAt instead.", replaceWith = ReplaceWith("this.storeAt(index, newValue)"))
    public operator fun set(index: Int, newValue: Int): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.exchangeAt instead.", replaceWith = ReplaceWith("this.exchangeAt(index, newValue)"))
    public fun getAndSet(index: Int, newValue: Int): Int {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.compareAndSetAt instead.", replaceWith = ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"))
    public fun compareAndSet(index: Int, expectedValue: Int, newValue: Int): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.compareAndExchangeAt instead.", replaceWith = ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"))
    public fun compareAndExchange(index: Int, expectedValue: Int, newValue: Int): Int {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.fetchAndAddAt instead.", replaceWith = ReplaceWith("this.fetchAndAddAt(index, delta)"))
    public fun getAndAdd(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.addAndFetchAt instead.", replaceWith = ReplaceWith("this.addAndFetchAt(index, delta)"))
    public fun addAndGet(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.fetchAndIncrementAt instead.", replaceWith = ReplaceWith("this.fetchAndIncrementAt(index)"))
    public fun getAndIncrement(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, 1)
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.incrementAndFetchAt instead.", replaceWith = ReplaceWith("this.incrementAndFetchAt(index)"))
    public fun incrementAndGet(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, 1) + 1
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.fetchAndDecrementAt instead.", replaceWith = ReplaceWith("this.fetchAndDecrementAt(index)"))
    public fun getAndDecrement(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, -1)
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray.decrementAndFetchAt instead.", replaceWith = ReplaceWith("this.decrementAndFetchAt(index)"))
    public fun decrementAndGet(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, -1) - 1
    }

    /**
     * Returns the string representation of the underlying [IntArray][array].
     */
    public override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicIntArray with size ${array.size}.")
    }
}

/**
 * Creates a new [AtomicIntArray] of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Suppress("DEPRECATION")
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicIntArray instead.", replaceWith = ReplaceWith("AtomicIntArray(size, init)", "kotlin.concurrent.atomics.AtomicIntArray"))
public inline fun AtomicIntArray(size: Int, init: (Int) -> Int): AtomicIntArray {
    val inner = IntArray(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicIntArray(inner)
}

/**
 * An [LongArray] in which elements are always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray instead.", replaceWith = ReplaceWith("kotlin.concurrent.atomics.AtomicLongArray", "kotlin.concurrent.atomics.AtomicLongArray"))
public class AtomicLongArray {
    private val array: LongArray

    /**
     * Creates a new [AtomicLongArray] of the given [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public constructor(size: Int) {
        array = LongArray(size)
    }

    /**
     * Creates a new [AtomicLongArray] filled with elements of the given [array].
     */
    @PublishedApi
    internal constructor(array: LongArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.size instead.", replaceWith = ReplaceWith("this.size"))
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.loadAt instead.", replaceWith = ReplaceWith("this.loadAt(index)"))
    public operator fun get(index: Int): Long {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.storeAt instead.", replaceWith = ReplaceWith("this.storeAt(index, newValue)"))
    public operator fun set(index: Int, newValue: Long): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.exchangeAt instead.", replaceWith = ReplaceWith("this.exchangeAt(index, newValue)"))
    public fun getAndSet(index: Int, newValue: Long): Long {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.compareAndSetAt instead.", replaceWith = ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"))
    public fun compareAndSet(index: Int, expectedValue: Long, newValue: Long): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.compareAndExchangeAt instead.", replaceWith = ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"))
    public fun compareAndExchange(index: Int, expectedValue: Long, newValue: Long): Long {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.fetchAndAddAt instead.", replaceWith = ReplaceWith("this.fetchAndAddAt(index, delta)"))
    public fun getAndAdd(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.addAndFetchAt instead.", replaceWith = ReplaceWith("this.addAndFetchAt(index, delta)"))
    public fun addAndGet(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.fetchAndIncrementAt instead.", replaceWith = ReplaceWith("this.fetchAndIncrementAt(index)"))
    public fun getAndIncrement(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, 1L)
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.incrementAndFetchAt instead.", replaceWith = ReplaceWith("this.incrementAndFetchAt(index)"))
    public fun incrementAndGet(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, 1L) + 1L
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.fetchAndDecrementAt instead.", replaceWith = ReplaceWith("this.fetchAndDecrementAt(index)"))
    public fun getAndDecrement(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, -1L)
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray.decrementAndFetchAt instead.", replaceWith = ReplaceWith("this.decrementAndFetchAt(index)"))
    public fun decrementAndGet(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, -1L) - 1L
    }

    /**
     * Returns the string representation of the underlying [IntArray][array].
     */
    public override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicLongArray with size ${array.size}.")
    }
}

/**
 * Creates a new [AtomicLongArray] of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Suppress("DEPRECATION")
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicLongArray instead.", replaceWith = ReplaceWith("AtomicLongArray(size, init)", "kotlin.concurrent.atomics.AtomicIntArray"))
public inline fun AtomicLongArray(size: Int, init: (Int) -> Long): AtomicLongArray {
    val inner = LongArray(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicLongArray(inner)
}

/**
 * An [Array]<T> in which elements are always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray instead.", replaceWith = ReplaceWith("kotlin.concurrent.atomics.AtomicArray", "kotlin.concurrent.atomics.AtomicArray"))
public class AtomicArray<T> {
    private val array: Array<T>

    /**
     * Creates a new [AtomicArray]<T> filled with elements of the given [array].
     */
    @PublishedApi
    internal constructor(array: Array<T>) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.size instead.", replaceWith = ReplaceWith("this.size", "kotlin.concurrent.atomics.AtomicArray"))
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.loadAt instead.", replaceWith = ReplaceWith("this.loadAt(index)"))
    public operator fun get(index: Int): T {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.storeAt instead.", replaceWith = ReplaceWith("this.storeAt(index, newValue)"))
    public operator fun set(index: Int, newValue: T): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.exchangeAt instead.", replaceWith = ReplaceWith("this.exchangeAt(index, newValue)"))
    public fun getAndSet(index: Int, newValue: T): T {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.compareAndSetAt instead.", replaceWith = ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"))
    public fun compareAndSet(index: Int, expectedValue: T, newValue: T): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    @Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray.compareAndExchangeAt instead.", replaceWith = ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"))
    public fun compareAndExchange(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Returns the string representation of the underlying [IntArray][array].
     */
    public override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicArray with size ${array.size}.")
    }
}

/**
 * Creates a new [AtomicArray]<T> of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
@Suppress("UNCHECKED_CAST", "DEPRECATION")
@Deprecated(message = "Use kotlin.concurrent.atomics.AtomicArray instead.", replaceWith = ReplaceWith("AtomicArray(size, init)", "kotlin.concurrent.atomics.AtomicArray"))
public inline fun <reified T> AtomicArray(size: Int, init: (Int) -> T): AtomicArray<T> {
    val inner = arrayOfNulls<T>(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicArray(inner as Array<T>)
}