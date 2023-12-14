/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

fun box(): String {
    val xBool = true
    val xBoolStatic : Any = false
    val xBoolDyanmic : Any = !xBool
    assertSame(xBoolStatic, xBoolDyanmic)

    val xByte = 1.toByte()
    val xByteStatic : Any = 2.toByte()
    val xByteDyanmic : Any = (xByte + xByte).toByte()
    assertSame(xByteStatic, xByteDyanmic)

    val xShort = 1.toShort()
    val xShortStatic : Any = 2.toShort()
    val xShortDyanmic : Any = (xShort + xShort).toShort()
    assertSame(xShortStatic, xShortDyanmic)

    val xInt = 1.toInt()
    val xIntStatic : Any = 2.toInt()
    val xIntDyanmic : Any = xInt + xInt
    assertSame(xIntStatic, xIntDyanmic)

    val xChar = 1.toChar()
    val xCharStatic : Any = 2.toChar()
    val xCharDyanmic : Any = (xChar.code + xChar.code).toChar()
    assertSame(xCharStatic, xCharDyanmic)

    val xLong = 1.toLong()
    val xLongStatic = 2.toLong()
    val xLongDyanmic = xLong + xLong
    assertSame(xLongStatic, xLongDyanmic)

    return "OK"
}