/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Returns `true` if this char sequence is empty (contains no characters).
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isEmpty(): Boolean = length == 0

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <T, R> Array<out T>.fold(initial: R, operation: (acc: R, T) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}


public actual fun Throwable.stackTraceToString(): String = toString()

public actual fun Throwable.printStackTrace() {
    TODO("Not implemented in reduced runtime")
}

public actual fun Throwable.addSuppressed(exception: Throwable) {
    TODO("Not implemented in reduced runtime")
}

public actual val Throwable.suppressedExceptions: List<Throwable>
    get() = TODO("Not implemented in reduced runtime")