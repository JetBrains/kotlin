/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
@Deprecated("Use lowercaseChar() instead.", ReplaceWith("lowercaseChar()"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public actual inline fun Char.toLowerCase(): Char = lowercaseChar()

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
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun Char.lowercaseChar(): Char = lowercase()[0]

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
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun Char.lowercase(): String = toString().asDynamic().toLowerCase().unsafeCast<String>()

/**
 * Converts this character to upper case using Unicode mapping rules of the invariant locale.
 */
@Deprecated("Use uppercaseChar() instead.", ReplaceWith("uppercaseChar()"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public actual inline fun Char.toUpperCase(): Char = uppercaseChar()

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
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun Char.uppercaseChar(): Char {
    val uppercase = uppercase()
    return if (uppercase.length > 1) this else uppercase[0]
}

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
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun Char.uppercase(): String = toString().asDynamic().toUpperCase().unsafeCast<String>()

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
public actual fun Char.isHighSurrogate(): Boolean = this in Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
public actual fun Char.isLowSurrogate(): Boolean = this in Char.MIN_LOW_SURROGATE..Char.MAX_LOW_SURROGATE

/**
 * Returns the Unicode general category of this character.
 */
@SinceKotlin("1.5")
public actual val Char.category: CharCategory
    get() = CharCategory.valueOf(getCategoryValue())

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 *
 * A character is considered to be defined in Unicode if its [category] is not [CharCategory.UNASSIGNED].
 */
@SinceKotlin("1.5")
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
@SinceKotlin("1.5")
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
@SinceKotlin("1.5")
public actual fun Char.isLetterOrDigit(): Boolean {
    if (this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9') {
        return true
    }
    if (this < '\u0080') {
        return false
    }

    return isDigitImpl() || isLetterImpl()
}

/**
 * Returns `true` if this character is a digit.
 *
 * A character is considered to be a digit if its [category] is [CharCategory.DECIMAL_DIGIT_NUMBER].
 *
 * @sample samples.text.Chars.isDigit
 */
@SinceKotlin("1.5")
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
 * Returns `true` if this character is upper case.
 *
 * A character is considered to be an upper case character if its [category] is [CharCategory.UPPERCASE_LETTER],
 * or it has contributory property `Other_Uppercase` as defined by the Unicode Standard.
 *
 * @sample samples.text.Chars.isUpperCase
 */
@SinceKotlin("1.5")
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
 * Returns `true` if this character is lower case.
 *
 * A character is considered to be a lower case character if its [category] is [CharCategory.LOWERCASE_LETTER],
 * or it has contributory property `Other_Lowercase` as defined by the Unicode Standard.
 *
 * @sample samples.text.Chars.isLowerCase
 */
@SinceKotlin("1.5")
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
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 *
 * @sample samples.text.Chars.isWhitespace
 */
public actual fun Char.isWhitespace(): Boolean = isWhitespaceImpl()