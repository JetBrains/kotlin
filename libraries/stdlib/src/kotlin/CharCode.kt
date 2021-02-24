/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


/**
 * Creates a Char with the specified [code], or throws an exception if the [code] is out of `Char.MIN_VALUE.code..Char.MAX_VALUE.code`.
 *
 * If the program that calls this function is written in a way that only valid [code] is passed as the argument,
 * using the overload that takes a [UShort] argument is preferable (`Char(intValue.toUShort())`).
 * That overload doesn't check validity of the argument, and may improve program performance when the function is called routinely inside a loop.
 *
 * @sample samples.text.Chars.charFromCode
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ExperimentalStdlibApi
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun Char(code: Int): Char {
    if (code < Char.MIN_VALUE.code || code > Char.MAX_VALUE.code) {
        throw IllegalArgumentException("Invalid Char code: $code")
    }
    return Char(code.toUShort())
}

/**
 * Creates a Char with the specified [code].
 *
 * @sample samples.text.Chars.charFromCode
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ExperimentalStdlibApi
@SinceKotlin("1.4")
@Suppress("NO_ACTUAL_FOR_EXPECT")
public expect fun Char(code: UShort): Char

/**
 * Returns the code of this Char.
 *
 * Code of a Char is the value it was constructed with, and the UTF-16 code unit corresponding to this Char.
 *
 * @sample samples.text.Chars.code
 */
@ExperimentalStdlibApi
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline val Char.code: Int get() = this.toInt()
