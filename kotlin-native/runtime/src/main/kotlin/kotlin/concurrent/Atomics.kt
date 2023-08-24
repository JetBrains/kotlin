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
 */
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
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String = value.toString()
}

/**
 * A [Long] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 */
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
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String = value.toString()
}

/**
 * An object reference that is always updated atomically.
 */
@SinceKotlin("1.9")
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
    public fun compareAndExchange(expected: T, newValue: T): T = this::value.compareAndExchangeField(expected, newValue)

    /**
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"
}

/**
 * A [kotlinx.cinterop.NativePtr] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * [kotlinx.cinterop.NativePtr] is a value type, hence it is stored in [AtomicNativePtr] without boxing
 * and [compareAndSet], [compareAndExchange] operations perform comparison by value.
 */
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
 * Atomically gets the value of the field referenced by [this].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * This is equivalent to KMutableProperty0#get() invocation and used internally to optimize allocation of a property reference.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_FIELD)
internal external fun <T> KMutableProperty0<T>.atomicGetField(): T

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * This is equivalent to KMutableProperty0#set(value: T) invocation and used internally to optimize allocation of a property reference.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.atomicSetField(newValue: T)

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the field was not equal to the expected value.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndSetField(expectedValue: T, newValue: T): Boolean

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the field in any case.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndExchangeField(expectedValue: T, newValue: T): T

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.getAndSetField(newValue: T): T

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddField(delta: Short): Short

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddField(newValue: Int): Int

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddField(newValue: Long): Long

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddField(newValue: Byte): Byte
