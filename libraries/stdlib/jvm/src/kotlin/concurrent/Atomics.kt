/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:Suppress("UNUSED_PARAMETER")

package kotlin.concurrent

/**
 * An [Int] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
public actual class AtomicInt(@Volatile private var value: Int) {

    @Deprecated("This method is deprecated, use load() instead.", level = DeprecationLevel.WARNING)
    public fun get(): Int { TODO() }

    public actual fun load(): Int { TODO() }

    @Deprecated("This method is deprecated, use store(newValue) instead.", level = DeprecationLevel.WARNING)
    public fun set(newValue: Int) { TODO() }

    public actual fun store(newValue: Int) { TODO() }

    @Deprecated("This method is deprecated, use exchange(newValue) instead.", level = DeprecationLevel.WARNING)
    public fun getAndSet(newValue: Int): Int { TODO() }

    public actual fun exchange(newValue: Int): Int { TODO() }

    public actual fun compareAndSet(expected: Int, newValue: Int): Boolean { TODO() }

    public actual fun compareAndExchange(expected: Int, newValue: Int): Int { TODO() }

    @Deprecated("This method is deprecated, use fetchAndAdd(newValue) instead.", level = DeprecationLevel.WARNING)
    public fun getAndAdd(delta: Int): Int { TODO() }

    public actual fun fetchAndAdd(delta: Int): Int { TODO() }

    @Deprecated("This method is deprecated, use addAndFetch(newValue) instead.", level = DeprecationLevel.WARNING)
    public fun addAndGet(delta: Int): Int { TODO() }

    public actual fun addAndFetch(delta: Int): Int { TODO() }

    @Deprecated("This method is deprecated, use fetchAndIncrement() instead.", level = DeprecationLevel.WARNING)
    public fun getAndIncrement(): Int { TODO() }

    public actual fun fetchAndIncrement(): Int { TODO() }

    @Deprecated("This method is deprecated, use incrementAndFetch() instead.", level = DeprecationLevel.WARNING)
    public fun incrementAndGet(): Int { TODO() }

    public actual fun incrementAndFetch(): Int { TODO() }

    @Deprecated("This method is deprecated, use decrementAndFetch() instead.", level = DeprecationLevel.WARNING)
    public fun decrementAndGet(): Int { TODO() }

    public actual fun decrementAndFetch(): Int { TODO() }

    @Deprecated("This method is deprecated, use fetchAndDecrement() instead.", level = DeprecationLevel.WARNING)
    public fun getAndDecrement(): Int { TODO() }

    public actual fun fetchAndDecrement(): Int { TODO() }

    public actual override fun toString(): String { TODO() }
}