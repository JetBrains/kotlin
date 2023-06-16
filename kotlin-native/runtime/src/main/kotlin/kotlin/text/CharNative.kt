/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.experimental.ExperimentalNativeApi
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
 * Returns `true` if this character is an ISO control character.
 *
 * A character is considered to be an ISO control character if its [category] is [CharCategory.CONTROL],
 * meaning the Char is in the range `'\u0000'..'\u001F'` or in the range `'\u007F'..'\u009F'`.
 *
 * @sample samples.text.Chars.isISOControl
 */
@GCUnsafeCall("Kotlin_Char_isISOControl")
external public actual fun Char.isISOControl(): Boolean

/**
 * Converts a surrogate pair to a unicode code point. Doesn't validate that the characters are a valid surrogate pair.
 *
 * Note that this function is unstable.
 * In the future it could be deprecated in favour of an overload that would return a `CodePoint` type.
 */
@ExperimentalNativeApi
// TODO: Consider removing from public API
public fun Char.Companion.toCodePoint(high: Char, low: Char): Int =
        (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/**
 * Checks if the codepoint specified is a supplementary codepoint or not.
 *
 * Note that this function is unstable.
 * In the future it could be deprecated in favour of an overload that would accept a `CodePoint` type.
 */
@ExperimentalNativeApi
// TODO: Consider removing from public API
public fun Char.Companion.isSupplementaryCodePoint(codepoint: Int): Boolean =
        codepoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT

/**
 * Checks if the specified [high] and [low] chars are [Char.isHighSurrogate] and [Char.isLowSurrogate] correspondingly.
 */
@ExperimentalNativeApi
// TODO: Consider removing from public API
public fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean = high.isHighSurrogate() && low.isLowSurrogate()

/**
 * Converts the codepoint specified to a char array. If the codepoint is not supplementary, the method will
 * return an array with one element otherwise it will return an array A with a high surrogate in A[0] and
 * a low surrogate in A[1].
 *
 *
 * Note that this function is unstable.
 * In the future it could be deprecated in favour of an overload that would accept a `CodePoint` type.
 */
@ExperimentalNativeApi
// TODO: Consider removing from public API
@Suppress("DEPRECATION")
public fun Char.Companion.toChars(codePoint: Int): CharArray =
        when {
            codePoint in 0 until MIN_SUPPLEMENTARY_CODE_POINT -> charArrayOf(codePoint.toChar())
            codePoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT -> {
                val low = ((codePoint - 0x10000) and 0x3FF) + MIN_LOW_SURROGATE.toInt()
                val high = (((codePoint - 0x10000) ushr 10) and 0x3FF) + MIN_HIGH_SURROGATE.toInt()
                charArrayOf(high.toChar(), low.toChar())
            }
            else -> throw IllegalArgumentException()
        }

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