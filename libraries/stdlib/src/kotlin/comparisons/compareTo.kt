/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly

/**
 * Compares this object with the specified object for order. Returns zero if this object is equal
 * to the specified [other] object, a negative number if it's less than [other], or a positive number
 * if it's greater than [other].
 *
 * This function delegates to [Comparable.compareTo] and allows to call it in infix form.
 */
@InlineOnly
@SinceKotlin("1.6")
public inline infix fun <T> Comparable<T>.compareTo(other: T): Int =
    this.compareTo(other)
