/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns `true` if this character is an ISO control character.
 *
 * A character is considered to be an ISO control character if its [category] is [CharCategory.CONTROL],
 * meaning the Char is in the range `'\u0000'..'\u001F'` or in the range `'\u007F'..'\u009F'`.
 *
 * @sample samples.text.Chars.isISOControl
 */
@SinceKotlin("1.5")
public actual fun Char.isISOControl(): Boolean {
    return this <= '\u001F' || this in '\u007F'..'\u009F'
}

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
public actual fun Char.isHighSurrogate(): Boolean = this in Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
public actual fun Char.isLowSurrogate(): Boolean = this in Char.MIN_LOW_SURROGATE..Char.MAX_LOW_SURROGATE

/** Converts a surrogate pair to a unicode code point. Doesn't validate that the characters are a valid surrogate pair. */
internal actual fun Char.Companion.toCodePoint(high: Char, low: Char): Int =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/** Checks if the codepoint specified is a supplementary codepoint or not. */
internal actual fun Char.Companion.isSupplementaryCodePoint(codepoint: Int): Boolean =
    codepoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT

internal actual fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean = high.isHighSurrogate() && low.isLowSurrogate()

/**
 * Converts the codepoint specified to a char array. If the codepoint is not supplementary, the method will
 * return an array with one element otherwise it will return an array A with a high surrogate in A[0] and
 * a low surrogate in A[1].
 */
@Suppress("DEPRECATION")
internal actual fun Char.Companion.toChars(codePoint: Int): CharArray =
    when {
        codePoint in 0 until MIN_SUPPLEMENTARY_CODE_POINT -> charArrayOf(codePoint.toChar())
        codePoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT -> {
            val low = ((codePoint - 0x10000) and 0x3FF) + MIN_LOW_SURROGATE.toInt()
            val high = (((codePoint - 0x10000) ushr 10) and 0x3FF) + MIN_HIGH_SURROGATE.toInt()
            charArrayOf(high.toChar(), low.toChar())
        }
        else -> throw IllegalArgumentException()
    }

internal actual fun digitOf(char: Char, radix: Int): Int = when {
    char >= '0' && char <= '9' -> char - '0'
    char >= 'A' && char <= 'Z' -> char - 'A' + 10
    char >= 'a' && char <= 'z' -> char - 'a' + 10
    char < '\u0080' -> -1
    char >= '\uFF21' && char <= '\uFF3A' -> char - '\uFF21' + 10 // full-width latin capital letter
    char >= '\uFF41' && char <= '\uFF5A' -> char - '\uFF41' + 10 // full-width latin small letter
    else -> char.digitToIntImpl()
}.let { if (it >= radix) -1 else it }
