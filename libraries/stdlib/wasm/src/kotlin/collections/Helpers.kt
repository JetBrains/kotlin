/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Returns an array of objects of the given type with the given [size], initialized with _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
internal inline fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    require(size >= 0) { "capacity must be non-negative." }
    @Suppress("TYPE_PARAMETER_AS_REIFIED")
    return Array<E>(size)
}

internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    @Suppress("UNCHECKED_CAST")
    this.fill(null as E, fromIndex, toIndex)
}

internal fun <E> Array<E>.resetAt(index: Int) {
    @Suppress("UNCHECKED_CAST")
    (this as Array<Any?>)[index] = null
}