/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codepoints

@ExperimentalCodePointApi
public actual fun String.codePointAt(index: Int): CodePoint = (this as CharSequence).codePointAt(index)


@ExperimentalCodePointApi
public actual fun String.codePointBefore(index: Int): CodePoint = (this as CharSequence).codePointBefore(index)


@ExperimentalCodePointApi
public actual fun CharSequence.codePointAt(index: Int): CodePoint {
    if (index !in 0..<length) throw IndexOutOfBoundsException("index: $index is required to be in range 0..<$length")
    val c1 = this[index]
    if (index < length - 1 && c1.isHighSurrogate()) {
        val c2 = this[index + 1]
        if (c2.isLowSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c1.toCodePoint()
}

@ExperimentalCodePointApi
public actual fun CharSequence.codePointBefore(index: Int): CodePoint {
    if (index !in 1..length) throw IndexOutOfBoundsException("index: $index is required to be in range 1..$length")
    val c2 = this[index - 1]
    if (index > 1 && c2.isLowSurrogate()) {
        val c1 = this[index - 2]
        if (c1.isHighSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c2.toCodePoint()
}

@ExperimentalCodePointApi
public actual fun CharArray.codePointAt(index: Int): CodePoint {
    if (index !in 0..<size) throw IndexOutOfBoundsException("index: $index is required to be in range 0..<$size")
    val c1 = this[index]
    if (index < size - 1 && c1.isHighSurrogate()) {
        val c2 = this[index + 1]
        if (c2.isLowSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c1.toCodePoint()
}


@ExperimentalCodePointApi
public actual fun CharArray.codePointBefore(index: Int): CodePoint {
    if (index !in 1..size) throw IndexOutOfBoundsException("index: $index is required to be in range 1..$size")
    val c2 = this[index - 1]
    if (index > 1 && c2.isLowSurrogate()) {
        val c1 = this[index - 2]
        if (c1.isHighSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c2.toCodePoint()
}
