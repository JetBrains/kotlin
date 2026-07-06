/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public expect val CodePoint.category: CharCategory

@ExperimentalCodePointApi
public operator fun CharCategory.contains(codePoint: CodePoint): Boolean =
    codePoint.category == this

@ExperimentalCodePointApi
public expect fun CodePoint.isDefined(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isDigit(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isLetter(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isLetterOrDigit(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isISOControl(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isLowerCase(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isUpperCase(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isTitleCase(): Boolean

@ExperimentalCodePointApi
public expect fun CodePoint.isWhitespace(): Boolean

