/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    for (i in Int.MAX_VALUE - 1 .. Int.MAX_VALUE) { sb.append(i); sb.append(' ') }; sb.appendLine()
    for (i in Int.MAX_VALUE - 1 until Int.MAX_VALUE) { sb.append(i); sb.append(' ') }; sb.appendLine()
    for (i in Int.MIN_VALUE + 1 downTo Int.MIN_VALUE) { sb.append(i); sb.append(' ') }; sb.appendLine()

    // Empty loops
    for (i in Byte.MIN_VALUE  until Byte.MIN_VALUE)  { sb.append(i); sb.append(' ') }
    for (i in Short.MIN_VALUE until Short.MIN_VALUE) { sb.append(i); sb.append(' ') }
    for (i in Int.MIN_VALUE   until Int.MIN_VALUE)   { sb.append(i); sb.append(' ') }
    for (i in Long.MIN_VALUE  until Long.MIN_VALUE)  { sb.append(i); sb.append(' ') }
    for (i in 0.toChar()      until 0.toChar())      { sb.append(i); sb.append(' ') }

    for (i in 0 until Byte.MIN_VALUE)  { sb.append(i); sb.append(' ') }
    for (i in 0 until Short.MIN_VALUE) { sb.append(i); sb.append(' ') }
    for (i in 0 until Int.MIN_VALUE)   { sb.append(i); sb.append(' ') }
    for (i in 0 until Long.MIN_VALUE)  { sb.append(i); sb.append(' ') }
    for (i in 'a' until 0.toChar())    { sb.append(i); sb.append(' ') }


    val M = Int.MAX_VALUE / 2
    for (i in M + 4..M + 10 step M)  { sb.append(i); sb.append(' ') }; sb.appendLine()

    assertEquals("""
        2147483646 2147483647 
        2147483646 
        -2147483647 -2147483648 
        1073741827 

    """.trimIndent(), sb.toString())
    return "OK"
}