/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CharsKt")

package kotlin.text

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
@OptIn(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public inline fun Char.toLowerCase(): Char = lowercaseChar()

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [lowercase] function.
 * If this character has no mapping equivalent, the character itself is returned.
 *
 * @sample samples.text.Chars.lowercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public expect fun Char.lowercaseChar(): Char

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the length of the returned string can be greater than one.
 * For example, `'\u0130'.lowercase()` returns `"\u0069\u0307"`,
 * where `'\u0130'` is the LATIN CAPITAL LETTER I WITH DOT ABOVE character (`İ`).
 * If this character has no lower case mapping, the result of `toString()` of this char is returned.
 *
 * @sample samples.text.Chars.lowercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public expect fun Char.lowercase(): String

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 */
@OptIn(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public inline fun Char.toUpperCase(): Char = uppercaseChar()

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [uppercase] function.
 * If this character has no mapping equivalent, the character itself is returned.
 *
 * @sample samples.text.Chars.uppercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public expect fun Char.uppercaseChar(): Char

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the length of the returned string can be greater than one.
 * For example, `'\uFB00'.uppercase()` returns `"\u0046\u0046"`,
 * where `'\uFB00'` is the LATIN SMALL LIGATURE FF character (`ﬀ`).
 * If this character has no upper case mapping, the result of `toString()` of this char is returned.
 *
 * @sample samples.text.Chars.uppercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public expect fun Char.uppercase(): String

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
 *   - Applying the method [uppercaseChar] to each character produces the same result
 *   - Applying the method [lowercaseChar] to each character produces the same result
 *
 * @sample samples.text.Chars.equals
 */
@OptIn(ExperimentalStdlibApi::class)
public fun Char.equals(other: Char, ignoreCase: Boolean = false): Boolean {
    if (this == other) return true
    if (!ignoreCase) return false

    if (this.uppercaseChar() == other.uppercaseChar()) return true
    if (this.lowercaseChar() == other.lowercaseChar()) return true
    return false
}

/**
 * Returns `true` if this character is a Unicode surrogate code unit.
 */
public fun Char.isSurrogate(): Boolean = this in Char.MIN_SURROGATE..Char.MAX_SURROGATE
