/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.native.internal.GCUnsafeCall

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
@GCUnsafeCall("Kotlin_Char_isHighSurrogate")
external public actual fun Char.isHighSurrogate(): Boolean

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
@GCUnsafeCall("Kotlin_Char_isLowSurrogate")
external public actual fun Char.isLowSurrogate(): Boolean

/**
 * Returns `true` if this character (Unicode code point) should be regarded as an ignorable
 * character in a Java identifier or a Unicode identifier.
 */
@GCUnsafeCall("Kotlin_Char_isIdentifierIgnorable")
external public fun Char.isIdentifierIgnorable(): Boolean

/**
 * Returns `true` if this character is an ISO control character.
 *
 * A character is considered to be an ISO control character if its [category] is [CharCategory.CONTROL],
 * meaning the Char is in the range `'\u0000'..'\u001F'` or in the range `'\u007F'..'\u009F'`.
 *
 * @sample samples.text.Chars.isISOControl
 */
@GCUnsafeCall("Kotlin_Char_isISOControl")
external public actual fun Char.isISOControl(): Boolean

@SharedImmutable
private val digits = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        -1, -1, -1, -1, -1, -1, -1,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35,
        -1, -1, -1, -1, -1, -1,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35
)

internal actual fun digitOf(char: Char, radix: Int): Int = when {
    char >= '0' && char <= 'z' -> digits[char - '0']
    char < '\u0080' -> -1
    char >= '\uFF21' && char <= '\uFF3A' -> char - '\uFF21' + 10 // full-width latin capital letter
    char >= '\uFF41' && char <= '\uFF5A' -> char - '\uFF41' + 10 // full-width latin small letter
    else -> char.digitToIntImpl()
}.let { if (it >= radix) -1 else it }