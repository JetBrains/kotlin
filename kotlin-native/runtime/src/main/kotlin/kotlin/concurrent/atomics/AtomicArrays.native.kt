/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

import kotlin.concurrent.AtomicArray
import kotlin.concurrent.Volatile
import kotlin.native.internal.*

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
@ExperimentalStdlibApi
public actual class AtomicIntArray {
    private val array: IntArray

    /**
     * Creates a new [AtomicIntArray] of the given [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public actual constructor(size: Int) {
        array = IntArray(size)
    }

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     */
    public actual constructor(array: IntArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): Int {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
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
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Returns the string representation of the underlying array of ints.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = array.toString()

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
@ExperimentalStdlibApi
public actual class AtomicLongArray {
    private val array: LongArray

    /**
     * Creates a new [AtomicLongArray] of the specified [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public actual constructor(size: Int) {
        array = LongArray(size)
    }

    /**
     * Creates a new [AtomicLongArray] filled with elements of the given [array].
     */
    public actual constructor(array: LongArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): Long {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
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
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndAddAt(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta)
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun addAndFetchAt(index: Int, delta: Long): Long {
        checkBounds(index)
        return array.getAndAdd(index, delta) + delta
    }

    /**
     * Returns the string representation of the underlying array of ints.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = array.toString()

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
@ExperimentalStdlibApi
public actual class AtomicArray<T> {
    private val array: Array<T>

    /**
     * Creates a new [AtomicArray]<T> filled with elements of the given [array].
     */
    public actual constructor(array: Array<T>) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    public actual val size: Int get() = array.size

    /**
     * Atomically loads the value from the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): T {
        checkBounds(index)
        return array.atomicGet(index)
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
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
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        return array.compareAndExchange(index, expectedValue, newValue)
    }

    /**
     * Returns the string representation of the underlying array of objects.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = array.toString()

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
 * Atomically gets the value of the [IntArray][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun IntArray.atomicGet(index: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun IntArray.atomicSet(index: Int, newValue: Int)

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun IntArray.getAndSet(index: Int, newValue: Int): Int

/**
 * Atomically adds the given [delta] to the [IntArray][this] element at the given [index]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_ARRAY_ELEMENT)
internal external fun IntArray.getAndAdd(index: Int, delta: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun IntArray.compareAndExchange(index: Int, expectedValue: Int, newValue: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun IntArray.compareAndSet(index: Int, expectedValue: Int, newValue: Int): Boolean

/**
 * Atomically gets the value of the [LongArray][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun LongArray.atomicGet(index: Int): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun LongArray.atomicSet(index: Int, newValue: Long)

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun LongArray.getAndSet(index: Int, newValue: Long): Long

/**
 * Atomically adds the given [delta] to the [LongArray][this] element at the given [index]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_ARRAY_ELEMENT)
internal external fun LongArray.getAndAdd(index: Int, delta: Long): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun LongArray.compareAndExchange(index: Int, expectedValue: Long, newValue: Long): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun LongArray.compareAndSet(index: Int, expectedValue: Long, newValue: Long): Boolean

/**
 * Atomically gets the value of the [Array<T>][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.atomicGet(index: Int): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.atomicSet(index: Int, newValue: T)

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.getAndSet(index: Int, newValue: T): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Comparison of values is done by reference.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun <T> Array<T>.compareAndExchange(index: Int, expectedValue: T, newValue: T): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Comparison of values is done by reference.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.compareAndSet(index: Int, expectedValue: T, newValue: T): Boolean
