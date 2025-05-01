/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AtomicArraysKt")

package kotlin.concurrent.atomics

/**
 * An array of ints in which elements may be updated atomically.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicIntArray] stores an [IntArray] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicIntArray] are represented by [java.util.concurrent.atomic.AtomicIntegerArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicIntArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public expect class AtomicIntArray {
    /**
     * Creates a new [AtomicIntArray] of the specified [size], with all elements initialized to zero.
     * @throws RuntimeException if the specified [size] is negative.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.sizeCons
     */
    public constructor(size: Int)

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     *
     * @see atomicIntArrayOf
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.intArrCons
     */
    public constructor(array: IntArray)

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.size
     */
    public val size: Int

    /**
     * Atomically loads the value from the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.loadAt
     */
    public fun loadAt(index: Int): Int

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.storeAt
     */
    public fun storeAt(index: Int, newValue: Int)

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.exchangeAt
     */
    public fun exchangeAt(index: Int, newValue: Int): Int

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
    public fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean

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
    public fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndAddAt
     */
    public fun fetchAndAddAt(index: Int, delta: Int): Int

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.addAndFetchAt
     */
    public fun addAndFetchAt(index: Int, delta: Int): Int

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
 *
 * @see atomicIntArrayOf
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.initCons
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public inline fun AtomicIntArray(size: Int, init: (Int) -> Int): AtomicIntArray =
    AtomicIntArray(IntArray(size) { init(it) })

/**
 * Atomically increments the element of this [AtomicIntArray] at the given [index] by one and returns the old value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndIncrementAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicIntArray.fetchAndIncrementAt(index: Int): Int = this.fetchAndAddAt(index, 1)

/**
 * Atomically increments the element of this [AtomicIntArray] at the given [index] by one and returns the new value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.incrementAndFetchAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicIntArray.incrementAndFetchAt(index: Int): Int = this.addAndFetchAt(index, 1)

/**
 * Atomically decrements the element of this [AtomicIntArray] at the given [index] by one and returns the new value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.decrementAndFetchAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicIntArray.decrementAndFetchAt(index: Int): Int = this.addAndFetchAt(index, -1)

/**
 * Atomically decrements the element of this [AtomicIntArray] at the given [index] by one and returns the old value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndDecrementAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicIntArray.fetchAndDecrementAt(index: Int): Int = this.fetchAndAddAt(index, -1)

/**
 * An array of longs in which elements may be updated atomically.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicLongArray] stores a [LongArray] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicLongArray] are represented by [java.util.concurrent.atomic.AtomicLongArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicLongArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public expect class AtomicLongArray {
    /**
     * Creates a new [AtomicLongArray] of the specified [size], with all elements initialized to zero.
     * @throws RuntimeException if the specified [size] is negative.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.sizeCons
     */
    public constructor(size: Int)

    /**
     * Creates a new [AtomicLongArray] filled with elements of the given [array].
     *
     * @see atomicLongArrayOf
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.longArrCons
     */
    public constructor(array: LongArray)

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.size
     */
    public val size: Int

    /**
     * Atomically loads the value from the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.loadAt
     */
    public fun loadAt(index: Int): Long

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.storeAt
     */
    public fun storeAt(index: Int, newValue: Long)

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.exchangeAt
     */
    public fun exchangeAt(index: Int, newValue: Long): Long

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
    public fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean

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
    public fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndAddAt
     */
    public fun fetchAndAddAt(index: Int, delta: Long): Long

    /**
     * Atomically adds the given [delta] to the element of this [AtomicLongArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.addAndFetchAt
     */
    public fun addAndFetchAt(index: Int, delta: Long): Long

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
 *
 * @see atomicLongArrayOf
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.initCons
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public inline fun AtomicLongArray(size: Int, init: (Int) -> Long): AtomicLongArray =
    AtomicLongArray(LongArray(size) { init(it) })

/**
 * Atomically increments the element of this [AtomicLongArray] at the given [index] by one and returns the old value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndIncrementAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLongArray.fetchAndIncrementAt(index: Int): Long = this.fetchAndAddAt(index, 1)

/**
 * Atomically increments the element of this [AtomicLongArray] at the given [index] by one and returns the new value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.incrementAndFetchAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLongArray.incrementAndFetchAt(index: Int): Long = this.addAndFetchAt(index, 1)

/**
 * Atomically decrements the element of this [AtomicLongArray] at the given [index] by one and returns the new value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.decrementAndFetchAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLongArray.decrementAndFetchAt(index: Int): Long = this.addAndFetchAt(index, -1)

/**
 * Atomically decrements the element of this [AtomicLongArray] at the given [index] by one and returns the old value of the element.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndDecrementAt
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLongArray.fetchAndDecrementAt(index: Int): Long = this.fetchAndAddAt(index, -1)

/**
 * A generic array of objects in which elements may be updated atomically.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicArray] stores an [Array] with elements of type [T] and atomically updates it's elements.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicArray] are represented by [java.util.concurrent.atomic.AtomicReferenceArray].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicArray] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public expect class AtomicArray<T> {

    /**
     * Creates a new [AtomicArray] filled with elements of the given [array].
     *
     * @see atomicArrayOf
     * @see atomicArrayOfNulls
     *
     * @sample samples.concurrent.atomics.AtomicArray.arrCons
     */
    public constructor(array: Array<T>)

    /**
     * Returns the number of elements in the array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.size
     */
    public val size: Int

    /**
     * Atomically loads the value from the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.loadAt
     */
    public fun loadAt(index: Int): T

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.storeAt
     */
    public fun storeAt(index: Int, newValue: T)

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index]
     * and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.exchangeAt
     */
    public fun exchangeAt(index: Int, newValue: T): T

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
    public fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean

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
    public fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T

    /**
     * Returns the string representation of the underlying array of objects.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Creates a new [AtomicArray] if the given type with the given [size],
 * where each element is initialized by calling the given [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 *
 * @throws RuntimeException if the specified [size] is negative.
 *
 * @see atomicArrayOf
 * @see atomicArrayOfNulls
 *
 * @sample samples.concurrent.atomics.AtomicArray.initCons
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
@Suppress("UNCHECKED_CAST")
public inline fun <reified T> AtomicArray(size: Int, init: (Int) -> T): AtomicArray<T> =
    AtomicArray(Array(size) { init(it) })

/**
 * Returns a new [AtomicArray] of the given type initialized with specified elements.
 *
 * @sample samples.concurrent.atomics.AtomicArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public expect fun <T> atomicArrayOf(vararg elements: T): AtomicArray<T>

/**
 * Returns a new [AtomicArray] of the given type with the given [size], initialized with null values.
 *
 * @throws RuntimeException if the specified [size] is negative.
 *
 * @sample samples.concurrent.atomics.AtomicArray.nullFactory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@Suppress("NOTHING_TO_INLINE")
public inline fun <reified T> atomicArrayOfNulls(size: Int): AtomicArray<T?> = AtomicArray(size) { null }

/**
 * Returns a new [AtomicIntArray] containing the specified [Int] numbers.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public expect fun atomicIntArrayOf(vararg elements: Int): AtomicIntArray

/**
 * Returns a new [AtomicLongArray] containing the specified [Long] numbers.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public expect fun atomicLongArrayOf(vararg elements: Long): AtomicLongArray
