/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text

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
external public fun Char.isWhitespace(): Boolean

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
 * Converts this character to uppercase.
 */
@SymbolName("Kotlin_Char_toUpperCase")
external public fun Char.toUpperCase(): Char

/**
 * Converts this character to lowercase.
 */
@SymbolName("Kotlin_Char_toLowerCase")
external public fun Char.toLowerCase(): Char

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
@SymbolName("Kotlin_Char_isHighSurrogate")
external public fun Char.isHighSurrogate(): Boolean

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
@SymbolName("Kotlin_Char_isLowSurrogate")
external public fun Char.isLowSurrogate(): Boolean


internal fun digitOf(char: Char, radix: Int): Int = digitOfChecked(char, checkRadix(radix))

@SymbolName("Kotlin_Char_digitOfChecked")
external internal fun digitOfChecked(char: Char, radix: Int): Int

/**
 * Checks whether the given [radix] is valid radix for string to number and number to string conversion.
 */
@PublishedApi
internal fun checkRadix(radix: Int): Int {
    if(radix !in Char.MIN_RADIX..Char.MAX_RADIX) {
        throw IllegalArgumentException("radix $radix was not in valid range ${Char.MIN_RADIX..Char.MAX_RADIX}")
    }
    return radix
}
