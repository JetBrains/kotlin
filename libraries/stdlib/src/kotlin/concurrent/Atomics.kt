/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

public expect class AtomicInt {
    public fun load(): Int

    public fun store(newValue: Int)

    public fun exchange(newValue: Int): Int

    public fun compareAndSet(expected: Int, newValue: Int): Boolean

    public fun compareAndExchange(expected: Int, newValue: Int): Int

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
public operator fun AtomicInt.plusAssign(delta: Int): Unit { this.addAndFetch(delta) }

/**
 * Atomically subtracts the [given value][delta] from the current value.
 */
public operator fun AtomicInt.minusAssign(delta: Int): Unit { this.addAndFetch(-delta) }