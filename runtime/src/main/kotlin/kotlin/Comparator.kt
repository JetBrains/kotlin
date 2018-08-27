/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

actual public interface Comparator<T> {
    actual fun compare(a: T, b: T): Int
}

actual public inline fun <T> Comparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T> {
    return object: Comparator<T> {
        override fun compare(a: T, b: T) = comparison(a, b)
    }
}