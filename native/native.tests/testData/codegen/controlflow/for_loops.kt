/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {

    // Simple loops
    for (i in 0..4) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 0 until 4) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 4 downTo 0) {
        sb.append(i)
    }
    sb.appendLine()
    sb.appendLine()

    // Steps
    for (i in 0..4 step 2) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 0 until 4 step 2) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 4 downTo 0 step 2) {
        sb.append(i)
    }
    sb.appendLine()
    sb.appendLine()


    // Two steps
    for (i in 0..6 step 2 step 3) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 0 until 6 step 2 step 3) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in 6 downTo 0 step 2 step 3) {
        sb.append(i)
    }
    sb.appendLine()
    sb.appendLine()

    // Without constants
    val a = 0
    val b = 4
    val s = 2
    for (i in a..b step s) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in a until b step s) {
        sb.append(i)
    }
    sb.appendLine()

    for (i in b downTo a step s) {
        sb.append(i)
    }
    sb.appendLine()
    sb.appendLine()

    assertEquals("""
        01234
        0123
        43210
        
        024
        02
        420
        
        036
        03
        630
        
        024
        02
        420


        """.trimIndent(), sb.toString())
    return "OK"
}