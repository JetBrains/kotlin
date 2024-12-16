/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

// Only for compatibility with shared K/N stdlib code

internal actual class AtomicReference<T> actual constructor(public actual var value: T) {
    public actual fun compareAndExchange(expected: T, newValue: T): T {
        if (value == expected) {
            val old = value
            value = newValue
            return old
        }
        return value
    }
    public actual fun compareAndSet(expected: T, newValue: T): Boolean {
        if (value == expected) {
            value = newValue
            return true
        }
        return false
    }
}
