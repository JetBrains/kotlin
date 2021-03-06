/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.IllegalArgumentException

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 *
 * A character is considered to be defined in Unicode if its [category] is not [CharCategory.UNASSIGNED].
 */
public actual fun Char.isDefined(): Boolean {
    if (this < '\u0080') {
        return true
    }
    return getCategoryValue() != CharCategory.UNASSIGNED.value
}

/**
 * Returns `true` if this character is a letter.
 *
 * A character is considered to be a letter if its [category] is [CharCategory.UPPERCASE_LETTER],
 * [CharCategory.LOWERCASE_LETTER], [CharCategory.TITLECASE_LETTER], [CharCategory.MODIFIER_LETTER], or [CharCategory.OTHER_LETTER].
 *
 * @sample samples.text.Chars.isLetter
 */
public actual fun Char.isLetter(): Boolean {
    if (this in 'a'..'z' || this in 'A'..'Z') {
        return true
    }
    if (this < '\u0080') {
        return false
    }
    return isLetterImpl()
}

/**
 * Returns `true` if this character is a letter or digit.
 *
 * @see isLetter
 * @see isDigit
 *
 * @sample samples.text.Chars.isLetterOrDigit
 */
public actual fun Char.isLetterOrDigit(): Boolean {
    if (this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9') {
        return true
    }
    if (this < '\u0080') {
        return false
    }

    return isDigit() || isLetter()
}

/**
 * Returns `true` if this character is a digit.
 *
 * A character is considered to be a digit if its [category] is [CharCategory.DECIMAL_DIGIT_NUMBER].
 *
 * @sample samples.text.Chars.isDigit
 */
public actual fun Char.isDigit(): Boolean {
    if (this in '0'..'9') {
        return true
    }
    if (this < '\u0080') {
        return false
    }
    return isDigitImpl()
}

/**
 * Returns `true` if this character (Unicode code point) should be regarded as an ignorable
 * character in a Java identifier or a Unicode identifier.
 */
@SymbolName("Kotlin_Char_isIdentifierIgnorable")
external public fun Char.isIdentifierIgnorable(): Boolean

/**
 * Returns `true` if this character is an ISO control character.
 *
 * A character is considered to be an ISO control character if its code is in the range `'\u0000'..'\u001F'` or in the range `'\u007F'..'\u009F'`.
 *
 * @sample samples.text.Chars.isISOControl
 */
@SymbolName("Kotlin_Char_isISOControl")
external public actual fun Char.isISOControl(): Boolean

/**
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 *
 * @sample samples.text.Chars.isWhitespace
 */
public actual fun Char.isWhitespace(): Boolean = isWhitespaceImpl()

/**
 * Returns `true` if this character is an upper case letter.
 *
 * A character is considered to be an upper case letter if its [category] is [CharCategory.UPPERCASE_LETTER].
 *
 * @sample samples.text.Chars.isUpperCase
 */
public actual fun Char.isUpperCase(): Boolean {
    if (this in 'A'..'Z') {
        return true
    }
    if (this < '\u0080') {
        return false
    }
    return isUpperCaseImpl()
}

/**
 * Returns `true` if this character is a lower case letter.
 *
 * A character is considered to be a lower case letter if its [category] is [CharCategory.LOWERCASE_LETTER].
 *
 * @sample samples.text.Chars.isLowerCase
 */
public actual fun Char.isLowerCase(): Boolean {
    if (this in 'a'..'z') {
        return true
    }
    if (this < '\u0080') {
        return false
    }
    return isLowerCaseImpl()
}

/**
 * Returns `true` if this character is a title case letter.
 *
 * A character is considered to be a title case letter if its [category] is [CharCategory.TITLECASE_LETTER].
 *
 * @sample samples.text.Chars.isTitleCase
 */
@SinceKotlin("1.5")
public actual fun Char.isTitleCase(): Boolean {
    if (this < '\u0080') {
        return false
    }
    return getCategoryValue() == CharCategory.TITLECASE_LETTER.value
}

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 */
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
@SinceKotlin("1.4")
@ExperimentalStdlibApi
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
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public actual fun Char.uppercase(): String = uppercaseImpl()

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
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
@SinceKotlin("1.4")
@ExperimentalStdlibApi
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
@SinceKotlin("1.4")
@ExperimentalStdlibApi
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
 * Returns the Unicode general category of this character.
 */
public actual val Char.category: CharCategory
    get() = CharCategory.valueOf(getCategoryValue())

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
