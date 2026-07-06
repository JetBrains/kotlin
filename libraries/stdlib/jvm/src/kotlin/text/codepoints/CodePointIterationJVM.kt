/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.text.codepoints

import kotlin.internal.InlineOnly

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun String.codePointAt(index: Int): CodePoint =
    (this as java.lang.String).codePointAt(index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharSequence.codePointAt(index: Int): CodePoint =
    Character.codePointAt(this, index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharArray.codePointAt(index: Int): CodePoint =
    Character.codePointAt(this, index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharArray.codePointAt(index: Int, endIndex: Int): CodePoint =
    Character.codePointAt(this, index, endIndex).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun String.codePointBefore(index: Int): CodePoint =
    (this as java.lang.String).codePointBefore(index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharSequence.codePointBefore(index: Int): CodePoint =
    Character.codePointBefore(this, index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharArray.codePointBefore(index: Int): CodePoint =
    Character.codePointBefore(this, index).toCodePoint()

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharArray.codePointBefore(index: Int, startIndex: Int): CodePoint =
    Character.codePointBefore(this, index, startIndex).toCodePoint()


@ExperimentalCodePointApi
@InlineOnly
public actual inline fun String.codePointCount(startIndex: Int, endIndex: Int): Int =
    (this as java.lang.String).codePointCount(startIndex, endIndex)

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharSequence.codePointCount(startIndex: Int, endIndex: Int): Int =
    Character.codePointCount(this, startIndex, endIndex)

@ExperimentalCodePointApi
@InlineOnly
public actual fun CharArray.codePointCount(startIndex: Int, endIndex: Int): Int =
    Character.codePointCount(this, startIndex, endIndex - startIndex)

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int =
    (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharSequence.offsetByCodePoints(index: Int, codePointOffset: Int): Int =
    Character.offsetByCodePoints(this, index, codePointOffset)

@ExperimentalCodePointApi
@InlineOnly
public actual inline fun CharArray.offsetByCodePoints(index: Int, codePointOffset: Int, startIndex: Int, endIndex: Int): Int =
    Character.offsetByCodePoints(this, startIndex, endIndex - startIndex, index, codePointOffset)

