/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

/**
 * An [Int] value which provides API of the common [AtomicInt].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicInt public actual constructor(private var value: Int) {

    /**
     * Gets the value of the atomic.
     */
    public actual fun load(): Int = value

    /**
     * Sets the value of the atomic to the [new value][newValue].
     */
    public actual fun store(newValue: Int) { value = newValue }

    /**
     * Sets the value to the given [new value][newValue] and returns the old value.
     */
    public actual fun exchange(newValue: Int): Int {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Int, newValue: Int): Boolean {
        if (value != expectedValue) return false
        value = newValue
        return true
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expected: Int, newValue: Int): Int {
        val oldValue = value
        if (oldValue == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Adds the [given value][delta] to the current value and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Int): Int {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Adds the [given value][delta] to the current value and returns the new value.
     */
    public actual fun addAndFetch(delta: Int): Int {
        value += delta
        return value
    }

    /**
     * Increments the current value by one and returns the old value.
     */
    public actual fun fetchAndIncrement(): Int = value++

    /**
     * Increments the current value by one and returns the new value.
     */
    public actual fun incrementAndFetch(): Int = ++value

    /**
     * Decrements the current value by one and returns the new value.
     */
    public actual fun fetchAndDecrement(): Int = value--

    /**
     * Decrements the current value by one and returns the old value.
     */
    public actual fun decrementAndFetch(): Int = --value

    /**
     * Returns the string representation of the underlying [Int] value.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * An [Long] value which provides API of the common [AtomicLong].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicLong public actual constructor(private var value: Long) {
    /**
     * Gets the value of the atomic.
     */
    public actual fun load(): Long = value

    /**
     * Sets the value of the atomic to the [new value][newValue].
     */
    public actual fun store(newValue: Long) { value = newValue }

    /**
     * Sets the value to the given [new value][newValue] and returns the old value.
     */
    public actual fun exchange(newValue: Long): Long {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expected: Long, newValue: Long): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expected: Long, newValue: Long): Long {
        val oldValue = value
        if (oldValue == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Adds the [given value][delta] to the current value and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Long): Long {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Adds the [given value][delta] to the current value and returns the new value.
     */
    public actual fun addAndFetch(delta: Long): Long {
        value += delta
        return value
    }

    /**
     * Increments the current value by one and returns the old value.
     */
    public actual fun fetchAndIncrement(): Long = value++

    /**
     * Increments the current value by one and returns the new value.
     */
    public actual fun incrementAndFetch(): Long = ++value

    /**
     * Decrements the current value by one and returns the new value.
     */
    public actual fun fetchAndDecrement(): Long = value--

    /**
     * Decrements the current value by one and returns the old value.
     */
    public actual fun decrementAndFetch(): Long = --value

    /**
     * Returns the string representation of the underlying [Long] value.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * An [Boolean] value which provides API of the common [AtomicBoolean].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicBoolean public actual constructor(private var value: Boolean) {

    /**
     * Gets the value of the atomic.
     */
    public actual fun load(): Boolean = value

    /**
     * Sets the value of the atomic to the [new value][newValue].
     */
    public actual fun store(newValue: Boolean) { value = newValue }

    /**
     * Sets the value to the given [new value][newValue] and returns the old value.
     */
    public actual fun exchange(newValue: Boolean): Boolean {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expected: Boolean, newValue: Boolean): Boolean {
        val oldValue = value
        if (oldValue == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying [Boolean] value.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * An object reference which provides API of the common [AtomicReference].
 *
 * Does not provide any atomicity guarantees since the JS platform does not support multi-threading.
 */
public actual class AtomicReference<T> public actual constructor(private var value: T) {
    /**
     * Gets the value of the atomic.
     */
    public actual fun load(): T = value

    /**
     * Sets the value of the atomic to the [new value][newValue].
     */
    public actual fun store(newValue: T) { value = newValue }

    /**
     * Sets the value to the given [new value][newValue] and returns the old value.
     */
    public actual fun exchange(newValue: T): T {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expected: T, newValue: T): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    /**
     * Sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expected: T, newValue: T): T {
        val oldValue = value
        if (oldValue == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying object.
     */
    public actual override fun toString(): String = value.toString()
}
