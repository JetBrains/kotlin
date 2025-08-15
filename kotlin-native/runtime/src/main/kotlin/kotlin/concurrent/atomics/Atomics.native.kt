/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.concurrent.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly
import kotlin.native.internal.*

/**
 * An [Int] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange], [fetchAndAdd], [addAndFetch],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * @constructor Creates a new [AtomicInt] initialized with the specified value.
 */
@Suppress("DEPRECATION")
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicInt public actual constructor(
    @get:Deprecated("To read the atomic value use load().", ReplaceWith("this.load()"), DeprecationLevel.ERROR)
    @set:Deprecated("To atomically set the new value use store(newValue: Int).", ReplaceWith("this.store(newValue)"), DeprecationLevel.ERROR)
    @Volatile public var value: Int
) {
    /**
     * Atomically loads the value from this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.load
     */
    public actual fun load(): Int = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     *
     * @sample samples.concurrent.atomics.AtomicInt.store
     */
    public actual fun store(newValue: Int): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.exchange
     */
    public actual fun exchange(newValue: Int): Int = this::value.getAndSetField(newValue)

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
    public actual fun compareAndSet(expectedValue: Int, newValue: Int): Boolean =
        this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.compareAndExchange
     */
    public actual fun compareAndExchange(expectedValue: Int, newValue: Int): Int =
        this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.fetchAndAdd
     */
    public actual fun fetchAndAdd(delta: Int): Int = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicInt.addAndFetch
     */
    public actual fun addAndFetch(delta: Int): Int = this::value.getAndAddField(delta) + delta

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    @Deprecated("Use exchange(newValue: Int) instead.", ReplaceWith("this.exchange(newValue)"), DeprecationLevel.ERROR)
    public fun getAndSet(newValue: Int): Int = this::value.getAndSetField(newValue)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    @Deprecated("Use fetchAndAdd(newValue: Int) instead.", ReplaceWith("this.fetchAndAdd(newValue)"), DeprecationLevel.ERROR)
    public fun getAndAdd(delta: Int): Int = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    @Deprecated("Use addAndFetch(newValue: Int) instead.", ReplaceWith("this.addAndFetch(newValue)"), DeprecationLevel.ERROR)
    public fun addAndGet(delta: Int): Int = this::value.getAndAddField(delta) + delta

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    @Deprecated("Use fetchAndIncrement() instead.", ReplaceWith("this.fetchAndIncrement()"), DeprecationLevel.ERROR)
    public fun getAndIncrement(): Int = this::value.getAndAddField(1)

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    @Deprecated("Use incrementAndFetch() instead.", ReplaceWith("this.incrementAndFetch()"), DeprecationLevel.ERROR)
    public fun incrementAndGet(): Int = this::value.getAndAddField(1) + 1

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    @Deprecated("Use decrementAndFetch() instead.", ReplaceWith("this.decrementAndFetch()"), DeprecationLevel.ERROR)
    public fun decrementAndGet(): Int = this::value.getAndAddField(-1) - 1

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    @Deprecated("Use fetchAndDecrement() instead.", ReplaceWith("this.fetchAndDecrement()"), DeprecationLevel.ERROR)
    public fun getAndDecrement(): Int = this::value.getAndAddField(-1)

    /**
     * Returns the string representation of the [Int] value stored in this [AtomicInt].
     *
     * This operation does not provide any atomicity guarantees.
     */
    @Suppress("DEPRECATION_ERROR")
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Long] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange], [fetchAndAdd], [addAndFetch],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * @constructor Creates a new [AtomicLong] initialized with the specified value.
 */
@Suppress("DEPRECATION")
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicLong public actual constructor(
    @get:Deprecated("To read the atomic value use load().", ReplaceWith("this.load()"), DeprecationLevel.ERROR)
    @set:Deprecated("To atomically set the new value use store(newValue: Long).", ReplaceWith("this.store(newValue)"), DeprecationLevel.ERROR)
    @Volatile public var value: Long
) {
    /**
     * Atomically loads the value from this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.load
     */
    public actual fun load(): Long = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     *
     * @sample samples.concurrent.atomics.AtomicLong.store
     */
    public actual fun store(newValue: Long): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.exchange
     */
    public actual fun exchange(newValue: Long): Long = this::value.getAndSetField(newValue)

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
    public actual fun compareAndSet(expectedValue: Long, newValue: Long): Boolean =
        this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.compareAndExchange
     */
    public actual fun compareAndExchange(expectedValue: Long, newValue: Long): Long =
        this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.fetchAndAdd
     */
    public actual fun fetchAndAdd(delta: Long): Long = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     *
     * @sample samples.concurrent.atomics.AtomicLong.addAndFetch
     */
    public actual fun addAndFetch(delta: Long): Long = this::value.getAndAddField(delta) + delta

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    @Deprecated("Use exchange(newValue: Long) instead.", ReplaceWith("this.exchange(newValue)"), DeprecationLevel.ERROR)
    public fun getAndSet(newValue: Long): Long = this::value.getAndSetField(newValue)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    @Deprecated("Use fetchAndAdd(newValue: Long) instead.", ReplaceWith("this.fetchAndAdd(newValue)"), DeprecationLevel.ERROR)
    public fun getAndAdd(delta: Long): Long = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    @Deprecated("Use addAndFetch(newValue: Long) instead.", ReplaceWith("this.addAndFetch(newValue)"), DeprecationLevel.ERROR)
    public fun addAndGet(delta: Long): Long = this::value.getAndAddField(delta) + delta

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    @Deprecated("Use fetchAndIncrement() instead.", ReplaceWith("this.fetchAndIncrement()"), DeprecationLevel.ERROR)
    public fun getAndIncrement(): Long = this::value.getAndAddField(1L)

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    @Deprecated("Use incrementAndFetch() instead.", ReplaceWith("this.incrementAndFetch()"), DeprecationLevel.ERROR)
    public fun incrementAndGet(): Long = this::value.getAndAddField(1L) + 1L

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    @Deprecated("Use decrementAndFetch() instead.", ReplaceWith("this.decrementAndFetch()"), DeprecationLevel.ERROR)
    public fun decrementAndGet(): Long = this::value.getAndAddField(-1L) - 1L

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    @Deprecated("Use fetchAndDecrement() instead.", ReplaceWith("this.fetchAndDecrement()"), DeprecationLevel.ERROR)
    public fun getAndDecrement(): Long = this::value.getAndAddField(-1L)

    /**
     * Returns the string representation of the underlying [Long] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    @Suppress("DEPRECATION_ERROR")
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Boolean] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * @constructor Creates a new [AtomicBoolean] initialized with the specified value.
 */
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicBoolean actual constructor(@Volatile private var value: Boolean) {

    /**
     * Atomically loads the value from this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.load
     */
    public actual fun load(): Boolean = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.store
     */
    public actual fun store(newValue: Boolean): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.exchange
     */
    public actual fun exchange(newValue: Boolean): Boolean = this::value.getAndSetField(newValue)

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
    public actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean =
        this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     *
     * @sample samples.concurrent.atomics.AtomicBoolean.compareAndExchange
     */
    public actual fun compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean =
        this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Returns the string representation of the underlying [Boolean] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * An object reference that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * @constructor Creates a new [AtomicReference] initialized with the specified value.
 */
@Suppress("DEPRECATION")
@SinceKotlin("2.1")
@ExperimentalAtomicApi
public actual class AtomicReference<T> actual constructor(
    @get:Deprecated("To read the atomic value use load().", ReplaceWith("this.load()"), DeprecationLevel.ERROR)
    @set:Deprecated("To atomically set the new value use store(newValue: T).", ReplaceWith("this.store(newValue)"), DeprecationLevel.ERROR)
    @Volatile public var value: T
) {

    /**
     * Atomically loads the value from this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.load
     */
    public actual fun load(): T = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     *
     * @sample samples.concurrent.atomics.AtomicReference.store
     */
    public actual fun store(newValue: T): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] and returns the old value.
     *
     * @sample samples.concurrent.atomics.AtomicReference.exchange
     */
    public actual fun exchange(newValue: T): T = this::value.getAndSetField(newValue)

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
    public actual fun compareAndSet(expectedValue: T, newValue: T): Boolean = this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by reference.
     *
     * @sample samples.concurrent.atomics.AtomicReference.compareAndExchange
     */
    public actual fun compareAndExchange(expectedValue: T, newValue: T): T = this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    @Deprecated("Use exchange(newValue: T) instead.", ReplaceWith("this.exchange(newValue)"), DeprecationLevel.ERROR)
    public fun getAndSet(newValue: T): T = this::value.getAndSetField(newValue)

    /**
     * Returns the string representation of the underlying object.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = load().toString()
}

/**
 * A [kotlinx.cinterop.NativePtr] that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * [kotlinx.cinterop.NativePtr] is a value type, hence it is stored in [AtomicNativePtr] without boxing
 * and [compareAndSet], [compareAndExchange] operations perform comparison by value.
 *
 * @constructor Creates a new [AtomicNativePtr] initialized with the specified value.
 */
@Suppress("DEPRECATION")
@SinceKotlin("2.1")
@ExperimentalAtomicApi
@ExperimentalForeignApi
public class AtomicNativePtr(
    @get:Deprecated("To read the atomic value use load().", ReplaceWith("this.load()"), DeprecationLevel.ERROR)
    @set:Deprecated("To atomically set the new value use store(newValue: T).", ReplaceWith("this.store(newValue)"), DeprecationLevel.ERROR)
    @Volatile public var value: NativePtr
) {

    /**
     * Atomically loads the value from this [AtomicNativePtr].
     */
    public fun load(): NativePtr = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicNativePtr].
     */
    public fun store(newValue: NativePtr): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicNativePtr] and returns the old value.
     */
    public fun exchange(newValue: NativePtr): NativePtr = this::value.getAndSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicNativePtr] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expectedValue: NativePtr, newValue: NativePtr): Boolean =
            this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicNativePtr] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expectedValue: NativePtr, newValue: NativePtr): NativePtr =
            this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    @Deprecated("Use exchange(newValue: NativePtr) instead.", ReplaceWith("this.exchange(newValue)"), DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    public fun getAndSet(newValue: NativePtr): NativePtr {
        // Pointer types are allowed for atomicrmw xchg operand since LLVM 15.0,
        // after LLVM version update, it may be implemented via getAndSetField intrinsic.
        // Check: https://youtrack.jetbrains.com/issue/KT-57557
        while (true) {
            val old = value
            if (this::value.compareAndSetField(old, newValue)) {
                return old
            }
        }
    }

    /**
     * Returns the string representation of the underlying [NativePtr].
     *
     * This operation does not provide any atomicity guarantees.
     */
    @Suppress("DEPRECATION_ERROR")
    public override fun toString(): String = value.toString()
}

/**
 *
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic integer value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    val _ = fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic integer value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicInt] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic integer value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic long value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    val _ = fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic long value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicLong] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic long value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic reference was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    val _ = fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic reference was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicReference] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this atomic reference was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
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
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}

/**
 * Atomically updates the value of this [AtomicNativePtr] with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this pointer was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi @ExperimentalForeignApi
@InlineOnly
public inline fun AtomicNativePtr.update(transform: (NativePtr) -> NativePtr): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicNativePtr] with the value obtained by calling the [transform] function on the current value
 * and returns the value replaced by the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this pointer was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi @ExperimentalForeignApi
@InlineOnly
public inline fun AtomicNativePtr.fetchAndUpdate(transform: (NativePtr) -> NativePtr): NativePtr {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicNativePtr] with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when this pointer was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi @ExperimentalForeignApi
@InlineOnly
public inline fun AtomicNativePtr.updateAndFetch(transform: (NativePtr) -> NativePtr): NativePtr {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}
