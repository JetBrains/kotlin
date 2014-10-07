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

public fun Char.isDefined(): Boolean = Character.isDefined(this)

public fun Char.isDigit(): Boolean = Character.isDigit(this)

public fun Char.isHighSurrogate(): Boolean = Character.isHighSurrogate(this)

public fun Char.isIdentifierIgnorable(): Boolean = Character.isIdentifierIgnorable(this)

public fun Char.isISOControl(): Boolean = Character.isISOControl(this)

public fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)

public fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

public fun Char.isJavaLetter(): Boolean = Character.isJavaLetter(this)

public fun Char.isJavaLetterOrDigit(): Boolean = Character.isJavaLetterOrDigit(this)

/**
 * Returns true if the character is whitespace
 *
 * @includeFunctionBody ../../test/text/StringTest.kt count
 */
public fun Char.isWhitespace(): Boolean = Character.isWhitespace(this)

/**
 * Returns true if this character is upper case
 */
public fun Char.isUpperCase(): Boolean = Character.isUpperCase(this)

/**
 * Returns true if this character is lower case
 */
public fun Char.isLowerCase(): Boolean = Character.isLowerCase(this)
