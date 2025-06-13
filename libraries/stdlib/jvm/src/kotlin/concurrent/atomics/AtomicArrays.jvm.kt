/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("AtomicArraysKt")
@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.concurrent.atomics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly

/**
 * Casts the given [AtomicIntArray] instance to [java.util.concurrent.atomic.AtomicIntegerArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicIntArray.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicIntegerArray = this as java.util.concurrent.atomic.AtomicIntegerArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicIntegerArray] instance to [AtomicIntArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicIntegerArray.asKotlinAtomicArray(): AtomicIntArray = this as AtomicIntArray

/**
 * Casts the given [AtomicLongArray] instance to [java.util.concurrent.atomic.AtomicLongArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicLongArray.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicLongArray = this as java.util.concurrent.atomic.AtomicLongArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLongArray] instance to [AtomicLongArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicLongArray.asKotlinAtomicArray(): AtomicLongArray = this as AtomicLongArray

/**
 * Casts the given [AtomicArray]<T> instance to [java.util.concurrent.atomic.AtomicReferenceArray]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> AtomicArray<T>.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicReferenceArray<T> = this as java.util.concurrent.atomic.AtomicReferenceArray<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReferenceArray]<T> instance to [AtomicArray]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> java.util.concurrent.atomic.AtomicReferenceArray<T>.asKotlinAtomicArray(): AtomicArray<T> = this as AtomicArray<T>

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.updateAt(index: Int, transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.updateAndFetchAt(index: Int, transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicIntArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when an integer value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicIntArray.fetchAndUpdateAt(index: Int, transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.updateAt(index: Int, transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.updateAndFetchAt(index: Int, transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a long value at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun AtomicLongArray.fetchAndUpdateAt(index: Int, transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.updateAt(index: Int, transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val _ = updateAndFetchAt(index, transform)
}

/**
 * Atomically updates the element of this [AtomicArray] at the given index using the [transform] function and returns
 * the updated value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.updateAndFetchAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.updateAndFetchAt(index: Int, transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return new
    }
}

/**
 * Atomically updates the element of this [AtomicLongArray] at the given index using the [transform] function and returns
 * the old value of the element.
 *
 * [transform] may be invoked more than once to recompute a result.
 * That may happen, for example,
 * when a reference at the specified index was concurrently updated while [transform] was applied,
 * or due to a spurious compare-and-set failure.
 * The latter is implementation-specific, and it should not be relied upon.
 *
 * It's recommended to keep [transform] fast and free of side effects.
 *
 * @throws IndexOutOfBoundsException if the [index] is out of bounds of this array.
 *
 * @sample samples.concurrent.atomics.AtomicArray.fetchAndUpdateAt
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> AtomicArray<T>.fetchAndUpdateAt(index: Int, transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = loadAt(index)
        val new = transform(old)
        if (compareAndSetAt(index, old, new)) return old
    }
}
