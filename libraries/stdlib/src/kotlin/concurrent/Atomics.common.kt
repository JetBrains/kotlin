/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AtomicsKt")

@file:Suppress("NEWER_VERSION_IN_SINCE_KOTLIN", "API_NOT_AVAILABLE")

package kotlin.concurrent

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * An [Int] value that may be updated atomically.
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
@SinceKotlin("2.1")
@ExperimentalStdlibApi
@ActualizeByJvmBuiltinProvider
public expect class AtomicInt public constructor(value: Int) {
    /**
     * Atomically loads the value from this [AtomicInt].
     */
    public fun load(): Int

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     */
    public fun store(newValue: Int)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] and returns the old value.
     */
    public fun exchange(newValue: Int): Int

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expectedValue: Int, newValue: Int): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expectedValue: Int, newValue: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the old value.
     */
    public fun fetchAndAdd(delta: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     */
    public fun addAndFetch(delta: Int): Int

    /**
     * Returns the string representation of the underlying [Int] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value of this [AtomicInt].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public operator fun AtomicInt.plusAssign(delta: Int): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value of this [AtomicInt].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public operator fun AtomicInt.minusAssign(delta: Int): Unit { this.addAndFetch(-delta) }

/**
 * Atomically increments the current value of this [AtomicInt] by one and returns the old value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicInt.fetchAndIncrement(): Int = this.fetchAndAdd(1)

/**
 * Atomically increments the current value of this [AtomicInt] by one and returns the new value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicInt.incrementAndFetch(): Int = this.addAndFetch(1)

/**
 * Atomically decrements the current value of this [AtomicInt] by one and returns the new value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicInt.decrementAndFetch(): Int = this.addAndFetch(-1)

/**
 * Atomically decrements the current value of this [AtomicInt] by one and returns the old value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicInt.fetchAndDecrement(): Int = this.fetchAndAdd(-1)

/**
 * A [Long] value that may be updated atomically.
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
@SinceKotlin("2.1")
@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicLong public constructor(value: Long) {
    /**
     * Atomically loads the value from this [AtomicLong].
     */
    public fun load(): Long

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     */
    public fun store(newValue: Long)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong]. and returns the old value.
     */
    public fun exchange(newValue: Long): Long

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expectedValue: Long, newValue: Long): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expectedValue: Long, newValue: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the old value.
     */
    public fun fetchAndAdd(delta: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     */
    public fun addAndFetch(delta: Long): Long

    /**
     * Returns the string representation of the underlying [Long] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value of this [AtomicLong].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public operator fun AtomicLong.plusAssign(delta: Long): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value of this [AtomicLong].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public operator fun AtomicLong.minusAssign(delta: Long): Unit { this.addAndFetch(-delta) }

/**
 * Atomically increments the current value of this [AtomicLong] by one and returns the old value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicLong.fetchAndIncrement(): Long = this.fetchAndAdd(1)

/**
 * Atomically increments the current value of this [AtomicLong] by one and returns the new value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicLong.incrementAndFetch(): Long = this.addAndFetch(1)

/**
 * Atomically decrements the current value of this [AtomicLong] by one and returns the new value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicLong.decrementAndFetch(): Long = this.addAndFetch(-1)

/**
 * Atomically decrements the current value of this [AtomicLong] by one and returns the old value.
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public fun AtomicLong.fetchAndDecrement(): Long = this.fetchAndAdd(-1)

/**
 * A [Boolean] value that may be updated atomically.
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
@SinceKotlin("2.1")
@ExperimentalStdlibApi
@ActualizeByJvmBuiltinProvider
public expect class AtomicBoolean public constructor(value: Boolean) {
    /**
     * Atomically loads the value from this [AtomicBoolean].
     */
    public fun load(): Boolean

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     */
    public fun store(newValue: Boolean)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     */
    public fun exchange(newValue: Boolean): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean

    /**
     * Returns the string representation of the current [Boolean] value.
     */
    public override fun toString(): String
}

/**
 * An object reference that may be updated atomically.
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
@SinceKotlin("2.1")
@ExperimentalStdlibApi
@ActualizeByJvmBuiltinProvider
public expect class AtomicReference<T> public constructor(value: T) {
    /**
     * Atomically loads the value from this [AtomicReference].
     */
    public fun load(): T

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     */
    public fun store(newValue: T)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference]. and returns the old value.
     */
    public fun exchange(newValue: T): T

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndSet(expectedValue: T, newValue: T): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndExchange(expectedValue: T, newValue: T): T

    /**
     * Returns the string representation of the underlying object.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}
