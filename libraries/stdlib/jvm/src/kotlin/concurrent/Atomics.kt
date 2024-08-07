/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.concurrent

/**
 * An [Int] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
public actual class AtomicInt(@Volatile private var value: Int) {

    public actual fun load(): Int { TODO() }

    public actual fun store(newValue: Int) { TODO() }

    public actual fun exchange(newValue: Int): Int { TODO() }

    public actual fun compareAndSet(expected: Int, newValue: Int): Boolean { TODO() }

    public actual fun compareAndExchange(expected: Int, newValue: Int): Int { TODO() }

    public actual fun fetchAndAdd(delta: Int): Int { TODO() }

    public actual fun addAndFetch(delta: Int): Int { TODO() }

    public actual fun fetchAndIncrement(): Int { TODO() }

    public actual fun incrementAndFetch(): Int { TODO() }

    public actual fun decrementAndFetch(): Int { TODO() }

    public actual fun fetchAndDecrement(): Int { TODO() }

    public actual override fun toString(): String { TODO() }
}