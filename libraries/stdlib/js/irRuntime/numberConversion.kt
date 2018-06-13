/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

fun asIs(a: dynamic) = a

fun numberToByte(a: dynamic): Byte = toByte(numberToInt(a))

fun numberToDouble(a: dynamic): Double = js("+a").unsafeCast<Double>()

fun numberToInt(a: dynamic): Int = doubleToInt(a)

fun numberToShort(a: dynamic): Short = toShort(numberToInt(a))

fun toByte(a: dynamic): Byte = js("(a & 0xFF) << 24 >> 24").unsafeCast<Byte>()

fun toShort(a: dynamic): Short = js("(a & 0xFFFF) << 16 >> 16").unsafeCast<Short>()

fun doubleToInt(a: dynamic) = js("""
    if (a > 2147483647) return 2147483647;
    if (a < -2147483648) return -2147483648;
    return a | 0;
""").unsafeCast<Int>()