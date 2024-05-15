/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * Classes which inherit from this interface have a defined total ordering between their instances.
 */
@ActualizeByJvmBuiltinProvider
public expect interface Comparable<in T> {
    /**
     * Compares this object with the specified object for order. Returns zero if this object is equal
     * to the specified [other] object, a negative number if it's less than [other], or a positive number
     * if it's greater than [other].
     */
    public operator fun compareTo(other: T): Int
}
