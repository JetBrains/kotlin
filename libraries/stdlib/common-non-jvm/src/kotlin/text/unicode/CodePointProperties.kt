/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

@ExperimentalCodePointApi
public actual val CodePoint.category: CharCategory
    get() = CharCategory.valueOf(getCategoryValue(this.code))

@ExperimentalCodePointApi
public actual fun CodePoint.isDefined(): Boolean =
    code < 0x80 || getCategoryValue(code) != CharCategory.UNASSIGNED.value

@ExperimentalCodePointApi
public actual fun CodePoint.isDigit(): Boolean =
    if (isBasic) code.toChar().isDigit() else isDigitImpl(code)

@ExperimentalCodePointApi
public actual fun CodePoint.isLetter(): Boolean =
    if (isBasic) code.toChar().isLetter() else isLetterImpl(code)

@ExperimentalCodePointApi
public actual fun CodePoint.isLetterOrDigit(): Boolean =
    if (isBasic) code.toChar().isLetterOrDigit() else (isLetterImpl(code) || isDigitImpl(code))

@ExperimentalCodePointApi
public actual fun CodePoint.isISOControl(): Boolean =
    isBasic && code.toChar().isISOControl()

@ExperimentalCodePointApi
public actual fun CodePoint.isLowerCase(): Boolean =
    if (isBasic) code.toChar().isLowerCase() else isLowerCaseImpl(code)

@ExperimentalCodePointApi
public actual fun CodePoint.isUpperCase(): Boolean =
    if (isBasic) code.toChar().isUpperCase() else isUpperCaseImpl(code)

@ExperimentalCodePointApi
public actual fun CodePoint.isTitleCase(): Boolean {
    if (code < 0x80) {
        return false
    }
    return getCategoryValue(code) == CharCategory.TITLECASE_LETTER.value
}

@ExperimentalCodePointApi
public actual fun CodePoint.isWhitespace(): Boolean =
    isWhitespaceImpl(code)

