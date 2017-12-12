/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
@file:kotlin.jvm.JvmVersion
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CharsKt")


package kotlin.text

import kotlin.*

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isDefined(): Boolean = Character.isDefined(this)

/**
 * Returns `true` if this character is a letter.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isLetter(): Boolean = Character.isLetter(this)

/**
 * Returns `true` if this character is a letter or digit.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isLetterOrDigit(): Boolean = Character.isLetterOrDigit(this)

/**
 * Returns `true` if this character (Unicode code point) is a digit.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isDigit(): Boolean = Character.isDigit(this)


/**
 * Returns `true` if this character (Unicode code point) should be regarded as an ignorable
 * character in a Java identifier or a Unicode identifier.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isIdentifierIgnorable(): Boolean = Character.isIdentifierIgnorable(this)

/**
 * Returns `true` if this character is an ISO control character.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isISOControl(): Boolean = Character.isISOControl(this)

/**
 * Returns `true` if this  character (Unicode code point) may be part of a Java identifier as other than the first character.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)

/**
 * Returns `true` if this character is permissible as the first character in a Java identifier.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

/**
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 */
public fun Char.isWhitespace(): Boolean = Character.isWhitespace(this) || Character.isSpaceChar(this)

/**
 * Returns `true` if this character is upper case.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isUpperCase(): Boolean = Character.isUpperCase(this)

/**
 * Returns `true` if this character is lower case.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isLowerCase(): Boolean = Character.isLowerCase(this)

/**
 * Converts this character to uppercase.
 */
@kotlin.internal.InlineOnly
public inline fun Char.toUpperCase(): Char = Character.toUpperCase(this)

/**
 * Converts this character to lowercase.
 */
@kotlin.internal.InlineOnly
public inline fun Char.toLowerCase(): Char = Character.toLowerCase(this)

/**
 * Returns `true` if this character is a titlecase character.
 */
@kotlin.internal.InlineOnly
public inline fun Char.isTitleCase(): Boolean = Character.isTitleCase(this)

/**
 * Converts this character to titlecase.
 *
 * @see Character.toTitleCase
 */
@kotlin.internal.InlineOnly
public inline fun Char.toTitleCase(): Char = Character.toTitleCase(this)

/**
 * Returns a value indicating a character's general category.
 */
public val Char.category: CharCategory get() = CharCategory.valueOf(Character.getType(this))

/**
 * Returns the Unicode directionality property for the given character.
 */
public val Char.directionality: CharDirectionality get() = CharDirectionality.valueOf(Character.getDirectionality(this).toInt())

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
@kotlin.internal.InlineOnly
public inline fun Char.isHighSurrogate(): Boolean = Character.isHighSurrogate(this)

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
@kotlin.internal.InlineOnly
public inline fun Char.isLowSurrogate(): Boolean = Character.isLowSurrogate(this)

// TODO Provide name for JVM7+
///**
// * Returns the Unicode name of this character, or `null` if the code point of this character is unassigned.
// */
//public fun Char.name(): String? = Character.getName(this.toInt())



internal fun digitOf(char: Char, radix: Int): Int = Character.digit(char.toInt(), radix)

/**
 * Checks whether the given [radix] is valid radix for string to number and number to string conversion.
 */
@PublishedApi
internal fun checkRadix(radix: Int): Int {
    if(radix !in Character.MIN_RADIX..Character.MAX_RADIX) {
        throw IllegalArgumentException("radix $radix was not in valid range ${Character.MIN_RADIX..Character.MAX_RADIX}")
    }
    return radix
}