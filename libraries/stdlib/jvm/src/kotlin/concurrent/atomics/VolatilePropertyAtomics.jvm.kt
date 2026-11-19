/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("AtomicsKt")
@file:Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")

package kotlin.concurrent.atomics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.AccessibleVolatilePropertyLiteral
import kotlin.internal.InlineOnly
import kotlin.reflect.KMutableProperty0

// ---- Int properties, mirroring AtomicInt ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("loadInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.load(): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("storeInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.store(newValue: Int): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("exchangeInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.exchange(newValue: Int): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndSetInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndSet(expectedValue: Int, newValue: Int): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndExchangeInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndExchange(expectedValue: Int, newValue: Int): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndAddInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndAdd(delta: Int): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("addAndFetchInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.addAndFetch(delta: Int): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndIncrementInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndIncrement(): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("incrementAndFetchInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.incrementAndFetch(): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndDecrementInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndDecrement(): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("decrementAndFetchInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.decrementAndFetch(): Int {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("plusAssignInt")
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.plusAssign(delta: Int): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("minusAssignInt")
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.minusAssign(delta: Int): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.update(transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndUpdateInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndUpdate(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateAndFetchInt")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.updateAndFetch(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}


// ---- Long properties, mirroring AtomicLong ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("loadLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.load(): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("storeLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.store(newValue: Long): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("exchangeLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.exchange(newValue: Long): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndSetLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndSet(expectedValue: Long, newValue: Long): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndExchangeLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndExchange(expectedValue: Long, newValue: Long): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndAddLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndAdd(delta: Long): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("addAndFetchLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.addAndFetch(delta: Long): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndIncrementLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndIncrement(): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("incrementAndFetchLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.incrementAndFetch(): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndDecrementLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndDecrement(): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("decrementAndFetchLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.decrementAndFetch(): Long {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("plusAssignLong")
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.plusAssign(delta: Long): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("minusAssignLong")
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.minusAssign(delta: Long): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.update(transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndUpdateLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndUpdate(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateAndFetchLong")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.updateAndFetch(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}


// ---- Boolean properties, mirroring AtomicBoolean ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("loadBoolean")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.load(): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("storeBoolean")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.store(newValue: Boolean): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("exchangeBoolean")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.exchange(newValue: Boolean): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndSetBoolean")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndExchangeBoolean")
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}


// ---- reference properties, mirroring AtomicReference ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("loadReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.load(): T {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("storeReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.store(newValue: T): Unit {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("exchangeReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.exchange(newValue: T): T {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndSetReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndSet(expectedValue: T, newValue: T): Boolean {
    throw NotImplementedError("Implementation is intrinsic")
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("compareAndExchangeReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndExchange(expectedValue: T, newValue: T): T {
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.update(transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("fetchAndUpdateReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.fetchAndUpdate(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}

@Suppress("WRONG_INVOCATION_KIND")
@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
@JvmName("updateAndFetchReference")
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.updateAndFetch(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    throw NotImplementedError("Implementation is intrinsic")
}
