/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.concurrent

// Only for compatibility with shared K/N stdlib code

internal val Any?.isFrozen
    inline get() = false

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> T.freeze(): T = this

internal class AtomicReference<T>(public var value: T) {
    public fun compareAndSwap(expected: T, new: T): T {
        if (value == expected) {
            val old = value
            value = new
            return old
        }
        return value
    }
    public fun compareAndSet(expected: T, new: T): Boolean {
        if (value == expected) {
            value = new
            return true
        }
        return false
    }
}
