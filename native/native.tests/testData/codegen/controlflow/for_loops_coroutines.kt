/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.coroutines.*

val sb = StringBuilder()

fun box(): String {
    val sq = sequence {
        for (i in 0..6 step 2) {
            sb.append("before: $i ")
            yield(i)
            sb.appendLine("after: $i")
        }
    }
    sb.appendLine("Got: ${sq.joinToString(separator = " ")}")

    assertEquals("""
        before: 0 after: 0
        before: 2 after: 2
        before: 4 after: 4
        before: 6 after: 6
        Got: 0 2 4 6
        
        """.trimIndent(), sb.toString())
    return "OK"
}