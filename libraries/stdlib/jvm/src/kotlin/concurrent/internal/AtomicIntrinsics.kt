/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NEWER_VERSION_IN_SINCE_KOTLIN")
package kotlin.concurrent.internal

/**
 * Atomics and AtomicArrays from the `kotlin.concurrent` package provide a compareAndExchange method.
 * However, since this method cannot be directly delegated to its corresponding Java implementation (as it's only available in Java 9 and later),
 * the current implementation relies on the methods listed below.
 *
 * Once multi-release JARs are supported, these methods can be directly delegated to the compareAndExchange implementation in Java for JDK 9 and above.
 *
 * See KT-71376
 */

@SinceKotlin("2.2")
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicInteger.compareAndExchange(expected: Int, newValue: Int): Int {
    while(true) {
        val currentValue = get()
        if (expected == currentValue) {
            if (compareAndSet(expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicLong.compareAndExchange(expected: Long, newValue: Long): Long {
    while(true) {
        val currentValue = get()
        if (expected == currentValue) {
            if (compareAndSet(expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicBoolean.compareAndExchange(expected: Boolean, newValue: Boolean): Boolean {
    while(true) {
        val currentValue = get()
        if (expected == currentValue) {
            if (compareAndSet(expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun <T> java.util.concurrent.atomic.AtomicReference<T>.compareAndExchange(expected: T, newValue: T): T {
    while(true) {
        val currentValue = get()
        if (expected === currentValue) {
            if (compareAndSet(expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicIntegerArray.compareAndExchange(index: Int, expected: Int, newValue: Int): Int {
    while(true) {
        val currentValue = get(index)
        if (expected == currentValue) {
            if (compareAndSet(index, expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicLongArray.compareAndExchange(index: Int, expected: Long, newValue: Long): Long {
    while(true) {
        val currentValue = get(index)
        if (expected == currentValue) {
            if (compareAndSet(index, expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}

@SinceKotlin("2.2")
@PublishedApi
internal fun <T> java.util.concurrent.atomic.AtomicReferenceArray<T>.compareAndExchange(index: Int, expected: T, newValue: T): T {
    while(true) {
        val currentValue = get(index)
        if (expected === currentValue) {
            if (compareAndSet(index, expected, newValue)) {
                return expected
            }
        } else {
            return currentValue
        }
    }
}