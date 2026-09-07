/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode


@ExperimentalCodePointApi
public actual fun CodePoint.lowercase(): String =
    if (isBasic) code.toChar().lowercaseImpl() else lowercaseCodePoint().toString()

@ExperimentalCodePointApi
public actual fun CodePoint.lowercaseCodePoint(): CodePoint =
    lowercaseCodePoint(code).toCodePoint()

@ExperimentalCodePointApi
public actual fun CodePoint.uppercase(): String =
    if (isBasic) code.toChar().uppercaseImpl() else uppercaseCodePoint().toString()

@ExperimentalCodePointApi
public actual fun CodePoint.uppercaseCodePoint(): CodePoint =
    uppercaseCodePoint(code).toCodePoint()
