/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

fun numberToByte(a: dynamic): Byte = toByte(numberToInt(a))

fun numberToDouble(a: dynamic): Double = js("+a").unsafeCast<Double>()

fun numberToInt(a: dynamic): Int = if (a is Long) a.toInt() else doubleToInt(a)

fun numberToShort(a: dynamic): Short = toShort(numberToInt(a))

// << and >> shifts are used to preserve sign of the number
fun toByte(a: dynamic): Byte = js("a << 24 >> 24").unsafeCast<Byte>()
fun toShort(a: dynamic): Short = js("a << 16 >> 16").unsafeCast<Short>()

fun numberToLong(a: dynamic): Long = if (a is Long) a else fromNumber(a)

fun toLong(a: dynamic): Long = fromInt(a)

fun doubleToInt(a: Double): Int = when {
    a > 2147483647 -> 2147483647
    a < -2147483648 -> -2147483648
    else -> jsBitwiseOr(a, 0)
}

fun numberToChar(a: dynamic) = Char(numberToInt(a) and 0xFFFF)