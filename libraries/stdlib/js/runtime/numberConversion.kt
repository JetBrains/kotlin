/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.js.internal.boxedLong.BoxedLongApi
import kotlin.js.internal.boxedLong.fromInt
import kotlin.js.internal.boxedLong.fromNumber

internal fun numberToByte(a: dynamic): Byte = toByte(numberToInt(a))

internal fun numberToDouble(@Suppress("UNUSED_PARAMETER") a: dynamic): Double = js("+a").unsafeCast<Double>()

internal fun numberToInt(a: dynamic): Int = if (a is Long) a.toInt() else doubleToInt(a)

internal fun numberToShort(a: dynamic): Short = toShort(numberToInt(a))

// << and >> shifts are used to preserve sign of the number
internal fun toByte(@Suppress("UNUSED_PARAMETER") a: dynamic): Byte = js("a << 24 >> 24").unsafeCast<Byte>()
internal fun toShort(@Suppress("UNUSED_PARAMETER") a: dynamic): Short = js("a << 16 >> 16").unsafeCast<Short>()

@BoxedLongApi
internal fun numberToLong(a: dynamic): Long = if (a is Long) a else fromNumber(a)

@BoxedLongApi
internal fun toLong(a: dynamic): Long = fromInt(a)

internal fun doubleToInt(a: Double): Int = when {
    a > 2147483647 -> 2147483647
    a < -2147483648 -> -2147483648
    else -> jsBitwiseOr(a, 0)
}

internal fun numberToChar(a: dynamic) = Char(numberToInt(a).toUShort())
