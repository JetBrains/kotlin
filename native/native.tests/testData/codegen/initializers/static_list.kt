/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

fun box(): String {
    assertEquals("abc, def, ghi", listOf("abc", "def", "ghi").joinToString())
    assertEquals("1, 2, 3", listOf(1, 2, 3).joinToString())
    assertEquals("4, 5, 6", listOf(4.toLong(), 5.toLong(), 6.toLong()).joinToString())
    assertEquals("7, 8, 9", listOf(7.toShort(), 8.toShort(), 9.toShort()).joinToString())
    assertEquals("10, 11, 12", listOf(10.toByte(), 11.toByte(), 12.toByte()).joinToString())
    assertEquals("abc", listOf('a', 'b', 'c').joinToString(""))
    assertEquals("1.5, 2.5, -3.5", listOf(1.5f, 2.5f, -3.5f).joinToString())
    assertEquals("4.5, 5.5, -6.5", listOf(4.5, 5.5, -6.5).joinToString())
    assertEquals("13, 14, 4294967295", listOf(13u, 14u, 4294967295u).joinToString())
    assertEquals("15, 16, 17", listOf(15.toULong(), 16.toULong(), 17.toULong()).joinToString())
    assertEquals("18, 19, 40000", listOf(18.toUShort(), 19.toUShort(), 40000.toUShort()).joinToString())
    assertEquals("20, 21, 200", listOf(20.toUByte(), 21.toUByte(), 200.toUByte()).joinToString())

    assertEquals("abc, 1, 2, 3, 4, a, 1.5, 2.5, 5, 6, 7, 8",
            listOf("abc", 1, 2.toLong(), 3.toShort(), 4.toByte(), 'a', 1.5f, 2.5, 5u, 6.toULong(), 7.toUShort(), 8.toUByte()).joinToString())

    return "OK"
}
