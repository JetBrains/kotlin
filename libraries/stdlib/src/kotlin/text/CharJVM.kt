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

package kotlin

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 */
public fun Char.isDefined(): Boolean = Character.isDefined(this)

/**
 * Returns `true` if this character is a letter.
 */
public fun Char.isLetter(): Boolean = Character.isLetter(this)

/**
 * Returns `true` if this character is a letter or digit.
 */
public fun Char.isLetterOrDigit(): Boolean = Character.isLetterOrDigit(this)

/**
 * Returns `true` if this character (Unicode code point) is a digit.
 */
public fun Char.isDigit(): Boolean = Character.isDigit(this)


/**
 * Returns `true` if this character (Unicode code point) should be regarded as an ignorable
 * character in a Java identifier or a Unicode identifier.
 */
public fun Char.isIdentifierIgnorable(): Boolean = Character.isIdentifierIgnorable(this)

/**
 * Returns `true` if this character is an ISO control character.
 */
public fun Char.isISOControl(): Boolean = Character.isISOControl(this)

/**
 * Returns `true` if this  character (Unicode code point) may be part of a Java identifier as other than the first character.
 */
public fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)

/**
 * Returns `true` if this character is permissible as the first character in a Java identifier.
 */
public fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

deprecated("Please use Char.isJavaIdentifierStart() instead")
public fun Char.isJavaLetter(): Boolean = Character.isJavaLetter(this)

deprecated("Please use Char.isJavaIdentifierPart() instead")
public fun Char.isJavaLetterOrDigit(): Boolean = Character.isJavaLetterOrDigit(this)

/**
 * Determines whether a character is whitespace according to the Unicode standard.
 * Returns `true` if the character is whitespace.
 */
public fun Char.isWhitespace(): Boolean = Character.isWhitespace(this) || Character.isSpaceChar(this)

/**
 * Returns `true` if this character is upper case.
 */
public fun Char.isUpperCase(): Boolean = Character.isUpperCase(this)

/**
 * Returns `true` if this character is lower case.
 */
public fun Char.isLowerCase(): Boolean = Character.isLowerCase(this)

/**
 * Converts this character to uppercase.
 */
public fun Char.toUpperCase(): Char = Character.toUpperCase(this)

/**
 * Converts this character to lowercase.
 */
public fun Char.toLowerCase(): Char = Character.toLowerCase(this)

/**
 * Returns `true` if this character is a titlecase character.
 */
public fun Char.isTitleCase(): Boolean = Character.isTitleCase(this)

/**
 * Converts this character to titlecase.
 *
 * @see Character.toTitleCase
 */
public fun Char.toTitleCase(): Char = Character.toTitleCase(this)

/**
 * Returns a value indicating a character's general category.
 */
public fun Char.category(): CharCategory = CharCategory.valueOf(Character.getType(this))

/**
 * Returns the Unicode directionality property for the given character.
 */
public fun Char.directionality(): CharDirectionality = CharDirectionality.valueOf(Character.getDirectionality(this).toInt())

// TODO Provide name for JVM7+
///**
// * Returns the Unicode name of this character, or null if the code point of this character is unassigned.
// */
//public fun Char.name(): String? = Character.getName(this.toInt())
