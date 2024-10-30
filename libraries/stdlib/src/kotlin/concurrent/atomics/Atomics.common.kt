/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AtomicsKt")

@file:Suppress("NEWER_VERSION_IN_SINCE_KOTLIN", "API_NOT_AVAILABLE")

package kotlin.concurrent.atomics

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
@ExperimentalAtomicApi
public expect class AtomicInt public constructor(value: Int) {
    /**
     * Atomically loads the value from this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.load
     */
    public fun load(): Int

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.store
     */
    public fun store(newValue: Int)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicInt.compareAndSet
     */
    public fun compareAndSet(expectedValue: Int, newValue: Int): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.compareAndExchange
     */
    public fun compareAndExchange(expectedValue: Int, newValue: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.fetchAndAdd
     */
    public fun fetchAndAdd(delta: Int): Int

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.addAndFetch
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
 *
 * @sample samples.concurrent.atomics.AtomicInt.plusAssign
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public operator fun AtomicInt.plusAssign(delta: Int): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value of this [AtomicInt].
 *
 * @sample samples.concurrent.atomics.AtomicInt.minusAssign
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public operator fun AtomicInt.minusAssign(delta: Int): Unit { this.addAndFetch(-delta) }

/**
 * Atomically increments the current value of this [AtomicInt] by one and returns the old value.
 *
 * @sample samples.concurrent.atomics.AtomicInt.fetchAndIncrement
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicInt.fetchAndIncrement(): Int = this.fetchAndAdd(1)

/**
 * Atomically increments the current value of this [AtomicInt] by one and returns the new value.
 *
 * @sample samples.concurrent.atomics.AtomicInt.incrementAndFetch
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicInt.incrementAndFetch(): Int = this.addAndFetch(1)

/**
 * Atomically decrements the current value of this [AtomicInt] by one and returns the new value.
 *
 * @sample samples.concurrent.atomics.AtomicInt.decrementAndFetch
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicInt.decrementAndFetch(): Int = this.addAndFetch(-1)

/**
 * Atomically decrements the current value of this [AtomicInt] by one and returns the old value.
 *
 * @sample samples.concurrent.atomics.AtomicInt.fetchAndDecrement
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
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
@ExperimentalAtomicApi
public expect class AtomicLong public constructor(value: Long) {
    /**
     * Atomically loads the value from this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.load
     */
    public fun load(): Long

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.store
     */
    public fun store(newValue: Long)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong]. and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicLong.compareAndSet
     */
    public fun compareAndSet(expectedValue: Long, newValue: Long): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.compareAndExchange
     */
    public fun compareAndExchange(expectedValue: Long, newValue: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.fetchAndAdd
     */
    public fun fetchAndAdd(delta: Long): Long

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.addAndFetch
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
 *
 * @sample samples.concurrent.atomics.AtomicLong.plusAssign
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public operator fun AtomicLong.plusAssign(delta: Long): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value of this [AtomicLong].
 *
 * @sample samples.concurrent.atomics.AtomicLong.minusAssign
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public operator fun AtomicLong.minusAssign(delta: Long): Unit { this.addAndFetch(-delta) }

/**
 * Atomically increments the current value of this [AtomicLong] by one and returns the old value.
 *
 * @sample samples.concurrent.atomics.AtomicLong.fetchAndIncrement
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLong.fetchAndIncrement(): Long = this.fetchAndAdd(1)

/**
 * Atomically increments the current value of this [AtomicLong] by one and returns the new value.
 *
 * @sample samples.concurrent.atomics.AtomicLong.incrementAndFetch
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLong.incrementAndFetch(): Long = this.addAndFetch(1)

/**
 * Atomically decrements the current value of this [AtomicLong] by one and returns the new value.
 *
 * @sample samples.concurrent.atomics.AtomicLong.decrementAndFetch
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public fun AtomicLong.decrementAndFetch(): Long = this.addAndFetch(-1)

/**
 * Atomically decrements the current value of this [AtomicLong] by one and returns the old value.
 *
 * @sample samples.concurrent.atomics.AtomicLong.fetchAndDecrement
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
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
@ExperimentalAtomicApi
public expect class AtomicBoolean public constructor(value: Boolean) {
    /**
     * Atomically loads the value from this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.load
     */
    public fun load(): Boolean

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.store
     */
    public fun store(newValue: Boolean)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.compareAndSet
     */
    public fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.compareAndExchange
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
@ExperimentalAtomicApi
public expect class AtomicReference<T> public constructor(value: T) {
    /**
     * Atomically loads the value from this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.load
     */
    public fun load(): T

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.store
     */
    public fun store(newValue: T)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference]. and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicReference.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicReference.compareAndSet
     */
    public fun compareAndSet(expectedValue: T, newValue: T): Boolean

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by reference.
     *
     * @sample samples.concurrent.atomics.AtomicReference.compareAndExchange
     */
    public fun compareAndExchange(expectedValue: T, newValue: T): T

    /**
     * Returns the string representation of the underlying object.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public override fun toString(): String
}
