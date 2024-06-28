/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.NativePtr
import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*

/**
 * An [Int] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@Deprecated("Use kotlin.concurrent.AtomicInt instead.", ReplaceWith("kotlin.concurrent.AtomicInt"), DeprecationLevel.ERROR)
public class AtomicInt(public @Volatile var value: Int) {
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: Int): Int = this::value.getAndSetField(newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     */
    public fun compareAndSet(expected: Int, newValue: Int): Boolean = this::value.compareAndSetField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     */
    public fun compareAndSwap(expected: Int, newValue: Int): Int = this::value.compareAndExchangeField(expected, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    public fun getAndAdd(delta: Int): Int = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    public fun addAndGet(delta: Int): Int = this::value.getAndAddField(delta) + delta

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    public fun getAndIncrement(): Int = this::value.getAndAddField(1)

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    public fun incrementAndGet(): Int = this::value.getAndAddField(1) + 1

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    public fun decrementAndGet(): Int = this::value.getAndAddField(-1) - 1

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    public fun getAndDecrement(): Int = this::value.getAndAddField(-1)

    /**
     * Atomically increments the current value by one.
     */
    @Deprecated("Use incrementAndGet() or getAndIncrement() instead.", ReplaceWith("this.incrementAndGet()"), DeprecationLevel.ERROR)
    public fun increment(): Unit {
        addAndGet(1)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated("Use decrementAndGet() or getAndDecrement() instead.", ReplaceWith("this.decrementAndGet()"), DeprecationLevel.ERROR)
    public fun decrement(): Unit {
        addAndGet(-1)
    }

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()
}

/**
 * A [Long] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@Deprecated("Use kotlin.concurrent.AtomicLong instead.", ReplaceWith("kotlin.concurrent.AtomicLong"), DeprecationLevel.ERROR)
public class AtomicLong(public @Volatile var value: Long = 0L)  {
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: Long): Long = this::value.getAndSetField(newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     */
    public fun compareAndSet(expected: Long, newValue: Long): Boolean = this::value.compareAndSetField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     */
    public fun compareAndSwap(expected: Long, newValue: Long): Long = this::value.compareAndExchangeField(expected, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the old value.
     */
    public fun getAndAdd(delta: Long): Long = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    public fun addAndGet(delta: Long): Long = this::value.getAndAddField(delta) + delta

    /**
     * Atomically increments the current value by one and returns the old value.
     */
    public fun getAndIncrement(): Long = this::value.getAndAddField(1L)

    /**
     * Atomically increments the current value by one and returns the new value.
     */
    public fun incrementAndGet(): Long = this::value.getAndAddField(1L) + 1L

    /**
     * Atomically decrements the current value by one and returns the new value.
     */
    public fun decrementAndGet(): Long = this::value.getAndAddField(-1L) - 1L

    /**
     * Atomically decrements the current value by one and returns the old value.
     */
    public fun getAndDecrement(): Long = this::value.getAndAddField(-1L)

    /**
     * Atomically adds the [given value][delta] to the current value and returns the new value.
     */
    @Deprecated(message = "Use addAndGet(delta: Long) instead.", level = DeprecationLevel.ERROR)
    public fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Atomically increments the current value by one.
     */
    @Deprecated("Use incrementAndGet() or getAndIncrement() instead.", ReplaceWith("this.incrementAndGet()"), DeprecationLevel.ERROR)
    public fun increment(): Unit {
        addAndGet(1L)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated("Use decrementAndGet() or getAndDecrement() instead.", ReplaceWith("this.decrementAndGet()"), DeprecationLevel.ERROR)
    public fun decrement(): Unit {
        addAndGet(-1L)
    }

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()
}

/**
 * An object reference that is always updated atomically.
 */
@Deprecated("Use kotlin.concurrent.AtomicReference instead.", ReplaceWith("kotlin.concurrent.AtomicReference"), DeprecationLevel.ERROR)
public class AtomicReference<T>(public @Volatile var value: T) {
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: T): T = this::value.getAndSetField(newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndSet(expected: T, newValue: T): Boolean = this::value.compareAndSetField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndSwap(expected: T, newValue: T): T = this::value.compareAndExchangeField(expected, newValue)

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"
}

/**
 * A [kotlinx.cinterop.NativePtr] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * [kotlinx.cinterop.NativePtr] is a value type, hence it is stored in [AtomicNativePtr] without boxing
 * and [compareAndSet], [compareAndSwap] operations perform comparison by value.
 */
@Deprecated("Use kotlin.concurrent.AtomicNativePtr instead.", ReplaceWith("kotlin.concurrent.AtomicNativePtr"), DeprecationLevel.ERROR)
public class AtomicNativePtr(public @Volatile var value: NativePtr) {
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
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
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expected: NativePtr, newValue: NativePtr): Boolean =
            this::value.compareAndSetField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSwap(expected: NativePtr, newValue: NativePtr): NativePtr =
            this::value.compareAndExchangeField(expected, newValue)

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()
}


private fun idString(value: Any) = "${value.hashCode().toUInt().toString(16)}"

private fun debugString(value: Any?): String {
    if (value == null) return "null"
    return "${value::class.qualifiedName}: ${idString(value)}"
}

/**
 * This class was useful only with legacy memory manager. Please use [AtomicReference] instead.
 */
@FreezingIsDeprecated
@Deprecated("Use kotlin.concurrent.AtomicReference instead.", ReplaceWith("kotlin.concurrent.AtomicReference"), DeprecationLevel.ERROR)
public class FreezableAtomicReference<T>(public @Volatile var value: T) {

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     *
     * @param expected the expected value
     * @param newValue the new value
     * @return the old value
     */
     public fun compareAndSwap(expected: T, newValue: T): T = this::value.compareAndExchangeField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns true if operation was successful.
     *
     * Note that comparison is identity-based, not value-based.
     *
     * @param expected the expected value
     * @param newValue the new value
     * @return true if successful
     */
    public fun compareAndSet(expected: T, newValue: T): Boolean = this::value.compareAndSetField(expected, newValue)

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

}
