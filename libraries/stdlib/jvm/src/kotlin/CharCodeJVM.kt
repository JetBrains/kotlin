/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Creates a Char with the specified [code].
 *
 * @sample samples.text.Chars.charFromCode
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public actual inline fun Char(code: UShort): Char {
    return code.toInt().toChar()
}
