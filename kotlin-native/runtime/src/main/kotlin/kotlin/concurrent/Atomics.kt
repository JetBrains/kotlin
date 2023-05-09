/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

import kotlinx.cinterop.NativePtr
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*

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
@SinceKotlin("1.9")
public class AtomicInt(@Volatile public var value: Int) {
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
    public fun compareAndExchange(expected: Int, newValue: Int): Int = this::value.compareAndExchangeField(expected, newValue)

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
    @Deprecated(level = DeprecationLevel.ERROR, message = "Use incrementAndGet() or getAndIncrement() instead.",
            replaceWith = ReplaceWith("this.incrementAndGet()"))
    public fun increment(): Unit {
        addAndGet(1)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated(level = DeprecationLevel.ERROR, message = "Use decrementAndGet() or getAndDecrement() instead.",
            replaceWith = ReplaceWith("this.decrementAndGet()"))
    public fun decrement(): Unit {
        addAndGet(-1)
    }

    /**
     * Returns the string representation of the current [value].
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
@SinceKotlin("1.9")
public class AtomicLong(@Volatile public var value: Long)  {
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
    public fun compareAndExchange(expected: Long, newValue: Long): Long = this::value.compareAndExchangeField(expected, newValue)

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
    @Deprecated(level = DeprecationLevel.ERROR, message = "Use addAndGet(delta: Long) instead.")
    public fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Atomically increments the current value by one.
     */
    @Deprecated(level = DeprecationLevel.ERROR, message = "Use incrementAndGet() or getAndIncrement() instead.",
            replaceWith = ReplaceWith("this.incrementAndGet()"))
    public fun increment(): Unit {
        addAndGet(1L)
    }

    /**
     * Atomically decrements the current value by one.
     */
    @Deprecated(level = DeprecationLevel.ERROR, message = "Use decrementAndGet() or getAndDecrement() instead.",
            replaceWith = ReplaceWith("this.decrementAndGet()"))
    public fun decrement(): Unit {
        addAndGet(-1L)
    }

    /**
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String = value.toString()
}

/**
 * An object reference that is always updated atomically.
 *
 * Legacy MM: An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * but frequently shall be of nullable type and be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.internal.GC.detectCycles]
 * in debug mode could be helpful.
 */
@FrozenLegacyMM
@LeakDetectorCandidate
@NoReorderFields
@OptIn(FreezingIsDeprecated::class)
@SinceKotlin("1.9")
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
    external public fun compareAndExchange(expected: T, newValue: T): T

    /**
     * Returns the string representation of the current [value].
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
 * and [compareAndSet], [compareAndExchange] operations perform comparison by value.
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicNativePtr] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
@SinceKotlin("1.9")
@ExperimentalForeignApi
public class AtomicNativePtr(@Volatile public var value: NativePtr) {
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
    public fun compareAndExchange(expected: NativePtr, newValue: NativePtr): NativePtr =
            this::value.compareAndExchangeField(expected, newValue)

    /**
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String = value.toString()
}


private fun idString(value: Any) = "${value.hashCode().toUInt().toString(16)}"

private fun debugString(value: Any?): String {
    if (value == null) return "null"
    return "${value::class.qualifiedName}: ${idString(value)}"
}

/**
 * Compares the value of the field referenced by [this] to [expectedValue], and if they are equal,
 * atomically replaces it with [newValue].
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Returns true if the actual field value matched [expectedValue]
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndSetField(expectedValue: T, newValue: T): Boolean

/**
 * Compares the value of the field referenced by [this] to [expectedValue], and if they are equal,
 * atomically replaces it with [newValue].
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Returns the field value before operation.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndExchangeField(expectedValue: T, newValue: T): T

/**
 * Atomically sets value of the field referenced by [this] to [newValue] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.getAndSetField(newValue: T): T


/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddField(delta: Short): Short

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddField(newValue: Int): Int

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddField(newValue: Long): Long

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddField(newValue: Byte): Byte
