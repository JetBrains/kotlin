/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.IllegalArgumentException

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 */
@Deprecated("Use uppercaseChar() instead.", ReplaceWith("uppercaseChar()"))
@DeprecatedSinceKotlin(warningSince = "1.5", errorSince = "2.1")
public actual fun Char.toUpperCase(): Char = uppercaseCharImpl()

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [uppercase] function.
 * If this character has no mapping equivalent, the character itself is returned.
 *
 * @sample samples.text.Chars.uppercase
 */
@SinceKotlin("1.5")
public actual fun Char.uppercaseChar(): Char = uppercaseCharImpl()

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
@SinceKotlin("1.5")
public actual fun Char.uppercase(): String = uppercaseImpl()

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
@Deprecated("Use lowercaseChar() instead.", ReplaceWith("lowercaseChar()"))
@DeprecatedSinceKotlin(warningSince = "1.5", errorSince = "2.1")
public actual fun Char.toLowerCase(): Char = lowercaseCharImpl()

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [lowercase] function.
 * If this character has no mapping equivalent, the character itself is returned.
 *
 * @sample samples.text.Chars.lowercase
 */
@SinceKotlin("1.5")
public actual fun Char.lowercaseChar(): Char = lowercaseCharImpl()

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
@SinceKotlin("1.5")
public actual fun Char.lowercase(): String = lowercaseImpl()

/**
 * Converts this character to title case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [titlecase] function.
 * If this character has no mapping equivalent, the result of calling [uppercaseChar] is returned.
 *
 * @sample samples.text.Chars.titlecase
 */
@SinceKotlin("1.5")
public actual fun Char.titlecaseChar(): Char = titlecaseCharImpl()

/**
 * Checks whether the given [radix] is valid radix for string to number and number to string conversion.
 */
@IgnorableReturnValue
@PublishedApi
@Suppress("DEPRECATION")
internal actual fun checkRadix(radix: Int): Int {
    if(radix !in Char_MIN_RADIX..Char_MAX_RADIX) {
        throw IllegalArgumentException("radix $radix was not in valid range ${Char_MIN_RADIX..Char_MAX_RADIX}")
    }
    return radix
}

// Char.Compaion methods. Konan specific.
internal const val Char_MIN_RADIX: Int = 2
internal const val Char_MAX_RADIX: Int = 36
// TODO: Make public when supplementary codepoints are supported.
internal const val Char_MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

/** Converts a unicode code point to lower case. */
internal fun Char.Companion.toLowerCase(codePoint: Int): Int =
    if (codePoint <= MAX_VALUE.code) {
        @Suppress("DEPRECATION")
        codePoint.toChar().lowercaseChar().toInt()
    } else {
        codePoint // TODO: Implement this transformation for supplementary codepoints.
    }

/** Converts a unicode code point to upper case. */
internal fun Char.Companion.toUpperCase(codePoint: Int): Int =
    if (codePoint <= MAX_VALUE.code) {
        @Suppress("DEPRECATION")
        codePoint.toChar().uppercaseChar().toInt()
    } else {
        codePoint // TODO: Implement this transformation for supplementary codepoints.
    }

internal expect fun Char.Companion.toCodePoint(high: Char, low: Char): Int
internal expect fun Char.Companion.toChars(codePoint: Int): CharArray
internal expect fun Char.Companion.isSupplementaryCodePoint(codepoint: Int): Boolean
internal expect fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean
