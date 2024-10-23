/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

/**
 * An array of ints which provides API of the common [AtomicIntArray].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
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
     * Gets the value of the element at the given [index].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): Int {
        checkBounds(index)
        return array[index]
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun storeAt(index: Int, newValue: Int) {
        checkBounds(index)
        array[index] = newValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun exchangeAt(index: Int, newValue: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
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
     * Adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        checkBounds(index)
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    /**
     * Adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        checkBounds(index)
        array[index] += delta
        return array[index]
    }

    /**
     * Increments the element at the given [index] by one and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndIncrementAt(index: Int): Int {
        checkBounds(index)
        return array[index]++
    }

    /**
     * Increments the element at the given [index] by one and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun incrementAndFetchAt(index: Int): Int {
        checkBounds(index)
        return ++array[index]
    }

    /**
     * Decrements the element at the given [index] by one and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndDecrementAt(index: Int): Int {
        checkBounds(index)
        return array[index]--
    }

    /**
     * Decrements the element at the given [index] by one and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun decrementAndFetchAt(index: Int): Int {
        checkBounds(index)
        return --array[index]
    }

    /**
     * Returns the string representation of the underlying array of ints.
     */
    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

/**
 * An array of longs which provides API of the common [AtomicLongArray].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicLongArray {
    private val array: LongArray

    /**
     * Creates a new [AtomicLongArray] of the given [size], with all elements initialized to zero.
     *
     * @throws RuntimeException if the specified [size] is negative.
     */
    public actual constructor(size: Int) {
        array = LongArray(size)
    }

    /**
     * Creates a new [AtomicIntArray] filled with elements of the given [array].
     */
    public actual constructor(array: LongArray) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    public actual val size: Int get() = array.size

    /**
     * Gets the value of the element at the given [index].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): Long {
        checkBounds(index)
        return array[index]
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun storeAt(index: Int, newValue: Long) {
        checkBounds(index)
        array[index] = newValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun exchangeAt(index: Int, newValue: Long): Long {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
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
     * Adds the given [delta] to the element at the given [index] and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndAddAt(index: Int, delta: Long): Long {
        checkBounds(index)
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    /**
     * Adds the given [delta] to the element at the given [index] and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun addAndFetchAt(index: Int, delta: Long): Long {
        checkBounds(index)
        array[index] += delta
        return array[index]
    }

    /**
     * Increments the element at the given [index] by one and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndIncrementAt(index: Int): Long {
        checkBounds(index)
        return array[index]++
    }

    /**
     * Increments the element at the given [index] by one and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun incrementAndFetchAt(index: Int): Long {
        checkBounds(index)
        return ++array[index]
    }

    /**
     * Decrements the element at the given [index] by one and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun fetchAndDecrementAt(index: Int): Long {
        checkBounds(index)
        return array[index]--
    }

    /**
     * Decrements the element at the given [index] by one and returns the new value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun decrementAndFetchAt(index: Int): Long {
        checkBounds(index)
        return --array[index]
    }

    /**
     * Returns the string representation of the underlying array of longs.
     */
    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}

/**
 * An array of longs which provides API of the common [AtomicArray].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicArray<T> {
    private val array: Array<T>

    /**
     * Creates a new [AtomicArray] filled with elements of the given [array].
     */
    public actual constructor (array: Array<T>) {
        this.array = array.copyOf()
    }

    /**
     * Returns the number of elements in the array.
     */
    public actual val size: Int get() = array.size

    /**
     * Gets the value of the element at the given [index].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun loadAt(index: Int): T {
        checkBounds(index)
        return array[index]
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue].
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun storeAt(index: Int, newValue: T) {
        checkBounds(index)
        array[index] = newValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * and returns the old value of the element.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun exchangeAt(index: Int, newValue: T): T {
        checkBounds(index)
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue].
     * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean {
        checkBounds(index)
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    /**
     * Sets the value of the element at the given [index] to the [new value][newValue]
     * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
     *
     * Comparison of values is done by value.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this array.
     */
    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T {
        checkBounds(index)
        val oldValue = array[index]
        if (oldValue == expectedValue) {
            array[index] = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying array of objects.
     */
    public actual override fun toString(): String = array.toString()

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= array.size) throw IndexOutOfBoundsException("index $index")
    }
}
