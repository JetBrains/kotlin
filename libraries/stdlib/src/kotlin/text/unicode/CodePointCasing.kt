/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

/**
 * Converts this code point to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the returned string can contain more than one code point.
 * For example, `CodePoint(0x0130).lowercase()` returns `"\u0069\u0307"`,
 * where `'\u0130'` is the LATIN CAPITAL LETTER I WITH DOT ABOVE character (`İ`).
 * If this code point has no lower case mapping, the result of `toString()` of this code point is returned.
 *
 * @sample samples.text.Chars.lowercase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.lowercase(): String

/**
 * Converts this code point to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping, use the [lowercase] function.
 * If this code point has no lower case equivalent, the code point itself is returned.
 *
 * @sample samples.text.Chars.lowercase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.lowercaseCodePoint(): CodePoint

/**
 * Converts this code point to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the returned string can contain more than one code point.
 * For example, `CodePoint(0xFB00).uppercase()` returns `"\u0046\u0046"`,
 * where `'\uFB00'` is the LATIN SMALL LIGATURE FF character (`ﬀ`).
 * If this code point has no upper case mapping, the result of `toString()` of this code point is returned.
 *
 * @sample samples.text.Chars.uppercase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.uppercase(): String

/**
 * Converts this code point to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping, use the [uppercase] function.
 * If this code point has no upper case equivalent, the code point itself is returned.
 *
 * @sample samples.text.Chars.uppercase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.uppercaseCodePoint(): CodePoint

/**
 * Converts this code point to title case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many character mapping, thus the returned string can contain more than one code point.
 * For example, `CodePoint(0xFB00).titlecase()` returns `"\u0046\u0066"`,
 * where `'\uFB00'` is the LATIN SMALL LIGATURE FF character (`ﬀ`).
 * If this code point has no title case mapping, the result of [uppercase] is returned instead.
 *
 * @sample samples.text.Chars.titlecase
 */
@ExperimentalUnicodeApi
public fun CodePoint.titlecase(): String {
    if (isBasic) {
        return code.toChar().titlecase()
    }
    // no different titlecase mapping outside BMP
    // no one-to-many uppercase expansions outside BMP
    return uppercase()
}

/**
 * Converts this code point to title case using Unicode mapping rules of the invariant locale.
 *
 * This function performs one-to-one character mapping.
 * To support one-to-many character mapping, use the [titlecase] function.
 * If this code point has no title case equivalent, the result of calling [uppercaseCodePoint] is returned.
 *
 * @sample samples.text.Chars.titlecase
 */
@ExperimentalUnicodeApi
public expect fun CodePoint.titlecaseCodePoint(): CodePoint
