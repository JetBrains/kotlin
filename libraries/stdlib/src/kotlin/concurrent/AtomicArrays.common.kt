/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * An array of ints in which elements may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicIntArray] stores an [IntArray] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicIntArray] are represented by [java.util.concurrent.atomic.AtomicIntegerArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and WASM [AtomicIntArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
public expect class AtomicIntArray {
    /**
     * Creates a new [AtomicIntArray] of the specified [size], with all elements initialized to zero.
     * @throws RuntimeException if the specified [size] is negative.
     */
    public constructor(size: Int)

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     */
    public constructor(array: IntArray)

    /**
     * Returns the number of elements in the array.
     */
    public val size: Int

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun loadAt(index: Int): Int

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun storeAt(index: Int, newValue: Int)

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun exchangeAt(index: Int, newValue: Int): Int

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndAddAt(index: Int, delta: Int): Int

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun addAndFetchAt(index: Int, delta: Int): Int

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndIncrementAt(index: Int): Int

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun incrementAndFetchAt(index: Int): Int

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndDecrementAt(index: Int): Int

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun decrementAndFetchAt(index: Int): Int

    /**
     * Returns the string representation of the underlying array of ints.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Creates a new [AtomicIntArray] of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@ExperimentalStdlibApi
public inline fun AtomicIntArray(size: Int, init: (Int) -> Int): AtomicIntArray {
    val inner = IntArray(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicIntArray(inner)
}

/**
 * An array of longs in which elements may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicLongArray] stores a [LongArray] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicLongArray] are represented by [java.util.concurrent.atomic.AtomicLongArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and WASM [AtomicLongArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
public expect class AtomicLongArray {
    /**
     * Creates a new [AtomicLongArray] of the specified [size], with all elements initialized to zero.
     * @throws RuntimeException if the specified [size] is negative.
     */
    public constructor(size: Int)

    /**
     * Creates a new [AtomicLongArray] filled with elements of the given [array].
     */
    public constructor(array: LongArray)

    /**
     * Returns the number of elements in the array.
     */
    public val size: Int

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun loadAt(index: Int): Long

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun storeAt(index: Int, newValue: Long)

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun exchangeAt(index: Int, newValue: Long): Long

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndAddAt(index: Int, delta: Long): Long

    /**
     * Atomically adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun addAndFetchAt(index: Int, delta: Long): Long

    /**
     * Atomically increments the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndIncrementAt(index: Int): Long

    /**
     * Atomically increments the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun incrementAndFetchAt(index: Int): Long

    /**
     * Atomically decrements the element at the given [index] by one and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun fetchAndDecrementAt(index: Int): Long

    /**
     * Atomically decrements the element at the given [index] by one and returns the new value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun decrementAndFetchAt(index: Int): Long

    /**
     * Returns the string representation of the underlying array of longs.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Creates a new [AtomicLongArray] of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@ExperimentalStdlibApi
public inline fun AtomicLongArray(size: Int, init: (Int) -> Long): AtomicLongArray {
    val inner = LongArray(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicLongArray(inner)
}

/**
 * A generic array of objects in which elements may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicArray] stores an [Array] with elements of type [T] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicArray] are represented by [java.util.concurrent.atomic.AtomicReferenceArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and WASM [AtomicArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
public expect class AtomicArray<T> {

    /**
     * Creates a new [AtomicArray]<T> filled with elements of the given [array].
     */
    public constructor(array: Array<T>)

    /**
     * Returns the number of elements in the array.
     */
    public val size: Int

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun loadAt(index: Int): T

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun storeAt(index: Int, newValue: T)

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun exchangeAt(index: Int, newValue: T): T

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by reference.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean

    /**
     * Atomically sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Provides sequential consistent ordering guarantees and never fails spuriously.
     *
     * Comparison of values is done by reference.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T

    /**
     * Returns the string representation of the underlying array of objects.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Creates a new [AtomicArray]<T> of the given [size], where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 */
@ExperimentalStdlibApi
@Suppress("UNCHECKED_CAST")
public inline fun <reified T> AtomicArray(size: Int, init: (Int) -> T): AtomicArray<T> {
    val inner = arrayOfNulls<T>(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicArray(inner as Array<T>)
}