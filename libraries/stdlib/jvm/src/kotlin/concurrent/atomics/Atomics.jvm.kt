/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("AtomicsKt")
@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.concurrent.atomics

/**
 * Casts the given [AtomicInt] instance to [java.util.concurrent.atomic.AtomicInteger].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicInt.asJavaAtomic(): java.util.concurrent.atomic.AtomicInteger = this as java.util.concurrent.atomic.AtomicInteger

/**
 * Casts the given [java.util.concurrent.atomic.AtomicInteger] instance to [AtomicInt].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicInteger.asKotlinAtomic(): AtomicInt = this as AtomicInt

/**
 * Casts the given [AtomicLong] instance to [java.util.concurrent.atomic.AtomicLong].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicLong.asJavaAtomic(): java.util.concurrent.atomic.AtomicLong = this as java.util.concurrent.atomic.AtomicLong

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLong] instance to [AtomicLong].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicLong.asKotlinAtomic(): AtomicLong = this as AtomicLong

/**
 * Casts the given [AtomicBoolean] instance to [java.util.concurrent.atomic.AtomicBoolean].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicBoolean.asJavaAtomic(): java.util.concurrent.atomic.AtomicBoolean = this as java.util.concurrent.atomic.AtomicBoolean

/**
 * Casts the given [java.util.concurrent.atomic.AtomicBoolean] instance to [AtomicBoolean].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicBoolean.asKotlinAtomic(): AtomicBoolean = this as AtomicBoolean

/**
 * Casts the given [AtomicReference]<T> instance to [java.util.concurrent.atomic.AtomicReference]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> AtomicReference<T>.asJavaAtomic(): java.util.concurrent.atomic.AtomicReference<T> = this as java.util.concurrent.atomic.AtomicReference<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReference]<T> instance to [AtomicReference]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> java.util.concurrent.atomic.AtomicReference<T>.asKotlinAtomic(): AtomicReference<T> = this as AtomicReference<T>

/**
 * Atomically updates the value of this [AtomicInt] with value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.update(transform: (Int) -> Int): Unit {
    fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicInt] with value obtained by calling the [transform] function on the current value
 * and returns a value replaced with the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.fetchAndUpdate(transform: (Int) -> Int): Int {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicInt] with value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicInt.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.updateAndFetch(transform: (Int) -> Int): Int {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}

/**
 * Atomically updates the value of this [AtomicLong] with value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.update(transform: (Long) -> Long): Unit {
    fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicLong] with value obtained by calling the [transform] function on the current value
 * and returns a value replaced with the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.fetchAndUpdate(transform: (Long) -> Long): Long {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicLong] with value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicLong.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.updateAndFetch(transform: (Long) -> Long): Long {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}

/**
 * Atomically updates the value of this [AtomicReference] with value obtained by calling the [transform] function on the current value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.update
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.update(transform: (T) -> T): Unit {
    fetchAndUpdate(transform)
}

/**
 * Atomically updates the value of this [AtomicReference] with value obtained by calling the [transform] function on the current value
 * and returns a value replaced with the updated one.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.fetchAndUpdate
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.fetchAndUpdate(transform: (T) -> T): T {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return old
    }
}

/**
 * Atomically updates the value of this [AtomicReference] with value obtained by calling the [transform] function on the current value
 * and returns the new value.
 *
 * [transform] may be invoked more than once to recompute a result.
 *
 * @sample samples.concurrent.atomics.AtomicReference.updateAndFetch
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.updateAndFetch(transform: (T) -> T): T {
    while (true) {
        val old = load()
        val newValue = transform(old)
        if (compareAndSet(old, newValue)) return newValue
    }
}
