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
public actual inline fun String.codePointBefore(index: Int): CodePoint =
    (this as java.lang.String).codePointBefore(index).toCodePoint()

@ExperimentalCodePointApi
public actual fun CharSequence.codePointAt(index: Int): CodePoint =
    Character.codePointAt(this, index).toCodePoint()

@ExperimentalCodePointApi
public actual fun CharSequence.codePointBefore(index: Int): CodePoint =
    Character.codePointBefore(this, index).toCodePoint()

@ExperimentalCodePointApi
public actual fun CharArray.codePointAt(index: Int): CodePoint =
    Character.codePointAt(this, index).toCodePoint()

@ExperimentalCodePointApi
public actual fun CharArray.codePointBefore(index: Int): CodePoint =
    Character.codePointBefore(this, index).toCodePoint()
