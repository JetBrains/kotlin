/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode


@ExperimentalUnicodeApi
public actual fun CodePoint.lowercase(): String =
    if (isBasic) code.toChar().lowercaseImpl() else lowercaseCodePoint().toString()

@ExperimentalUnicodeApi
public actual fun CodePoint.lowercaseCodePoint(): CodePoint =
    lowercaseCodePoint(code).toCodePoint()

@ExperimentalUnicodeApi
public actual fun CodePoint.uppercase(): String =
    if (isBasic) code.toChar().uppercaseImpl() else uppercaseCodePoint().toString()

@ExperimentalUnicodeApi
public actual fun CodePoint.uppercaseCodePoint(): CodePoint =
    uppercaseCodePoint(code).toCodePoint()
