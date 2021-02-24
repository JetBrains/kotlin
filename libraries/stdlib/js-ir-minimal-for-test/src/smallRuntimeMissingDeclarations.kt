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

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public fun Long.toString(radix: Int): String =
    this.toStringImpl(checkRadix(radix))

/**
 * Checks whether the given [radix] is valid radix for string to number and number to string conversion.
 */
internal fun checkRadix(radix: Int): Int {
    if (radix !in 2..36) {
        throw IllegalArgumentException("radix $radix was not in valid range 2..36")
    }
    return radix
}