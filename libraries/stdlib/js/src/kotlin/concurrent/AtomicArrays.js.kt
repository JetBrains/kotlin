/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

public actual class AtomicIntArray {
    private val array: IntArray

    public actual constructor(size: Int) {
        array = IntArray(size)
    }

    public actual constructor(array: IntArray) {
        this.array = IntArray(array.size)
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): Int = array[index]

    public actual fun storeAt(index: Int, newValue: Int) { array[index] = newValue }

    public actual fun exchangeAt(index: Int, newValue: Int): Int {
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: Int, newValue: Int): Boolean {
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual fun fetchAndAddAt(index: Int, delta: Int): Int {
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    public actual fun addAndFetchAt(index: Int, delta: Int): Int {
        array[index] += delta
        return array[index]
    }

    public actual fun fetchAndIncrementAt(index: Int): Int = array[index]++

    public actual fun incrementAndFetchAt(index: Int): Int = ++array[index]

    public actual fun fetchAndDecrementAt(index: Int): Int = array[index]--

    public actual fun decrementAndFetchAt(index: Int): Int = --array[index]

    public actual override fun toString(): String = array.toString()
}

public actual class AtomicLongArray {
    private val array: LongArray

    public actual constructor(size: Int) {
        array = LongArray(size)
    }

    public actual constructor(array: LongArray) {
        this.array = LongArray(array.size)
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): Long = array[index]

    public actual fun storeAt(index: Int, newValue: Long) { array[index] = newValue }

    public actual fun exchangeAt(index: Int, newValue: Long): Long {
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: Long, newValue: Long): Boolean {
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual fun fetchAndAddAt(index: Int, delta: Long): Long {
        val oldValue = array[index]
        array[index] += delta
        return oldValue
    }

    public actual fun addAndFetchAt(index: Int, delta: Long): Long {
        array[index] += delta
        return array[index]
    }

    public actual fun fetchAndIncrementAt(index: Int): Long = array[index]++

    public actual fun incrementAndFetchAt(index: Int): Long = ++array[index]

    public actual fun fetchAndDecrementAt(index: Int): Long = array[index]--

    public actual fun decrementAndFetchAt(index: Int): Long = --array[index]

    public actual override fun toString(): String = array.toString()
}

public actual class AtomicArray<T> {
    private val array: Array<T>

    public actual constructor (array: Array<T>) {
        this.array = array.copyOf()
    }

    public actual val size: Int get() = array.size

    public actual fun loadAt(index: Int): T = array[index]

    public actual fun storeAt(index: Int, newValue: T) { array[index] = newValue }

    public actual fun exchangeAt(index: Int, newValue: T): T {
        val oldValue = array[index]
        array[index] = newValue
        return oldValue
    }

    public actual fun compareAndSetAt(index: Int, expectedValue: T, newValue: T): Boolean {
        if (array[index] != expectedValue) return false
        array[index] = newValue
        return true
    }

    public actual override fun toString(): String = array.toString()
}
