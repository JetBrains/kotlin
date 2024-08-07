/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
public expect class AtomicIntArray public constructor(size: Int) {
    public constructor(array: IntArray)

    public val size: Int

    public fun loadAt(index: Int): Int

    public fun storeAt(index: Int, newValue: Int)

    public fun exchangeAt(index: Int, newValue: Int): Int

    public fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean

    public fun fetchAndAddAt(index: Int, delta: Int): Int

    public fun addAndFetchAt(index: Int, delta: Int): Int

    public fun fetchAndIncrementAt(index: Int): Int

    public fun incrementAndFetchAt(index: Int): Int

    public fun fetchAndDecrementAt(index: Int): Int

    public fun decrementAndFetchAt(index: Int): Int

    public override fun toString(): String
}

@ActualizeByJvmBuiltinProvider
public expect class AtomicLongArray public constructor(size: Int) {
    public constructor(array: LongArray)

    public val size: Int

    public fun loadAt(index: Int): Long

    public fun storeAt(index: Int, newValue: Long)

    public fun exchangeAt(index: Int, newValue: Long): Long

    public fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean

    public fun fetchAndAddAt(index: Int, delta: Long): Long

    public fun addAndFetchAt(index: Int, delta: Long): Long

    public fun fetchAndIncrementAt(index: Int): Long

    public fun incrementAndFetchAt(index: Int): Long

    public fun fetchAndDecrementAt(index: Int): Long

    public fun decrementAndFetchAt(index: Int): Long

    public override fun toString(): String
}

@ActualizeByJvmBuiltinProvider
public expect class AtomicArray<T> {
    public constructor(array: Array<T>)

    public val size: Int

    public fun loadAt(index: Int): T

    public fun storeAt(index: Int, newValue: T)

    public fun exchangeAt(index: Int, newValue: T): T

    public fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean

    public override fun toString(): String
}