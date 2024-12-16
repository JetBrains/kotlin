/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * An [IntArray] in which elements are always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("1.9")
@RequireKotlin(version = "1.9.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
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
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
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
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
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
    public val length: Int get() = array.size

    /**
     * Atomically gets the value of the element at the given [index].
     *
     * Provides sequential consistent ordering guarantees.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
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
@Suppress("UNCHECKED_CAST")
public inline fun <reified T> AtomicArray(size: Int, init: (Int) -> T): AtomicArray<T> {
    val inner = arrayOfNulls<T>(size)
    for (index in 0 until size) {
        inner[index] = init(index)
    }
    return AtomicArray(inner as Array<T>)
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
internal external fun <T> Array<T>.getAndSet(index: Int, value: T): T

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
