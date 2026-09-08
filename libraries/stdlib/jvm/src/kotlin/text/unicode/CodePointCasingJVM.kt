/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

import kotlin.internal.InlineOnly


@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@InlineOnly
public actual inline fun CodePoint.lowercase(): String =
    toString().lowercase()

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@InlineOnly
public actual inline fun CodePoint.lowercaseCodePoint(): CodePoint =
    Character.toLowerCase(code).toCodePoint()

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@InlineOnly
public actual inline fun CodePoint.uppercase(): String =
    toString().uppercase()

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@InlineOnly
public actual inline fun CodePoint.uppercaseCodePoint(): CodePoint =
    Character.toLowerCase(code).toCodePoint()

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
@InlineOnly
public actual inline fun CodePoint.titlecaseCodePoint(): CodePoint =
    Character.toTitleCase(code).toCodePoint()
