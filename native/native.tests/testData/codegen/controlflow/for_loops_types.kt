/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    for (i in 0.toByte() .. 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toByte()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toShort()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toInt()) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toLong()) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toByte()) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toShort()) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toInt()) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toLong()) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toByte()) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toShort()) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toInt()) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toLong()) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toByte()) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toShort()) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toInt()) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toLong()) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toByte()) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toShort()) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toInt()) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toLong()) sb.append(i); sb.appendLine()
    for (i in 'a' .. 'd') sb.append(i); sb.appendLine()
    for (i in 'a' until 'd') sb.append(i); sb.appendLine()
    for (i in 'd' downTo 'a') sb.append(i); sb.appendLine()

    for (i in 0.toByte() .. 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() .. 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() .. 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() .. 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toByte() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toShort() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toInt() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() .. 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toByte() until 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toShort() until 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 0.toInt() until 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toByte() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toShort() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toInt() step 2L) sb.append(i); sb.appendLine()
    for (i in 0.toLong() until 4.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toByte() downTo 0.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toShort() downTo 0.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toByte() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toShort() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toInt() step 2) sb.append(i); sb.appendLine()
    for (i in 4.toInt() downTo 0.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toByte() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toShort() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toInt() step 2L) sb.append(i); sb.appendLine()
    for (i in 4.toLong() downTo 0.toLong() step 2L) sb.append(i); sb.appendLine()
    for (i in 'a' .. 'd' step 2) sb.append(i); sb.appendLine()
    for (i in 'a' until 'd' step 2) sb.append(i); sb.appendLine()
    for (i in 'd' downTo 'a' step 2) sb.append(i); sb.appendLine()

    assertEquals("""
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        01234
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        0123
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        43210
        abcd
        abc
        dcba
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        024
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        02
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        420
        ac
        ac
        db
        
        """.trimIndent(), sb.toString())
    return "OK"
}