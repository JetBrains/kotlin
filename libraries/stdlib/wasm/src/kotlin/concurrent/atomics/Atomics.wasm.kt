/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly

/**
 * An [Int] value that may be updated atomically.
 *
 * Since the Wasm platform does not support multi-threading,
 * the implementation is trivial and has no atomic synchronizations.
 *
 * @constructor Creates a new [AtomicInt] initialized with the specified value.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicInt public actual constructor(private var value: Int) {

    /**
     * Atomically loads the value from this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.load
     */
    public actual fun load(): Int = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.store
     */
    public actual fun store(newValue: Int) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicInt.compareAndSet
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
     *
     * @sample samples.concurrent.atomics.AtomicInt.compareAndExchange
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
     *
     * @sample samples.concurrent.atomics.AtomicInt.fetchAndAdd
     */
    public actual fun fetchAndAdd(delta: Int): Int {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.addAndFetch
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
 *
 * @constructor Creates a new [AtomicLong] initialized with the specified value.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicLong public actual constructor(private var value: Long) {
    /**
     * Atomically loads the value from this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.load
     */
    public actual fun load(): Long = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.store
     */
    public actual fun store(newValue: Long) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicLong.compareAndSet
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
     *
     * @sample samples.concurrent.atomics.AtomicLong.compareAndExchange
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
     *
     * @sample samples.concurrent.atomics.AtomicLong.fetchAndAdd
     */
    public actual fun fetchAndAdd(delta: Long): Long {
        val oldValue = value
        value += delta
        return oldValue
    }

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.addAndFetch
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
 *
 * @constructor Creates a new [AtomicBoolean] initialized with the specified value.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicBoolean public actual constructor(private var value: Boolean) {

    /**
     * Atomically loads the value from this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.load
     */
    public actual fun load(): Boolean = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.store
     */
    public actual fun store(newValue: Boolean) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.exchange
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
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.compareAndSet
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
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.compareAndExchange
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
 *
 * @constructor Creates a new [AtomicReference] initialized with the specified value.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicReference<T> public actual constructor(private var value: T) {
    /**
     * Atomically loads the value from this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.load
     */
    public actual fun load(): T = value

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.store
     */
    public actual fun store(newValue: T) { value = newValue }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicReference.exchange
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
     * Comparison of values is done by reference.
     *
     * @sample samples.concurrent.atomics.AtomicReference.compareAndSet
     */
    public actual fun compareAndSet(expectedValue: T, newValue: T): Boolean {
        if (value !== expectedValue) return false
        value = newValue
        return true
    }

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by reference.
     *
     * @sample samples.concurrent.atomics.AtomicReference.compareAndExchange
     */
    public actual fun compareAndExchange(expectedValue: T, newValue: T): T {
        val oldValue = value
        if (oldValue === expectedValue) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the underlying object.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicInt.update(transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    store(transform(load()))
}

/**
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicInt.fetchAndUpdate(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = load()
    store(transform(old))
    return old
}

/**
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicInt.updateAndFetch(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val newValue = transform(load())
    store(newValue)
    return newValue
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLong.update(transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    store(transform(load()))
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLong.fetchAndUpdate(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = load()
    store(transform(old))
    return old
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLong.updateAndFetch(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val newValue = transform(load())
    store(newValue)
    return newValue
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicReference<T>.update(transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    store(transform(load()))
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicReference<T>.fetchAndUpdate(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = load()
    store(transform(old))
    return old
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * Wasm does not support multithreading, thus the implementation is trivial,
 * and [transform] will only be invoked exactly once to compute the result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicReference<T>.updateAndFetch(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val newValue = transform(load())
    store(newValue)
    return newValue
}
