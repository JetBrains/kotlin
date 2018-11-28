/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CharsKt")

package kotlin.text

/**
 * Concatenates this Char and a String.
 *
 * @sample samples.text.Chars.plus
 */
@kotlin.internal.InlineOnly
public inline operator fun Char.plus(other: String): String = this.toString() + other

/**
 * Returns `true` if this character is equal to the [other] character, optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 *
 * Two characters are considered the same ignoring case if at least one of the following is `true`:
 *   - The two characters are the same (as compared by the == operator)
 *   - Applying the method [toUpperCase] to each character produces the same result
 *   - Applying the method [toLowerCase] to each character produces the same result
 *
 * @sample samples.text.Chars.equals
 */
public fun Char.equals(other: Char, ignoreCase: Boolean = false): Boolean {
    if (this == other) return true
    if (!ignoreCase) return false

    if (this.toUpperCase() == other.toUpperCase()) return true
    if (this.toLowerCase() == other.toLowerCase()) return true
    return false
}

/**
 * Returns `true` if this character is a Unicode surrogate code unit.
 */
public fun Char.isSurrogate(): Boolean = this in Char.MIN_SURROGATE..Char.MAX_SURROGATE
