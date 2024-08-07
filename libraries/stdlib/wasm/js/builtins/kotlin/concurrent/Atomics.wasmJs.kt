/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

public actual class AtomicInt {
    public actual fun load(): Int {
        TODO("Not yet implemented")
    }

    public actual fun store(newValue: Int) {
    }

    public actual fun exchange(newValue: Int): Int {
        TODO("Not yet implemented")
    }

    public actual fun compareAndSet(expected: Int, newValue: Int): Boolean {
        TODO("Not yet implemented")
    }

    public actual fun compareAndExchange(expected: Int, newValue: Int): Int {
        TODO("Not yet implemented")
    }

    public actual fun fetchAndAdd(delta: Int): Int {
        TODO("Not yet implemented")
    }

    public actual fun addAndFetch(delta: Int): Int {
        TODO("Not yet implemented")
    }

    public actual fun fetchAndIncrement(): Int {
        TODO("Not yet implemented")
    }

    public actual fun incrementAndFetch(): Int {
        TODO("Not yet implemented")
    }

    public actual fun decrementAndFetch(): Int {
        TODO("Not yet implemented")
    }

    public actual fun fetchAndDecrement(): Int {
        TODO("Not yet implemented")
    }

    public actual override fun toString(): String {
        TODO("Not yet implemented")
    }

}