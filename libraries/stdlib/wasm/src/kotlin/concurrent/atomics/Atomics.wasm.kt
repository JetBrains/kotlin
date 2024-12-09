/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

/**
 * An [Int] value that may be updated atomically.
 *
 * Since the Wasm platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicInt public actual constructor(private var value: Int) {

    /**
     * Atomically loads the value from this [AtomicInt].
     */
    public actual fun load(): Int = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     */
    public actual fun store(newValue: Int) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] and returns the old value.
     */
    public actual fun exchange(newValue: Int): Int {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue],
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
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Int, newValue: Int): Int {
        val oldValue = value
        if (oldValue == expectedValue) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Int): Int {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     */
    public actual fun addAndFetch(delta: Int): Int {
        value += delta
        return value
    }

    /**
     * Returns the string representation of the underlying [Int] value.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Long] value that may be updated atomically.
 *
 * Since the Wasm platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicLong public actual constructor(private var value: Long) {
    /**
     * Atomically loads the value from this [AtomicLong].
     */
    public actual fun load(): Long = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     */
    public actual fun store(newValue: Long) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] and returns the old value.
     */
    public actual fun exchange(newValue: Long): Long {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Long, newValue: Long): Boolean {
        if (value != expectedValue) return false
        value = newValue
        return true
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Long, newValue: Long): Long {
        val oldValue = value
        if (oldValue == expectedValue) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Long): Long {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     */
    public actual fun addAndFetch(delta: Long): Long {
        value += delta
        return value
    }

    /**
     * Returns the string representation of the underlying [Long] value.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Boolean] value that may be updated atomically.
 *
 * Since the Wasm platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicBoolean public actual constructor(private var value: Boolean) {

    /**
     * Atomically loads the value from this [AtomicBoolean].
     */
    public actual fun load(): Boolean = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     */
    public actual fun store(newValue: Boolean) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     */
    public actual fun exchange(newValue: Boolean): Boolean {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean {
        if (value != expectedValue) return false
        value = newValue
        return true
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean {
        val oldValue = value
        if (oldValue == expectedValue) {
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
 * An object reference that may be updated atomically.
 *
 * Since the Wasm platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicReference<T> public actual constructor(private var value: T) {
    /**
     * Atomically loads the value from this [AtomicReference].
     */
    public actual fun load(): T = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     */
    public actual fun store(newValue: T) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] and returns the old value.
     */
    public actual fun exchange(newValue: T): T {
        val oldValue = value
        value = newValue
        return oldValue
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: T, newValue: T): Boolean {
        if (value != expectedValue) return false
        value = newValue
        return true
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: T, newValue: T): T {
        val oldValue = value
        if (oldValue == expectedValue) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying object.
     */
    public actual override fun toString(): String = value.toString()
}