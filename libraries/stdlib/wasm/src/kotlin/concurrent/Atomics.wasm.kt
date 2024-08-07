/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

@Suppress("DEPRECATION")
public actual class AtomicInt public actual constructor(
    @Deprecated("To read the atomic value use load(). To atomically set the new value use store(newValue: T).")
    public var value: Int
) {
    public actual fun load(): Int = value

    public actual fun store(newValue: Int) { value = newValue }

    public actual fun exchange(newValue: Int): Int {
        val oldValue = value
        value = newValue
        return oldValue
    }

    public actual fun compareAndSet(expected: Int, newValue: Int): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    public actual fun fetchAndAdd(delta: Int): Int {
        val oldValue = value
        value += delta
        return oldValue
    }

    public actual fun addAndFetch(delta: Int): Int {
        value += delta
        return value
    }

    public actual fun fetchAndIncrement(): Int = value++

    public actual fun incrementAndFetch(): Int = ++value

    public actual fun fetchAndDecrement(): Int = value--

    public actual fun decrementAndFetch(): Int = --value

    public actual override fun toString(): String = value.toString()
}

@Suppress("DEPRECATION")
public actual class AtomicLong public actual constructor(
    @Deprecated("To read the atomic value use load(). To atomically set the new value use store(newValue: T).")
    public var value: Long
) {
    public actual fun load(): Long = value

    public actual fun store(newValue: Long) { value = newValue }

    public actual fun exchange(newValue: Long): Long {
        val oldValue = value
        value = newValue
        return oldValue
    }

    public actual fun compareAndSet(expected: Long, newValue: Long): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    public actual fun fetchAndAdd(delta: Long): Long {
        val oldValue = value
        value += delta
        return oldValue
    }

    public actual fun addAndFetch(delta: Long): Long {
        value += delta
        return value
    }

    public actual fun fetchAndIncrement(): Long = value++

    public actual fun incrementAndFetch(): Long = ++value

    public actual fun fetchAndDecrement(): Long = value--

    public actual fun decrementAndFetch(): Long = --value

    public actual override fun toString(): String = value.toString()
}

public actual class AtomicBoolean public actual constructor(private var value: Boolean) {
    public actual fun load(): Boolean = value

    public actual fun store(newValue: Boolean) { value = newValue }

    public actual fun exchange(newValue: Boolean): Boolean {
        val oldValue = value
        value = newValue
        return oldValue
    }

    public actual fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    public fun compareAndExchange(expected: Boolean, newValue: Boolean): Boolean {
        val oldValue = value
        if (value == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the current [value].
     */
    public actual override fun toString(): String = value.toString()
}

@Suppress("DEPRECATION")
public actual class AtomicReference<T> public actual constructor(
    @Deprecated("To read the atomic value use load(). To atomically set the new value use store(newValue: T).")
    public var value: T
) {

    public actual fun load(): T = value

    public actual fun store(newValue: T) { value = newValue }

    public actual fun exchange(newValue: T): T {
        val oldValue = value
        value = newValue
        return oldValue
    }

    public actual fun compareAndSet(expected: T, newValue: T): Boolean {
        if (value != expected) return false
        value = newValue
        return true
    }

    public fun compareAndExchange(expected: T, newValue: T): T {
        val oldValue = value
        if (value == expected) {
            value = newValue
        }
        return oldValue
    }

    /**
     * Returns the string representation of the current [value].
     */
    public actual override fun toString(): String = value.toString()
}