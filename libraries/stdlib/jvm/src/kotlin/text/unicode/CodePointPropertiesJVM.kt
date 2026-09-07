/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

import kotlin.text.category


@ExperimentalCodePointApi
public actual val CodePoint.category: CharCategory
    get() = CharCategory.valueOf(Character.getType(this.code))

@ExperimentalCodePointApi
public val CodePoint.directionality: CharDirectionality
    get() = CharDirectionality.valueOf(Character.getDirectionality(this.code).toInt())

/**
 * Returns `true` if this character (Unicode code point) is defined in Unicode.
 *
 * A character is considered to be defined in Unicode if its [category] is not [CharCategory.UNASSIGNED].
 */
@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isDefined(): Boolean = Character.isDefined(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isLetter(): Boolean = Character.isLetter(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isDigit(): Boolean = Character.isDigit(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isLetterOrDigit(): Boolean = Character.isLetterOrDigit(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isISOControl(): Boolean = Character.isISOControl(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isLowerCase(): Boolean = Character.isLowerCase(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isTitleCase(): Boolean = Character.isTitleCase(this.code)

@ExperimentalCodePointApi
@kotlin.internal.InlineOnly
public actual inline fun CodePoint.isUpperCase(): Boolean = Character.isUpperCase(this.code)

@ExperimentalCodePointApi
public actual fun CodePoint.isWhitespace(): Boolean =
    // no whitespace outside BMP
    isBasic && code.toChar().isWhitespace()
