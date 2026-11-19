/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")

package kotlin.concurrent.atomics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.AccessibleVolatilePropertyLiteral
import kotlin.internal.InlineOnly
import kotlin.reflect.KMutableProperty0

// Atomic operations on volatile properties ("unboxed atomics").
//
// Each function below is an intrinsic: it may only be called on a property reference literal (e.g. `this::counter`)
// referencing an accessible `var` property whose backing field is annotated with [kotlin.concurrent.Volatile].
// Top-level properties and properties of objects (including companion objects) are not supported: they are allocated once,
// so the matching `Atomic*` class can be used for them instead.
// The compiler replaces such calls with an atomic operation on the backing field, no `KProperty` or atomic box is allocated.
// The set of operations mirrors the API of the matching `Atomic*` class.

// ---- Int properties, mirroring AtomicInt ----

/**
 * Atomically stores the [new value][newValue] into the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.exchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.exchange(newValue: Int): Int

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue],
 * returns true if the operation was successful and false only if the current value was not equal to the expected value.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.compareAndSet
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndSet(expectedValue: Int, newValue: Int): Boolean

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue]
 * and returns the old value in any case.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.compareAndExchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndExchange(expectedValue: Int, newValue: Int): Int

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.fetchAndAdd
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndAdd(delta: Int): Int

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.addAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.addAndFetch(delta: Int): Int

/**
 * Atomically increments the current value of the referenced volatile property by one and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.fetchAndIncrement
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndIncrement(): Int

/**
 * Atomically increments the current value of the referenced volatile property by one and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.incrementAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.incrementAndFetch(): Int

/**
 * Atomically decrements the current value of the referenced volatile property by one and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.fetchAndDecrement
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndDecrement(): Int

/**
 * Atomically decrements the current value of the referenced volatile property by one and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.decrementAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.decrementAndFetch(): Int

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.plusAssign
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.plusAssign(delta: Int): Unit

/**
 * Atomically subtracts the [given value][delta] from the current value of the referenced volatile property.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.minusAssign
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.minusAssign(delta: Int): Unit

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.update
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.update(transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the old value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.fetchAndUpdate
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndUpdate(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicInt.updateAndFetch
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.updateAndFetch(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}


// ---- Long properties, mirroring AtomicLong ----

/**
 * Atomically stores the [new value][newValue] into the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.exchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.exchange(newValue: Long): Long

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue],
 * returns true if the operation was successful and false only if the current value was not equal to the expected value.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.compareAndSet
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndSet(expectedValue: Long, newValue: Long): Boolean

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue]
 * and returns the old value in any case.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.compareAndExchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndExchange(expectedValue: Long, newValue: Long): Long

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.fetchAndAdd
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndAdd(delta: Long): Long

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.addAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.addAndFetch(delta: Long): Long

/**
 * Atomically increments the current value of the referenced volatile property by one and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.fetchAndIncrement
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndIncrement(): Long

/**
 * Atomically increments the current value of the referenced volatile property by one and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.incrementAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.incrementAndFetch(): Long

/**
 * Atomically decrements the current value of the referenced volatile property by one and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.fetchAndDecrement
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndDecrement(): Long

/**
 * Atomically decrements the current value of the referenced volatile property by one and returns the new value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.decrementAndFetch
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.decrementAndFetch(): Long

/**
 * Atomically adds the [given value][delta] to the current value of the referenced volatile property.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.plusAssign
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.plusAssign(delta: Long): Unit

/**
 * Atomically subtracts the [given value][delta] from the current value of the referenced volatile property.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.minusAssign
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.minusAssign(delta: Long): Unit

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.update
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.update(transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the old value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.fetchAndUpdate
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndUpdate(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicLong.updateAndFetch
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.updateAndFetch(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}


// ---- Boolean properties, mirroring AtomicBoolean ----

/**
 * Atomically stores the [new value][newValue] into the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicBoolean.exchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.exchange(newValue: Boolean): Boolean

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue],
 * returns true if the operation was successful and false only if the current value was not equal to the expected value.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicBoolean.compareAndSet
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue]
 * and returns the old value in any case.
 *
 * Comparison of values is done by value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicBoolean.compareAndExchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean


// ---- reference properties, mirroring AtomicReference ----

/**
 * Atomically stores the [new value][newValue] into the referenced volatile property and returns the old value.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.exchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.exchange(newValue: T): T

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue],
 * returns true if the operation was successful and false only if the current value was not equal to the expected value.
 *
 * Comparison of values is done by reference.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.compareAndSet
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndSet(expectedValue: T, newValue: T): Boolean

/**
 * Atomically stores the given [new value][newValue] into the referenced volatile property if its current value equals the [expected value][expectedValue]
 * and returns the old value in any case.
 *
 * Comparison of values is done by reference.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.compareAndExchange
 */
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndExchange(expectedValue: T, newValue: T): T

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.update
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.update(transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the old value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.fetchAndUpdate
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.fetchAndUpdate(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}

/**
 * Atomically updates the value of the referenced volatile property with the value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example, when the property value was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * This function can only be called on a property reference literal (e.g. `this::foo`) of a `var` property
 * whose backing field is annotated with [kotlin.concurrent.Volatile], and is accessible at the call site.
 * Top-level properties and properties of objects are not supported: use the matching `Atomic*` class for them.
 *
 * @see kotlin.concurrent.atomics.AtomicReference.updateAndFetch
 */
@Suppress("EXPECTED_DECLARATION_WITH_BODY", "WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public expect inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.updateAndFetch(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    error("Unreachable")
}
