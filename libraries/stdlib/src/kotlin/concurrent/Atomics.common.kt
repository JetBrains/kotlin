/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * An [Int] value that may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicInt] stores a volatile [Int] variable and atomically updates it.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicInt] are represented by [java.util.concurrent.atomic.AtomicInteger].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicInt] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicInt public constructor(value: Int) {
    /**
     * Atomically gets the value of the atomic.
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun load(): Int

    /**
     * Atomically sets the value of the atomic to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun store(newValue: Int)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun exchange(newValue: Int): Int

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expectedValue: Int, newValue: Int): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expected: Int, newValue: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    public fun fetchAndAdd(delta: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    public fun addAndFetch(delta: Int): Int

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    public fun fetchAndIncrement(): Int

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    public fun incrementAndFetch(): Int

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    public fun decrementAndFetch(): Int

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    public fun fetchAndDecrement(): Int

    /**
     * Returns the string representation of the underlying [Int] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicInt.plusAssign(delta: Int): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicInt.minusAssign(delta: Int): Unit { this.addAndFetch(-delta) }

/**
 * A [Long] value that may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicLong] stores a volatile [Long] variable and atomically updates it.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicLong] are represented by [java.util.concurrent.atomic.AtomicLong].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicLong] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicLong public constructor(value: Long) {
    /**
     * Atomically gets the value of the atomic.
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun load(): Long

    /**
     * Atomically sets the value of the atomic to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun store(newValue: Long)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun exchange(newValue: Long): Long

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expected: Long, newValue: Long): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expected: Long, newValue: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    public fun fetchAndAdd(delta: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    public fun addAndFetch(delta: Long): Long

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    public fun fetchAndIncrement(): Long

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    public fun incrementAndFetch(): Long

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    public fun decrementAndFetch(): Long

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    public fun fetchAndDecrement(): Long

    /**
     * Returns the string representation of the underlying [Long] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicLong.plusAssign(delta: Long): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicLong.minusAssign(delta: Long): Unit { this.addAndFetch(-delta) }

/**
 * A [Boolean] value that may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicBoolean] stores a volatile [Boolean] variable and atomically updates it.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicBoolean] are represented by [java.util.concurrent.atomic.AtomicInteger].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicBoolean] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicBoolean public constructor(value: Boolean) {
    /**
     * Atomically gets the value of the atomic.
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun load(): Boolean

    /**
     * Atomically sets the value of the atomic to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun store(newValue: Boolean)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun exchange(newValue: Boolean): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expected: Boolean, newValue: Boolean): Boolean

    /**
     * Returns the string representation of the current [Boolean] value.
     */
    public override fun toString(): String
}

/**
 * An object reference that may be updated atomically with guaranteed sequential consistent ordering.
 *
 * Platform-specific implementation details:
 *
 * When targeting the Native backend, [AtomicReference] stores a volatile variable of type [T] and atomically updates it.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * When targeting the JVM, instances of [AtomicReference] are represented by [java.util.concurrent.atomic.AtomicReference].
 * For details about guarantees of volatile accesses and updates of atomics refer to The Java Language Specification (17.4 Memory Model).
 *
 * For JS and Wasm [AtomicReference] is implemented trivially and is not thread-safe since these platforms do not support multi-threading.
 */
@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicReference<T> public constructor(value: T) {
    /**
     * Atomically gets the value of the atomic.
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun load(): T

    /**
     * Atomically sets the value of the atomic to the [new value][newValue].
     *
     * Provides sequential consistent ordering guarantees.
     */
    public fun store(newValue: T)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun exchange(newValue: T): T

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndSet(expected: T, newValue: T): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndExchange(expected: T, newValue: T): T

    /**
     * Returns the string representation of the underlying object.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}
