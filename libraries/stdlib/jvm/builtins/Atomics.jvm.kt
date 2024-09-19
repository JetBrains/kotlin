/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

/**
 * An [Int] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
public actual class AtomicInt(value: Int) {

    public actual fun load(): Int

    public actual fun store(value: Int)

    public actual fun exchange(newValue: Int): Int

    public actual fun compareAndSet(expectedValue: Int, newValue: Int): Boolean

    public actual fun compareAndExchange(expectedValue: Int, newValue: Int): Int = TODO()

    public actual fun fetchAndAdd(delta: Int): Int

    public actual fun addAndFetch(delta: Int): Int

    public actual fun fetchAndIncrement(): Int

    public actual fun incrementAndFetch(): Int

    public actual fun decrementAndFetch(): Int

    public actual fun fetchAndDecrement(): Int

    public actual override fun toString(): String
}

public actual class AtomicLong(value: Long) {

    public actual fun load(): Long

    public actual fun store(value: Long)

    public actual fun exchange(newValue: Long): Long

    public actual fun compareAndSet(expectedValue: Long, newValue: Long): Boolean

    public actual fun compareAndExchange(expectedValue: Long, newValue: Long): Long

    public actual fun fetchAndAdd(delta: Long): Long

    public actual fun addAndFetch(delta: Long): Long

    public actual fun fetchAndIncrement(): Long

    public actual fun incrementAndFetch(): Long

    public actual fun decrementAndFetch(): Long

    public actual fun fetchAndDecrement(): Long

    public actual override fun toString(): String
}

public actual class AtomicBoolean (value: Boolean) {
    public actual fun load(): Boolean

    public actual fun store(newValue: Boolean)

    public actual fun exchange(newValue: Boolean): Boolean

    public actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean

    public actual fun compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean

    public actual override fun toString(): String
}

public actual class AtomicReference<T> (value: T) {
    public actual fun load(): T

    public actual fun store(newValue: T)

    public actual fun exchange(newValue: T): T

    public actual fun compareAndSet(expectedValue: T, newValue: T): Boolean

    public actual fun compareAndExchange(expectedValue: T, newValue: T): T

    public actual override fun toString(): String
}