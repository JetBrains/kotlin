/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

// actually \s is enough to match all whitespace, but \xA0 added because of different regexp behavior of Rhino used in Selenium tests
public actual fun Char.isWhitespace(): Boolean = toString().matches("[\\s\\xA0]")

@kotlin.internal.InlineOnly
public actual inline fun Char.toLowerCase(): Char = js("String.fromCharCode")(toInt()).toLowerCase().charCodeAt(0).unsafeCast<Int>().toChar()

@kotlin.internal.InlineOnly
public actual inline fun Char.toUpperCase(): Char = js("String.fromCharCode")(toInt()).toUpperCase().charCodeAt(0).unsafeCast<Int>().toChar()

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
public actual fun Char.isHighSurrogate(): Boolean = this in Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
public actual fun Char.isLowSurrogate(): Boolean = this in Char.MIN_LOW_SURROGATE..Char.MAX_LOW_SURROGATE
