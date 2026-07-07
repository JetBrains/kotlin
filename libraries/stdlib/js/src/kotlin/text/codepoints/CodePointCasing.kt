/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public actual fun CodePoint.lowercase(): String =
    toString().lowercase()

@ExperimentalCodePointApi
public actual fun CodePoint.lowercaseCodePoint(): CodePoint =
    // there's a single multichar expansion
    lowercase().codePointAt(0)

@ExperimentalCodePointApi
public actual fun CodePoint.uppercase(): String =
    toString().uppercase()

@ExperimentalCodePointApi
public actual fun CodePoint.uppercaseCodePoint(): CodePoint {
    val uppercase = uppercase()
    return if (uppercase.codePointCount() > 1) this else uppercase.codePointAt(0)
}
