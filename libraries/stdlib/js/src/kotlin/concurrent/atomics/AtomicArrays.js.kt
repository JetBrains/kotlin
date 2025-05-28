/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

/**
 * An array of ints in which elements may be updated atomically.
 *
 * Since the JS platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
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
        this.array = array.asDynamic().slice().unsafeCast<IntArray>()
    }

    /**
     * Creates a new [AtomicIntArray] that wraps the given [array].
     */
    internal constructor(array: IntArray, @Suppress("UNUSED_PARAMETER") dummy: Any?) {
        this.array = array
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
        return array[index]
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: Int) {
        checkBounds(index)
        array[index] = newValue
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
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicIntArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
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
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the old value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndAddAt
     */
    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    /**
     * Atomically adds the given [delta] to the element of this [AtomicIntArray] at the given [index] and returns the new value of the element.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     *  @sample samples.concurrent.atomics.AtomicIntArray.addAndFetchAt
     */
    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        checkBounds(index)
        array[index] += delta
        return array[index]
    }

    /**
     * Returns the string representation of the underlying array of ints.
     */
    public actual override fun toString(): String = array.contentToString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.updateAt(index: Int, transform: (Int) -> Int): Unit {
    storeAt(index, transform(loadAt(index)))
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.updateAndFetchAt(index: Int, transform: (Int) -> Int): Int {
    val updated = transform(loadAt(index))
    storeAt(index, updated)
    return updated
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.fetchAndUpdateAt(index: Int, transform: (Int) -> Int): Int {
    val old = loadAt(index)
    storeAt(index, transform(old))
    return old
}

/**
 * An array of longs in which elements may be updated atomically.
 *
 * Since the JS platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicLongArray {
    private val array: LongArray

    /**
     * Creates a new [AtomicLongArray] of the given [size], with all elements initialized to zero.
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
        this.array = array.asDynamic().slice().unsafeCast<LongArray>()
    }

    /**
     * Creates a new [AtomicLongArray] that wraps the given [array].
     */
    internal constructor(array: LongArray, @Suppress("UNUSED_PARAMETER") dummy: Any?) {
        this.array = array
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
        return array[index]
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: Long) {
        checkBounds(index)
        array[index] = newValue
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
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicLongArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by value.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicLongArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
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
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
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
        val oldValue = array[index]
        array[index] += delta
        return oldValue
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
        array[index] += delta
        return array[index]
    }

    /**
     * Returns the string representation of the underlying array of longs.
     */
    public actual override fun toString(): String = array.contentToString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.updateAt(index: Int, transform: (Long) -> Long): Unit {
    storeAt(index, transform(loadAt(index)))
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.updateAndFetchAt(index: Int, transform: (Long) -> Long): Long {
    val updated = transform(loadAt(index))
    storeAt(index, updated)
    return updated
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.fetchAndUpdateAt(index: Int, transform: (Long) -> Long): Long {
    val old = loadAt(index)
    storeAt(index, transform(old))
    return old
}

/**
 * A generic array of objects in which elements may be updated atomically.
 *
 * Since the JS platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
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
    public actual constructor (array: Array<T>) {
        this.array = array.asDynamic().slice() as Array<T>
    }

    /**
     * Creates a new [AtomicArray] that wraps the given [array].
     */
    internal constructor(array: Array<T>, @Suppress("UNUSED_PARAMETER") dummy: Any?) {
        this.array = array
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
        return array[index]
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index].
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.storeAt
     */
    public actual fun storeAt(index: Int, newValue: T) {
        checkBounds(index)
        array[index] = newValue
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
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Atomically stores the [new value][newValue] into the element of this [AtomicArray] at the given [index]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by reference.
     *
     * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
     *
     * @sample samples.concurrent.atomics.AtomicArray.compareAndSetAt
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean {
        checkBounds(index)
        if (array[index] !== expectedValue) return false
        array[index] = newValue
        return true
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
        val oldValue = array[index]
        if (oldValue === expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying array of objects.
     */
    public actual override fun toString(): String = array.contentToString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.updateAt(index: Int, transform: (T) -> T): Unit {
    storeAt(index, transform(loadAt(index)))
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.updateAndFetchAt(index: Int, transform: (T) -> T): T {
    val updated = transform(loadAt(index))
    storeAt(index, updated)
    return updated
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] function may be invoked multiple times.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.fetchAndUpdateAt(index: Int, transform: (T) -> T): T {
    val old = loadAt(index)
    storeAt(index, transform(old))
    return old
}
