/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.IllegalArgumentException

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 */
@SymbolName("Kotlin_Char_isDefined")
external public fun Char.isDefined(): Boolean

/**
 * Returns `true` if this character is a letter.
 */
@SymbolName("Kotlin_Char_isLetter")
external public fun Char.isLetter(): Boolean

/**
 * Returns `true` if this character is a letter or digit.
 */
@SymbolName("Kotlin_Char_isLetterOrDigit")
external public fun Char.isLetterOrDigit(): Boolean

/**
 * Returns `true` if this character (Unicode code point) is a digit.
 */
@SymbolName("Kotlin_Char_isDigit")
external public fun Char.isDigit(): Boolean

/**
 * Returns `true` if this character (Unicode code point) should be regarded as an ignorable
 * character in a Java identifier or a Unicode identifier.
 */
@SymbolName("Kotlin_Char_isIdentifierIgnorable")
external public fun Char.isIdentifierIgnorable(): Boolean

/**
 * Returns `true` if this character is an ISO control character.
 */
@SymbolName("Kotlin_Char_isISOControl")
external public fun Char.isISOControl(): Boolean

/**
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 */
@SymbolName("Kotlin_Char_isWhitespace")
external public actual fun Char.isWhitespace(): Boolean

/**
 * Returns `true` if this character is upper case.
 */
@SymbolName("Kotlin_Char_isUpperCase")
external public fun Char.isUpperCase(): Boolean

/**
 * Returns `true` if this character is lower case.
 */
@SymbolName("Kotlin_Char_isLowerCase")
external public fun Char.isLowerCase(): Boolean

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 */
@SymbolName("Kotlin_Char_toUpperCase")
external public actual fun Char.toUpperCase(): Char

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
public actual fun Char.uppercaseChar(): Char = toUpperCase()

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
public actual fun Char.uppercase(): String {
    return uppercaseChar().toString()
}

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
@SymbolName("Kotlin_Char_toLowerCase")
external public actual fun Char.toLowerCase(): Char

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
public actual fun Char.lowercaseChar(): Char = toLowerCase()

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
public actual fun Char.lowercase(): String {
    return lowercaseChar().toString()
}

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
@SymbolName("Kotlin_Char_isHighSurrogate")
external public actual fun Char.isHighSurrogate(): Boolean

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
@SymbolName("Kotlin_Char_isLowSurrogate")
external public actual fun Char.isLowSurrogate(): Boolean


internal actual fun digitOf(char: Char, radix: Int): Int = digitOfChecked(char, checkRadix(radix))

@SymbolName("Kotlin_Char_digitOfChecked")
external internal fun digitOfChecked(char: Char, radix: Int): Int

/**
 * Returns a value indicating a character's general category.
 */
public val Char.category: CharCategory get() = CharCategory.valueOf(getType())

/** Retrun a Unicode category of the character as an Int. */
@SymbolName("Kotlin_Char_getType")
external internal fun Char.getType(): Int

/**
 * Checks whether the given [radix] is valid radix for string to number and number to string conversion.
 */
@PublishedApi
internal actual fun checkRadix(radix: Int): Int {
    if(radix !in Char.MIN_RADIX..Char.MAX_RADIX) {
        throw IllegalArgumentException("radix $radix was not in valid range ${Char.MIN_RADIX..Char.MAX_RADIX}")
    }
    return radix
}

// Char.Compaion methods. Konan specific.

// TODO: Make public when supplementary codepoints are supported.
/** Converts a unicode code point to lower case. */
internal fun Char.Companion.toLowerCase(codePoint: Int): Int =
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        codePoint.toChar().toLowerCase().toInt()
    } else {
        codePoint // TODO: Implement this transformation for supplementary codepoints.
    }

/** Converts a unicode code point to upper case. */
internal fun Char.Companion.toUpperCase(codePoint: Int): Int =
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        codePoint.toChar().toUpperCase().toInt()
    } else {
        codePoint // TODO: Implement this transformation for supplementary codepoints.
    }

/** Converts a surrogate pair to a unicode code point. Doesn't validate that the characters are a valid surrogate pair. */
public fun Char.Companion.toCodePoint(high: Char, low: Char): Int =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/** Checks if the codepoint specified is a supplementary codepoint or not. */
public fun Char.Companion.isSupplementaryCodePoint(codepoint: Int): Boolean =
    codepoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT

public fun Char.Companion.isSurrogatePair(high: Char, low: Char): Boolean = high.isHighSurrogate() && low.isLowSurrogate()

/**
 * Converts the codepoint specified to a char array. If the codepoint is not supplementary, the method will
 * return an array with one element otherwise it will return an array A with a high surrogate in A[0] and
 * a low surrogate in A[1].
 */
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
