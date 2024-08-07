/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicInt public constructor(value: Int) {

    public fun load(): Int

    public fun store(newValue: Int)

    public fun exchange(newValue: Int): Int

    public fun compareAndSet(expected: Int, newValue: Int): Boolean

    public fun fetchAndAdd(delta: Int): Int

    public fun addAndFetch(delta: Int): Int

    public fun fetchAndIncrement(): Int

    public fun incrementAndFetch(): Int

    public fun decrementAndFetch(): Int

    public fun fetchAndDecrement(): Int

    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicInt.plusAssign(delta: Int): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicInt.minusAssign(delta: Int): Unit { this.addAndFetch(-delta) }

@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicLong public constructor(value: Long) {
    public fun load(): Long

    public fun store(newValue: Long)

    public fun exchange(newValue: Long): Long

    public fun compareAndSet(expected: Long, newValue: Long): Boolean

    public fun fetchAndAdd(delta: Long): Long

    public fun addAndFetch(delta: Long): Long

    public fun fetchAndIncrement(): Long

    public fun incrementAndFetch(): Long

    public fun decrementAndFetch(): Long

    public fun fetchAndDecrement(): Long

    public override fun toString(): String
}

/**
 * Atomically adds the [given value][delta] to the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicLong.plusAssign(delta: Long): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value.
 */
@ExperimentalStdlibApi
public operator fun AtomicLong.minusAssign(delta: Long): Unit { this.addAndFetch(-delta) }

@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicBoolean public constructor(value: Boolean) {
    public fun load(): Boolean

    public fun store(newValue: Boolean)

    public fun exchange(newValue: Boolean): Boolean

    public fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean

    public override fun toString(): String
}

@ActualizeByJvmBuiltinProvider
@ExperimentalStdlibApi
public expect class AtomicReference<T> public constructor(value: T) {
    public fun load(): T

    public fun store(newValue: T)

    public fun exchange(newValue: T): T

    public fun compareAndSet(expected: T, newValue: T): Boolean

    public override fun toString(): String
}

