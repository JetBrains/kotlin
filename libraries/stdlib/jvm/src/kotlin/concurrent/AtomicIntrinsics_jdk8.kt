/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

/**
 * Atomics and AtomicArrays from the `kotlin.concurrent` package provide a compareAndExchange method.
 * However, since this method cannot be directly delegated to its corresponding Java implementation (as it's only available in Java 9 and later),
 * the current implementation relies on the methods listed below.
 *
 * Once multi-release JARs are supported, these methods can be directly delegated to the compareAndExchange implementation in Java for JDK 9 and above.
 *
 * See KT-71376
 */

@Deprecated("Provided to implement kotlin.concurrent.AtomicInt.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicInteger.compareAndExchange_jdk8(expected: Int, newValue: Int): Int {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicLong.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicLong.compareAndExchange_jdk8(expected: Long, newValue: Long): Long {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicBoolean.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicBoolean.compareAndExchange_jdk8(expected: Boolean, newValue: Boolean): Boolean {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicReference<T>.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun <T> java.util.concurrent.atomic.AtomicReference<T>.compareAndExchange_jdk8(expected: T, newValue: T): T {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicIntArray.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicIntegerArray.compareAndExchange_jdk8(index: Int, expected: Int, newValue: Int): Int {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicLongArray.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun java.util.concurrent.atomic.AtomicLongArray.compareAndExchange_jdk8(index: Int, expected: Long, newValue: Long): Long {
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

@Deprecated("Provided to implement kotlin.concurrent.AtomicArray<T>.compareAndExchange for Java 8.", level = DeprecationLevel.HIDDEN)
@PublishedApi
internal fun <T> java.util.concurrent.atomic.AtomicReferenceArray<T>.compareAndExchange_jdk8(index: Int, expected: T, newValue: T): T {
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