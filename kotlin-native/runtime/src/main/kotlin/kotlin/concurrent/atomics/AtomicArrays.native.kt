/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

import kotlin.concurrent.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly

/**
 * An array of ints in which elements may be updated atomically.
 *
 * Read operation [loadAt] has the same memory effects as reading a [Volatile] property;
 * Write operation [storeAt] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchangeAt], [compareAndSetAt], [compareAndExchangeAt], [fetchAndAddAt], [addAndFetchAt],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicIntArray {
    private val array: IntArray

    /**
     * Creates a new [AtomicIntArray] of the given [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.sizeCons
     */
    public actual constructor(size: Int) {
        array = IntArray(size)
    }

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.intArrCons
     */
    public actual constructor(array: IntArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.size
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.loadAt
     */
    public actual fun loadAt(index: Int): Int {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: Int): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.exchangeAt
     */
    public actual fun exchangeAt(index: Int, newValue: Int): Int {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.compareAndExchangeAt
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     *  @sample samples.concurrent.atomics.AtomicIntArray.fetchAndAddAt
     */
    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.addAndFetchAt
     */
    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated("Use size instead.", ReplaceWith("this.size"), DeprecationLevel.ERROR)
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use loadAt(index: Int) instead.", ReplaceWith("this.loadAt(index)"), DeprecationLevel.ERROR)
    public operator fun get(index: Int): Int {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use storeAt(index: Int, newValue: Int) instead.", ReplaceWith("this.storeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use exchangeAt(index: Int, newValue: Int) instead.", ReplaceWith("this.exchangeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndSetAt(index: Int, expectedValue: Int, newValue: Int) instead.",
        ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int) instead.",
        ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
    public fun compareAndExchange(index: Int, expectedValue: Int, newValue: Int): Int {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndAddAt(index: Int, delta: Int) instead.", ReplaceWith("this.fetchAndAddAt(index, delta)"), DeprecationLevel.ERROR)
    public fun getAndAdd(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use addAndFetchAt(index: Int, delta: Int) instead.", ReplaceWith("this.addAndFetchAt(index, delta)"), DeprecationLevel.ERROR)
    public fun addAndGet(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndIncrementAt(index: Int) instead.", ReplaceWith("this.fetchAndIncrementAt(index)"), DeprecationLevel.ERROR)
    public fun getAndIncrement(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, 1)
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use incrementAndFetchAt(index: Int) instead.", ReplaceWith("this.incrementAndFetchAt(index)"), DeprecationLevel.ERROR)
    public fun incrementAndGet(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, 1) + 1
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndDecrementAt(index: Int) instead.", ReplaceWith("this.fetchAndDecrementAt(index)"), DeprecationLevel.ERROR)
    public fun getAndDecrement(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, -1)
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use decrementAndFetchAt(index: Int) instead.", ReplaceWith("this.decrementAndFetchAt(index)"), DeprecationLevel.ERROR)
    public fun decrementAndGet(index: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, -1) - 1
    }

    /**
     * Returns the string representation of the underlying array of ints.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = buildString {
        append('[')
        for (i in array.indices) {
            val value = array.atomicGet(i)
            if (i > 0) append(", ")
            append(value)
        }
        append(']')
    }

    // See KT-71459
    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) checkBoundsSlowPath(index)
    }

    private fun checkBoundsSlowPath(index: Int) {
        throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicIntArray with size ${array.size}.")
    }
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.updateAt(index: Int, transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.updateAndFetchAt(index: Int, transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.fetchAndUpdateAt(index: Int, transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}

/**
 * An array of longs in which elements may be updated atomically.
 *
 * Read operation [loadAt] has the same memory effects as reading a [Volatile] property;
 * Write operation [storeAt] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchangeAt], [compareAndSetAt], [compareAndExchangeAt], [fetchAndAddAt], [addAndFetchAt],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicLongArray {
    private val array: LongArray

    /**
     * Creates a new [AtomicLongArray] of the specified [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.sizeCons
     */
    public actual constructor(size: Int) {
        array = LongArray(size)
    }

    /**
     * Creates a new [AtomicLongArray] filled with elements of the given [array].
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.longArrCons
     */
    public actual constructor(array: LongArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.size
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.loadAt
     */
    public actual fun loadAt(index: Int): Long {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: Long): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.exchangeAt
     */
    public actual fun exchangeAt(index: Int, newValue: Long): Long {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.compareAndExchangeAt
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndAddAt
     */
    public actual fun fetchAndAddAt(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.addAndFetchAt
     */
    public actual fun addAndFetchAt(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated("Use size instead.", ReplaceWith("this.size"), DeprecationLevel.ERROR)
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use loadAt(index: Int) instead.", ReplaceWith("this.loadAt(index)"), DeprecationLevel.ERROR)
    public operator fun get(index: Int): Long {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use storeAt(index: Int, newValue: Long) instead.", ReplaceWith("this.storeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use exchangeAt(index: Int, newValue: Long) instead.", ReplaceWith("this.exchangeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndSetAt(index: Int, expectedValue: Long, newValue: Long) instead.",
        ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long) instead.",
        ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
    public fun compareAndExchange(index: Int, expectedValue: Long, newValue: Long): Long {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndAddAt(index: Int, delta: Long) instead.", ReplaceWith("this.fetchAndAddAt(index, delta)"), DeprecationLevel.ERROR)
    public fun getAndAdd(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use addAndFetchAt(index: Int, delta: Long) instead.", ReplaceWith("this.addAndFetchAt(index, delta)"), DeprecationLevel.ERROR)
    public fun addAndGet(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndIncrementAt(index: Int) instead.", ReplaceWith("this.fetchAndIncrementAt(index)"), DeprecationLevel.ERROR)
    public fun getAndIncrement(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, 1L)
    }

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use incrementAndFetchAt(index: Int) instead.", ReplaceWith("this.incrementAndFetchAt(index)"), DeprecationLevel.ERROR)
    public fun incrementAndGet(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, 1L) + 1L
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use fetchAndDecrementAt(index: Int) instead.", ReplaceWith("this.fetchAndDecrementAt(index)"), DeprecationLevel.ERROR)
    public fun getAndDecrement(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, -1L)
    }

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use decrementAndFetchAt(index: Int) instead.", ReplaceWith("this.decrementAndFetchAt(index)"), DeprecationLevel.ERROR)
    public fun decrementAndGet(index: Int): Long {
        checkBounds(index)
        return array.getAndAdd(index, -1L) - 1L
    }

    /**
     * Returns the string representation of the underlying array of ints.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = buildString {
        append('[')
        for (i in array.indices) {
            val value = array.atomicGet(i)
            if (i > 0) append(", ")
            append(value)
        }
        append(']')
    }

    // See KT-71459
    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) checkBoundsSlowPath(index)
    }

    private fun checkBoundsSlowPath(index: Int) {
        throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicIntArray with size ${array.size}.")
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.updateAt(index: Int, transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.updateAndFetchAt(index: Int, transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.fetchAndUpdateAt(index: Int, transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}

/**
 * A generic array of objects in which elements may be updated atomically.
 *
 * Read operation [loadAt] has the same memory effects as reading a [Volatile] property;
 * Write operation [storeAt] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchangeAt], [compareAndSetAt], [compareAndExchangeAt],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicArray<T> {
    private val array: Array<T>

    /**
     * Creates a new [AtomicArray] filled with elements of the given [array].
     *
     * @sample samples.concurrent.atomics.AtomicArray.arrCons
     */
    public actual constructor(array: Array<T>) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.size
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.loadAt
     */
    public actual fun loadAt(index: Int): T {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: T): Unit {
        checkBounds(index)
        array.atomicSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.exchangeAt
     */
    public actual fun exchangeAt(index: Int, newValue: T): T {
        checkBounds(index)
        return array.getAndSet(index, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by reference.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean {
        checkBounds(index)
        return array.compareAndSet(index, expectedValue, newValue)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by reference.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.compareAndExchangeAt
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Returns the number of elements in the array.
     */
    @Deprecated("Use size instead.", ReplaceWith("this.size"), DeprecationLevel.ERROR)
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use loadAt(index: Int) instead.", ReplaceWith("this.loadAt(index)"), DeprecationLevel.ERROR)
    public operator fun get(index: Int): T {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use storeAt(index: Int, newValue: T) instead.", ReplaceWith("this.storeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated("Use exchangeAt(index: Int, newValue: T) instead.", ReplaceWith("this.exchangeAt(index, newValue)"), DeprecationLevel.ERROR)
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndSetAt(index: Int, expectedValue: T, newValue: T) instead.",
        ReplaceWith("this.compareAndSetAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
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
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    @Deprecated(
        "Use compareAndExchangeAt(index: Int, expectedValue: T, newValue: T) instead.",
        ReplaceWith("this.compareAndExchangeAt(index, expectedValue, newValue)"),
        DeprecationLevel.ERROR
    )
    public fun compareAndExchange(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Returns the string representation of the underlying array of objects.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = buildString {
        append('[')
        for (i in array.indices) {
            val value = array.atomicGet(i)
            if (i > 0) append(", ")
            append(value)
        }
        append(']')
    }

    // See KT-71459
    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) checkBoundsSlowPath(index)
    }

    private fun checkBoundsSlowPath(index: Int) {
        throw IndexOutOfBoundsException("The index $index is out of the bounds of the AtomicIntArray with size ${array.size}.")
    }
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.updateAt(index: Int, transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.updateAndFetchAt(index: Int, transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.fetchAndUpdateAt(index: Int, transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}
