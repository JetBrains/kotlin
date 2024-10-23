/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.concurrent

import java.util.concurrent.atomic.*

/**
 * An [Int] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
public actual class AtomicInt public actual constructor(value: Int) {

    public actual fun load(): Int { TODO()}

    public actual fun store(newValue: Int) { TODO() }

    public actual fun exchange(newValue: Int): Int = TODO()

    public actual fun compareAndSet(expectedValue: Int, newValue: Int): Boolean = TODO()

    public actual fun compareAndExchange(expected: Int, newValue: Int): Int = TODO()

    public actual fun fetchAndAdd(delta: Int): Int = TODO()

    public actual fun addAndFetch(delta: Int): Int = TODO()

    public actual fun fetchAndIncrement(): Int = TODO()

    public actual fun incrementAndFetch(): Int = TODO()

    public actual fun decrementAndFetch(): Int = TODO()

    public actual fun fetchAndDecrement(): Int = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicInt] instance to [java.util.concurrent.atomic.AtomicInteger].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicInt.asJavaAtomic(): AtomicInteger = this as AtomicInteger

/**
 * Casts the given [java.util.concurrent.atomic.AtomicInteger] instance to [AtomicInt].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicInteger.asKotlinAtomic(): AtomicInt = this as AtomicInt

public actual class AtomicLong public actual constructor(value: Long) {

    public actual fun load(): Long { TODO()}

    public actual fun store(newValue: Long) { TODO() }

    public actual fun exchange(newValue: Long): Long = TODO()

    public actual fun compareAndSet(expected: Long, newValue: Long): Boolean = TODO()

    public actual fun compareAndExchange(expected: Long, newValue: Long): Long = TODO()

    public actual fun fetchAndAdd(delta: Long): Long = TODO()

    public actual fun addAndFetch(delta: Long): Long = TODO()

    public actual fun fetchAndIncrement(): Long = TODO()

    public actual fun incrementAndFetch(): Long = TODO()

    public actual fun decrementAndFetch(): Long = TODO()

    public actual fun fetchAndDecrement(): Long = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicLong] instance to [java.util.concurrent.atomic.AtomicLong].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicLong.asJavaAtomic(): java.util.concurrent.atomic.AtomicLong = this as java.util.concurrent.atomic.AtomicLong

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLong] instance to [AtomicLong].
 */
@Suppress("UNCHECKED_CAST")
public fun java.util.concurrent.atomic.AtomicLong.asKotlinAtomic(): AtomicLong = this as AtomicLong

public actual class AtomicBoolean public actual constructor(value: Boolean) {

    public actual fun load(): Boolean { TODO()}

    public actual fun store(newValue: Boolean) { TODO() }

    public actual fun exchange(newValue: Boolean): Boolean = TODO()

    public actual fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean = TODO()

    public actual fun compareAndExchange(expected: Boolean, newValue: Boolean): Boolean = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicBoolean] instance to [java.util.concurrent.atomic.AtomicBoolean].
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicBoolean.asJavaAtomic(): java.util.concurrent.atomic.AtomicBoolean = this as java.util.concurrent.atomic.AtomicBoolean

/**
 * Casts the given [java.util.concurrent.atomic.AtomicBoolean] instance to [AtomicBoolean].
 */
@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicBoolean.asKotlinAtomic(): AtomicBoolean = this as AtomicBoolean

public actual class AtomicReference<T> public actual constructor(value: T) {

    public actual fun load(): T { TODO()}

    public actual fun store(newValue: T) { TODO() }

    public actual fun exchange(newValue: T): T = TODO()

    public actual fun compareAndSet(expected: T, newValue: T): Boolean = TODO()

    public actual fun compareAndExchange(expected: T, newValue: T): T = TODO()

    public actual override fun toString(): String = TODO()
}

/**
 * Casts the given [AtomicReference]<T> instance to [java.util.concurrent.atomic.AtomicReference]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicReference<T>.asJavaAtomic(): java.util.concurrent.atomic.AtomicReference<T> = this as java.util.concurrent.atomic.AtomicReference<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReference]<T> instance to [AtomicReference]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicReference<T>.asKotlinAtomic(): AtomicReference<T> = this as AtomicReference<T>