/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")

package kotlin.concurrent.atomics

import kotlin.concurrent.atomicGetField
import kotlin.concurrent.compareAndExchangeField
import kotlin.concurrent.compareAndSetField
import kotlin.concurrent.getAndAddField
import kotlin.concurrent.getAndSetField
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.AccessibleVolatilePropertyLiteral
import kotlin.internal.InlineOnly
import kotlin.reflect.KMutableProperty0

// ---- Int properties, mirroring AtomicInt ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.exchange(newValue: Int): Int {
    return this.getAndSetField(newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndSet(expectedValue: Int, newValue: Int): Boolean {
    return this.compareAndSetField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.compareAndExchange(expectedValue: Int, newValue: Int): Int {
    return this.compareAndExchangeField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndAdd(delta: Int): Int {
    return this.getAndAddField(delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.addAndFetch(delta: Int): Int {
    return this.getAndAddField(delta) + delta
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndIncrement(): Int {
    return this.getAndAddField(1)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.incrementAndFetch(): Int {
    return this.getAndAddField(1) + 1
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndDecrement(): Int {
    return this.getAndAddField(-1)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.decrementAndFetch(): Int {
    return this.getAndAddField(-1) + -1
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.plusAssign(delta: Int): Unit {
    val _ = this.getAndAddField(delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.minusAssign(delta: Int): Unit {
    val _ = this.getAndAddField(-delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.update(transform: (Int) -> Int): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.fetchAndUpdate(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return old
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Int>.updateAndFetch(transform: (Int) -> Int): Int {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        val new = transform(old)
        if (this.compareAndSetField(old, new)) return new
    }
}


// ---- Long properties, mirroring AtomicLong ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.exchange(newValue: Long): Long {
    return this.getAndSetField(newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndSet(expectedValue: Long, newValue: Long): Boolean {
    return this.compareAndSetField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.compareAndExchange(expectedValue: Long, newValue: Long): Long {
    return this.compareAndExchangeField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndAdd(delta: Long): Long {
    return this.getAndAddField(delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.addAndFetch(delta: Long): Long {
    return this.getAndAddField(delta) + delta
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndIncrement(): Long {
    return this.getAndAddField(1L)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.incrementAndFetch(): Long {
    return this.getAndAddField(1L) + 1L
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndDecrement(): Long {
    return this.getAndAddField(-1L)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.decrementAndFetch(): Long {
    return this.getAndAddField(-1L) + -1L
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.plusAssign(delta: Long): Unit {
    val _ = this.getAndAddField(delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline operator fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.minusAssign(delta: Long): Unit {
    val _ = this.getAndAddField(-delta)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.update(transform: (Long) -> Long): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.fetchAndUpdate(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return old
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Long>.updateAndFetch(transform: (Long) -> Long): Long {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        val new = transform(old)
        if (this.compareAndSetField(old, new)) return new
    }
}


// ---- Boolean properties, mirroring AtomicBoolean ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.exchange(newValue: Boolean): Boolean {
    return this.getAndSetField(newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean {
    return this.compareAndSetField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<Boolean>.compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean {
    return this.compareAndExchangeField(expectedValue, newValue)
}


// ---- reference properties, mirroring AtomicReference ----

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.exchange(newValue: T): T {
    return this.getAndSetField(newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndSet(expectedValue: T, newValue: T): Boolean {
    return this.compareAndSetField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.compareAndExchange(expectedValue: T, newValue: T): T {
    return this.compareAndExchangeField(expectedValue, newValue)
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.update(transform: (T) -> T): Unit {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.fetchAndUpdate(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        if (this.compareAndSetField(old, transform(old))) return old
    }
}

@SinceKotlin("2.5")
@ExperimentalAtomicApi
@InlineOnly
public actual inline fun <T> @receiver:AccessibleVolatilePropertyLiteral KMutableProperty0<T>.updateAndFetch(transform: (T) -> T): T {
    contract {
        callsInPlace(transform, InvocationKind.AT_LEAST_ONCE)
    }
    while (true) {
        val old = this.atomicGetField()
        val new = transform(old)
        if (this.compareAndSetField(old, new)) return new
    }
}
