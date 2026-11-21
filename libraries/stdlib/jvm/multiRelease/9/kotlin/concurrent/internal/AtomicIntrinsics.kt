/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")
package kotlin.concurrent.internal

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Atomics and AtomicArrays from the `kotlin.concurrent` package provide a compareAndExchange method.
 *
 * Starting from Java 9, it's possible to delegate directly to its corresponding Java implementation.
 */

@SinceKotlin("2.1")
@PublishedApi
internal fun AtomicInteger.compareAndExchange(expected: Int, newValue: Int): Int = this.compareAndExchange(expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun AtomicLong.compareAndExchange(expected: Long, newValue: Long): Long = this.compareAndExchange(expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun AtomicBoolean.compareAndExchange(expected: Boolean, newValue: Boolean): Boolean = this.compareAndExchange(expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun <T> AtomicReference<T>.compareAndExchange(expected: T, newValue: T): T = this.compareAndExchange(expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun AtomicIntegerArray.compareAndExchange(index: Int, expected: Int, newValue: Int): Int = this.compareAndExchange(index, expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun AtomicLongArray.compareAndExchange(index: Int, expected: Long, newValue: Long): Long = this.compareAndExchange(index, expected, newValue)

@SinceKotlin("2.1")
@PublishedApi
internal fun <T> AtomicReferenceArray<T>.compareAndExchange(index: Int, expected: T, newValue: T): T = this.compareAndExchange(index, expected, newValue)
