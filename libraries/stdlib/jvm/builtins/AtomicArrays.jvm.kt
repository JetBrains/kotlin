/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

public actual class AtomicIntArray public actual constructor(public actual val size: Int) {
    public actual constructor(array: IntArray)

    public actual fun loadAt(index: Int): Int

    public actual fun storeAt(index: Int, newValue: Int)

    public actual fun exchangeAt(index: Int, newValue: Int): Int

    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Int, newValue: Int): Int

    public actual fun fetchAndAddAt(index: Int, delta: Int): Int

    public actual fun addAndFetchAt(index: Int, delta: Int): Int

    public actual fun fetchAndIncrementAt(index: Int): Int

    public actual fun incrementAndFetchAt(index: Int): Int

    public actual fun fetchAndDecrementAt(index: Int): Int

    public actual fun decrementAndFetchAt(index: Int): Int

    public actual override fun toString(): String
}

public actual class AtomicLongArray public actual constructor(public actual val size: Int) {
    public actual constructor(array: LongArray)

    public actual fun loadAt(index: Int): Long

    public actual fun storeAt(index: Int, newValue: Long)

    public actual fun exchangeAt(index: Int, newValue: Long): Long

    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean

    public actual fun compareAndExchangeAt(index: Int, expectedValue: Long, newValue: Long): Long

    public actual fun fetchAndAddAt(index: Int, delta: Long): Long

    public actual fun addAndFetchAt(index: Int, delta: Long): Long

    public actual fun fetchAndIncrementAt(index: Int): Long

    public actual fun incrementAndFetchAt(index: Int): Long

    public actual fun fetchAndDecrementAt(index: Int): Long

    public actual fun decrementAndFetchAt(index: Int): Long

    public actual override fun toString(): String
}

public actual class AtomicArray<T> {
    public actual constructor (array: Array<T>)

    public actual val size: Int

    public actual fun loadAt(index: Int): T

    public actual fun storeAt(index: Int, newValue: T)

    public actual fun exchangeAt(index: Int, newValue: T): T

    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean

    public actual fun compareAndExchangeAt(index: Int, expectedValue: T, newValue: T): T

    public actual override fun toString(): String
}