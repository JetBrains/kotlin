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

// Single-threaded platform: plain reads and writes of the property are atomic.

// ---- Int properties, mirroring AtomicInt ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.exchange(newValue: Int): Int {
    val old = get()
    set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndSet(expectedValue: Int, newValue: Int): Boolean {
    if (get() != expectedValue) return false
    set(newValue)
    return true
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndExchange(expectedValue: Int, newValue: Int): Int {
    val old = get()
    if (old == expectedValue) set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndAdd(delta: Int): Int {
    val old = get()
    set(old + delta)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.addAndFetch(delta: Int): Int {
    val new = get() + delta
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndIncrement(): Int {
    val old = get()
    set(old + 1)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.incrementAndFetch(): Int {
    val new = get() + 1
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndDecrement(): Int {
    val old = get()
    set(old + -1)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.decrementAndFetch(): Int {
    val new = get() + -1
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.plusAssign(delta: Int): Unit {
    set(get() + delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.minusAssign(delta: Int): Unit {
    set(get() - delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.update(transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    set(transform(get()))
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndUpdate(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = get()
    set(transform(old))
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.updateAndFetch(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val new = transform(get())
    set(new)
    return new
}


// ---- Long properties, mirroring AtomicLong ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.exchange(newValue: Long): Long {
    val old = get()
    set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndSet(expectedValue: Long, newValue: Long): Boolean {
    if (get() != expectedValue) return false
    set(newValue)
    return true
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndExchange(expectedValue: Long, newValue: Long): Long {
    val old = get()
    if (old == expectedValue) set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndAdd(delta: Long): Long {
    val old = get()
    set(old + delta)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.addAndFetch(delta: Long): Long {
    val new = get() + delta
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndIncrement(): Long {
    val old = get()
    set(old + 1L)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.incrementAndFetch(): Long {
    val new = get() + 1L
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndDecrement(): Long {
    val old = get()
    set(old + -1L)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.decrementAndFetch(): Long {
    val new = get() + -1L
    set(new)
    return new
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.plusAssign(delta: Long): Unit {
    set(get() + delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.minusAssign(delta: Long): Unit {
    set(get() - delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.update(transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    set(transform(get()))
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndUpdate(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = get()
    set(transform(old))
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.updateAndFetch(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val new = transform(get())
    set(new)
    return new
}


// ---- Boolean properties, mirroring AtomicBoolean ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.exchange(newValue: Boolean): Boolean {
    val old = get()
    set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean {
    if (get() != expectedValue) return false
    set(newValue)
    return true
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean {
    val old = get()
    if (old == expectedValue) set(newValue)
    return old
}


// ---- reference properties, mirroring AtomicReference ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.exchange(newValue: T): T {
    val old = get()
    set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndSet(expectedValue: T, newValue: T): Boolean {
    if (get() !== expectedValue) return false
    set(newValue)
    return true
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndExchange(expectedValue: T, newValue: T): T {
    val old = get()
    if (old === expectedValue) set(newValue)
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.update(transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    set(transform(get()))
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.fetchAndUpdate(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val old = get()
    set(transform(old))
    return old
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.updateAndFetch(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    val new = transform(get())
    set(new)
    return new
}
