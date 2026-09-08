/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

/**
 * Returns the Unicode general category of the Unicode character corresponding to this code point.
 */
@ExperimentalUnicodeApi
public expect val CodePoint.category: CharCategory

/**
 * Returns `true` if the Unicode character corresponding to the specified [codePoint] belongs to this category.
 */
@ExperimentalUnicodeApi
public operator fun CharCategory.contains(codePoint: CodePoint): Boolean =
    codePoint.category == this


/**
 * Returns `true` if the Unicode character corresponding to this code point is defined in Unicode.
 *
 * A character is considered to be defined in Unicode if its [category] is not [CharCategory.UNASSIGNED].
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isDefined(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code point is a digit.
 *
 * A character is considered to be a digit if its [category] is [CharCategory.DECIMAL_DIGIT_NUMBER].
 *
 * @sample samples.text.CodePoints.isDigit
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isDigit(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is a letter.
 *
 * A character is considered to be a letter if its [category] is [CharCategory.UPPERCASE_LETTER],
 * [CharCategory.LOWERCASE_LETTER], [CharCategory.TITLECASE_LETTER], [CharCategory.MODIFIER_LETTER], or [CharCategory.OTHER_LETTER].
 *
 * @sample samples.text.CodePoints.isLetter
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isLetter(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is a letter or digit.
 *
 * @see isLetter
 * @see isDigit
 *
 * @sample samples.text.CodePoints.isLetterOrDigit
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isLetterOrDigit(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is an ISO control character.
 *
 * A character is considered to be an ISO control character if its [category] is [CharCategory.CONTROL],
 * meaning the Char is in the range `'\u0000'..'\u001F'` or in the range `'\u007F'..'\u009F'`.
 *
 * @sample samples.text.CodePoints.isISOControl
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isISOControl(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is lower case.
 *
 * A character is considered to be a lower case character if its [category] is [CharCategory.LOWERCASE_LETTER],
 * or it has contributory property `Other_Lowercase` as defined by the Unicode Standard.
 *
 * @sample samples.text.CodePoints.isLowerCase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isLowerCase(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is upper case.
 *
 * A character is considered to be an upper case character if its [category] is [CharCategory.UPPERCASE_LETTER],
 * or it has contributory property `Other_Uppercase` as defined by the Unicode Standard.
 *
 * @sample samples.text.CodePoints.isUpperCase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isUpperCase(): Boolean

/**
 * Returns `true` if the Unicode character corresponding to this code pointCodePoints is a title case letter.
 *
 * A character is considered to be a title case letter if its [category] is [CharCategory.TITLECASE_LETTER].
 *
 * @sample samples.text.CodePoints.isTitleCase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isTitleCase(): Boolean

/**
 * Determines whether the Unicode character corresponding to this code point is whitespace.
 *
 * A character is considered whitespace if either its Unicode [category][Char.category]
 * is one of [CharCategory.SPACE_SEPARATOR], [CharCategory.LINE_SEPARATOR], [CharCategory.PARAGRAPH_SEPARATOR],
 * or it is a [CharCategory.CONTROL] character in range `U+0009..U+000D` or `U+001C..U+001F`.
 *
 * Returns `true` if the character is whitespace.
 *
 * @sample samples.text.CodePoints.isWhitespace
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.isWhitespace(): Boolean

