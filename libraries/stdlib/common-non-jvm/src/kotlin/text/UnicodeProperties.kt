/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns the Unicode general category of this character as an Int.
 */
internal expect fun getCategoryValue(code: Int): Int

/**
 * Returns
 *   - `1` if the character is a lower case letter,
 *   - `2` if the character is an upper case letter,
 *   - `3` if the character is a letter but not a lower or upper case letter,
 *   - `0` otherwise.
 */
internal expect fun getLetterType(code: Int): Int

/**
 * Returns `true` if this character is a letter.
 */
internal fun isLetterImpl(code: Int): Boolean {
    return getLetterType(code) != 0
}

/**
 * Returns `true` if this character is a digit.
 */
internal expect fun isDigitImpl(code: Int): Boolean

/**
 * Returns `true` if this character is a whitespace.
 */
internal expect fun isWhitespaceImpl(code: Int): Boolean

/**
 * Returns `true` if this character is a lower case letter, or it has contributory property `Other_Lowercase`.
 */
internal fun isLowerCaseImpl(code: Int): Boolean {
    return getLetterType(code) == 1 || isOtherLowercase(code)
}

/**
 * Returns `true` if this character is an upper case letter, or it has contributory property `Other_Uppercase`.
 */
internal fun isUpperCaseImpl(code: Int): Boolean {
    return getLetterType(code) == 2 || isOtherUppercase(code)
}

internal expect fun isOtherLowercase(code: Int): Boolean
internal expect fun isOtherUppercase(code: Int): Boolean

