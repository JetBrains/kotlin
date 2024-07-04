/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Creates a Char with the specified [code].
 *
 * @sample samples.text.Chars.charFromCode
 */
@SinceKotlin("1.5")
@Deprecated("Provided for expect-actual matching", level = DeprecationLevel.HIDDEN)
@kotlin.internal.InlineOnly
public actual inline fun Char(code: UShort): Char = Char(code)
