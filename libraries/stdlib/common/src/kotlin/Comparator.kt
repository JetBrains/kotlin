/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Provides a comparison function for imposing a total ordering between instances of the type [T].
 */
public expect fun interface Comparator<T> {
    /**
     * Compares its two arguments for order. Returns zero if the arguments are equal,
     * a negative number if the first argument is less than the second, or a positive number
     * if the first argument is greater than the second.
     */
    public fun compare(a: T, b: T): Int
}