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
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicInt] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
@Deprecated("Use kotlin.concurrent.AtomicInt instead.", ReplaceWith("kotlin.concurrent.AtomicInt"))
@DeprecatedSinceKotlin(warningSince = "1.9")
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
    @Deprecated("Use incrementAndGet() or getAndIncrement() instead.", ReplaceWith("this.incrementAndGet()"))
    public fun increment(): Unit {
        addAndGet(1)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated("Use decrementAndGet() or getAndDecrement() instead.", ReplaceWith("this.decrementAndGet()"))
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
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicLong] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
@Deprecated("Use kotlin.concurrent.AtomicLong instead.", ReplaceWith("kotlin.concurrent.AtomicLong"))
@DeprecatedSinceKotlin(warningSince = "1.9")
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
    @Deprecated("Use addAndGet(delta: Long) instead.")
    public fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Atomically increments the current value by one.
     */
    @Deprecated("Use incrementAndGet() or getAndIncrement() instead.", ReplaceWith("this.incrementAndGet()"))
    public fun increment(): Unit {
        addAndGet(1L)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated("Use decrementAndGet() or getAndDecrement() instead.", ReplaceWith("this.decrementAndGet()"))
    fun decrement(): Unit {
        addAndGet(-1L)
    }

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = value.toString()
}

/**
 * An object reference that is always updated atomically.
 *
 * Legacy MM: An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * but frequently shall be of nullable type and be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.runtime.GC.detectCycles]
 * in debug mode could be helpful.
 */
@FrozenLegacyMM
@LeakDetectorCandidate
@NoReorderFields
@OptIn(FreezingIsDeprecated::class)
@Deprecated("Use kotlin.concurrent.AtomicReference instead.", ReplaceWith("kotlin.concurrent.AtomicReference"))
@DeprecatedSinceKotlin(warningSince = "1.9")
public class AtomicReference<T> {
    private var value_: T

    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    // Optimization for speeding up access.
    private var cookie: Int = 0

    /**
     * Creates a new atomic reference pointing to the [given value][value].
     *
     * @throws InvalidMutabilityException with legacy MM if reference is not frozen.
     */
    constructor(value: T) {
        if (this.isFrozen) {
            checkIfFrozen(value)
        }
        value_ = value
    }

    /**
     * The current value.
     * Gets the current value or sets to the given [new value][newValue].
     *
     * Legacy MM: if the [new value][newValue] value is not null, it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(newValue) = setImpl(newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: T): T {
        while (true) {
            val old = value
            if (old === newValue) {
                return old
            }
            if (compareAndSet(old, newValue)) {
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
     * Comparison of values is done by reference.
     */
    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSet")
    external public fun compareAndSet(expected: T, newValue: T): Boolean

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     *
     * Legacy MM: if the [new value][newValue] value is not null, it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     */
    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSwap")
    external public fun compareAndSwap(expected: T, newValue: T): T

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

    // Implementation details.
    @GCUnsafeCall("Kotlin_AtomicReference_set")
    private external fun setImpl(newValue: Any?): Unit

    @GCUnsafeCall("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?
}

/**
 * A [kotlinx.cinterop.NativePtr] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * [kotlinx.cinterop.NativePtr] is a value type, hence it is stored in [AtomicNativePtr] without boxing
 * and [compareAndSet], [compareAndSwap] operations perform comparison by value.
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicNativePtr] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
@Deprecated("Use kotlin.concurrent.AtomicNativePtr instead.", ReplaceWith("kotlin.concurrent.AtomicNativePtr"))
@DeprecatedSinceKotlin(warningSince = "1.9")
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
 * Note: this class is useful only with legacy memory manager. Please use [AtomicReference] instead.
 *
 * An atomic reference to a Kotlin object. Can be used in concurrent scenarious, but must be frozen first,
 * otherwise behaves as regular box for the value. If frozen, shall be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.runtime.GC.detectCycles]
 * in debug mode could be helpful.
 */
@NoReorderFields
@LeakDetectorCandidate
@ExportTypeInfo("theFreezableAtomicReferenceTypeInfo")
@FreezingIsDeprecated
@Deprecated("Use kotlin.concurrent.AtomicReference instead.", ReplaceWith("kotlin.concurrent.AtomicReference"))
@DeprecatedSinceKotlin(warningSince = "1.9")
public class FreezableAtomicReference<T>(private var value_: T) {
    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    // Optimization for speeding up access.
    private var cookie: Int = 0

    /**
     * The referenced value.
     * Gets the value or sets to the given [new value][newValue]. If the [new value][newValue] is not null,
     * and `this` is frozen - it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException if the value is not frozen or a permanent object
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(newValue) {
            if (this.isShareable())
                setImpl(newValue)
            else
                value_ = newValue
        }

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Legacy MM: If the [new value][newValue] value is not null and object is frozen, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param newValue the new value
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     * @return the old value
     */
     public fun compareAndSwap(expected: T, newValue: T): T {
        return if (this.isShareable()) {
            @Suppress("UNCHECKED_CAST")(compareAndSwapImpl(expected, newValue) as T)
        } else {
            val old = value_
            if (old === expected) value_ = newValue
            old
        }
    }

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
    public fun compareAndSet(expected: T, newValue: T): Boolean {
        if (this.isShareable())
            return compareAndSetImpl(expected, newValue)
        val old = value_
        if (old === expected) {
            value_ = newValue
            return true
        } else {
            return false
        }
    }

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

    // TODO: Consider making this public.
    internal fun swap(newValue: T): T {
        while (true) {
            val old = value
            if (old === newValue) {
                return old
            }
            if (compareAndSet(old, newValue)) {
                return old
            }
        }
    }

    // Implementation details.
    @GCUnsafeCall("Kotlin_AtomicReference_set")
    private external fun setImpl(newValue: Any?): Unit

    @GCUnsafeCall("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?

    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSwap")
    private external fun compareAndSwapImpl(expected: Any?, newValue: Any?): Any?

    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSet")
    private external fun compareAndSetImpl(expected: Any?, newValue: Any?): Boolean
}
