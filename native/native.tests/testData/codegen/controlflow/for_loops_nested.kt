/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    // Simple
    for (i in 0..2) {
        for (j in 0..2) {
            sb.append("$i$j ")
        }
    }
    sb.appendLine()

    // Break
    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            sb.append("$i$j ")
            if (j == 1) break
        }
    }
    sb.appendLine()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            sb.append("$i$j ")
            if (j == 1) break@l2
        }
    }
    sb.appendLine()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            sb.append("$i$j ")
            if (j == 1) break@l1
        }
    }
    sb.appendLine()

    // Continue
    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue
            sb.append("$i$j ")
        }
    }
    sb.appendLine()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue@l2
            sb.append("$i$j ")
        }
    }
    sb.appendLine()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue@l1
            sb.append("$i$j ")
        }
    }
    sb.appendLine()

    assertEquals("""
        00 01 02 10 11 12 20 21 22 
        00 01 10 11 20 21 
        00 01 10 11 20 21 
        00 01 
        00 02 10 12 20 22 
        00 02 10 12 20 22 
        00 10 20 

    """.trimIndent(), sb.toString())
    return "OK"
}