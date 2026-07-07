/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public expect fun CodePoint.lowercase(): String

@ExperimentalCodePointApi
public expect fun CodePoint.lowercaseCodePoint(): CodePoint

@ExperimentalCodePointApi
public expect fun CodePoint.uppercase(): String

@ExperimentalCodePointApi
public expect fun CodePoint.uppercaseCodePoint(): CodePoint

@ExperimentalCodePointApi
public fun CodePoint.titlecase(): String {
    if (isBasic) {
        return code.toChar().titlecase()
    }
    // no different titlecase mapping outside BMP
    // no one-to-many uppercase expansions outside BMP
    return uppercase()
}

@ExperimentalCodePointApi
public expect fun CodePoint.titlecaseCodePoint(): CodePoint
