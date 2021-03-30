/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CharsKt")

package kotlin.text

/**
 * Returns the numeric value of the decimal digit that this Char represents.
 * Throws an exception if this Char is not a valid decimal digit.
 *
 * A Char is considered to represent a decimal digit if the Char is one of the ASCII decimal digits '0' through '9'.
 * In this case, `this.code - '0'.code` is returned.
 *
 * @sample samples.text.Chars.digitToInt
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Char.digitToInt(): Int {
    if (this in '0'..'9') {
        return this - '0'
    }
    throw IllegalArgumentException("Char $this is not a decimal digit")
}

/**
 * Returns the numeric value of the digit that this Char represents in the specified [radix].
 * Throws an exception if the [radix] is not in the range `2..36` or if this Char is not a valid digit in the specified [radix].
 *
 * A Char is considered to represent a digit in the specified [radix] if at least one of the following is true:
 *  - The Char is one of the ASCII decimal digits '0' through '9' and its [code] is less than `radix + '0'.code`. In this case, `this.code - '0'.code` is returned.
 *  - The Char is one of the uppercase Latin letters 'A' through 'Z' and its [code] is less than `radix + 'A'.code - 10`. In this case, `this.code - 'A'.code + 10` is returned.
 *  - The Char is one of the lowercase Latin letters 'a' through 'z' and its [code] is less than `radix + 'a'.code - 10`. In this case, `this.code - 'a'.code + 10` is returned.
 *
 * @sample samples.text.Chars.digitToInt
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Char.digitToInt(radix: Int): Int {
    return digitToIntOrNull(radix) ?: throw IllegalArgumentException("Char $this is not a digit in the given radix=$radix")
}

/**
 *
 * Returns the numeric value of the decimal digit that this Char represents, or `null` if this Char is not a valid decimal digit.
 *
 * A Char is considered to represent a decimal digit if the Char is one of the ASCII decimal digits '0' through '9'.
 * In this case, `this.code - '0'.code` is returned.
 *
 * @sample samples.text.Chars.digitToIntOrNull
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Char.digitToIntOrNull(): Int? {
    if (this in '0'..'9') {
        return this - '0'
    }
    return null
}

/**
 * Returns the numeric value of the digit that this Char represents in the specified [radix], or `null` if this Char is not a valid digit in the specified [radix].
 * Throws an exception if the [radix] is not in the range `2..36`.
 *
 * A Char is considered to represent a digit in the specified [radix] if at least one of the following is true:
 *  - The Char is one of the ASCII decimal digits '0' through '9' and its [code] is less than `radix + '0'.code`. In this case, `this.code - '0'.code` is returned.
 *  - The Char is one of the uppercase Latin letters 'A' through 'Z' and its [code] is less than `radix + 'A'.code - 10`. In this case, `this.code - 'A'.code + 10` is returned.
 *  - The Char is one of the lowercase Latin letters 'a' through 'z' and its [code] is less than `radix + 'a'.code - 10`. In this case, `this.code - 'a'.code + 10` is returned.
 *
 * @sample samples.text.Chars.digitToIntOrNull
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Char.digitToIntOrNull(radix: Int): Int? {
    if (radix !in 2..36) {
        throw IllegalArgumentException("Invalid radix: $radix. Valid radix values are in range 2..36")
    }
    if (this in '0'..'9') {
        val digit = this - '0'
        return if (digit < radix) digit else null
    }
    val a = if (this <= 'Z') 'A' else 'a'
    val digit = 10 + (this - a)
    return if (digit in 10 until radix) digit else null
}

/**
 * Returns the Char that represents this decimal digit.
 * Throws an exception if this value is not in the range `0..9`.
 *
 * If this value is in `0..9`, the decimal digit Char with code `'0'.code + this` is returned.
 *
 * @sample samples.text.Chars.digitToChar
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Int.digitToChar(): Char {
    if (this in 0..9) {
        return '0' + this
    }
    throw IllegalArgumentException("Int $this is not a decimal digit")
}

/**
 * Returns the Char that represents this numeric digit value in the specified [radix].
 * Throws an exception if the [radix] is not in the range `2..36` or if this value is not in the range `0 until radix`.
 *
 * If this value is less than `10`, the decimal digit Char with code `'0'.code + this` is returned.
 * Otherwise, the uppercase Latin letter with code `'A'.code + this - 10` is returned.
 *
 * @sample samples.text.Chars.digitToChar
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
public fun Int.digitToChar(radix: Int): Char {
    if (radix !in 2..36) {
        throw IllegalArgumentException("Invalid radix: $radix. Valid radix values are in range 2..36")
    }
    if (this < 0 || this >= radix) {
        throw IllegalArgumentException("Digit $this does not represent a valid digit in radix $radix")
    }
    return if (this < 10) {
        '0' + this
    } else {
        'A' + this - 10
    }
}

/**
 * Converts this character to lower case using Unicode mapping rules of the invariant locale.
 */
public expect fun Char.toLowerCase(): Char

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
public expect fun Char.toUpperCase(): Char

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
 * Converts this character to title case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping use the [titlecase] function.
 * If this character has no mapping equivalent, the result of calling [uppercaseChar] is returned.
 *
 * @sample samples.text.Chars.titlecase
 */
@SinceKotlin("1.5")
@ExperimentalStdlibApi
public expect fun Char.titlecaseChar(): Char

/**
 * Converts this character to title case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the length of the returned string can be greater than one.
 * For example, `'\uFB00'.titlecase()` returns `"\u0046\u0066"`,
 * where `'\uFB00'` is the LATIN SMALL LIGATURE FF character (`ﬀ`).
 * If this character has no title case mapping, the result of [uppercase] is returned instead.
 *
 * @sample samples.text.Chars.titlecase
 */
@SinceKotlin("1.5")
@ExperimentalStdlibApi
public fun Char.titlecase(): String = titlecaseImpl()

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

/**
 * Returns the Unicode general category of this character.
 */
@SinceKotlin("1.5")
public expect val Char.category: CharCategory

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 *
 * A character is considered to be defined in Unicode if its [category] is not [CharCategory.UNASSIGNED].
 */
@SinceKotlin("1.5")
public expect fun Char.isDefined(): Boolean

/**
 * Returns `true` if this character is a letter.
 *
 * A character is considered to be a letter if its [category] is [CharCategory.UPPERCASE_LETTER],
 * [CharCategory.LOWERCASE_LETTER], [CharCategory.TITLECASE_LETTER], [CharCategory.MODIFIER_LETTER], or [CharCategory.OTHER_LETTER].
 *
 * @sample samples.text.Chars.isLetter
 */
@SinceKotlin("1.5")
public expect fun Char.isLetter(): Boolean

/**
 * Returns `true` if this character is a letter or digit.
 *
 * @see isLetter
 * @see isDigit
 *
 * @sample samples.text.Chars.isLetterOrDigit
 */
@SinceKotlin("1.5")
public expect fun Char.isLetterOrDigit(): Boolean

/**
 * Returns `true` if this character is a digit.
 *
 * A character is considered to be a digit if its [category] is [CharCategory.DECIMAL_DIGIT_NUMBER].
 *
 * @sample samples.text.Chars.isDigit
 */
@SinceKotlin("1.5")
public expect fun Char.isDigit(): Boolean

/**
 * Returns `true` if this character is an upper case letter.
 *
 * A character is considered to be an upper case letter if its [category] is [CharCategory.UPPERCASE_LETTER].
 *
 * @sample samples.text.Chars.isUpperCase
 */
@SinceKotlin("1.5")
public expect fun Char.isUpperCase(): Boolean

/**
 * Returns `true` if this character is a lower case letter.
 *
 * A character is considered to be a lower case letter if its [category] is [CharCategory.LOWERCASE_LETTER].
 *
 * @sample samples.text.Chars.isLowerCase
 */
@SinceKotlin("1.5")
public expect fun Char.isLowerCase(): Boolean

/**
 * Returns `true` if this character is a title case letter.
 *
 * A character is considered to be a title case letter if its [category] is [CharCategory.TITLECASE_LETTER].
 *
 * @sample samples.text.Chars.isTitleCase
 */
@SinceKotlin("1.5")
public expect fun Char.isTitleCase(): Boolean

/**
 * Returns `true` if this character is an ISO control character.
 *
 * A character is considered to be an ISO control character if its [category] is [CharCategory.CONTROL].
 *
 * @sample samples.text.Chars.isISOControl
 */
@SinceKotlin("1.5")
public expect fun Char.isISOControl(): Boolean

/**
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 *
 * @sample samples.text.Chars.isWhitespace
 */
public expect fun Char.isWhitespace(): Boolean
