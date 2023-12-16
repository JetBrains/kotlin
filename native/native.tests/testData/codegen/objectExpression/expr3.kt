/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var cnt = 0

    var x: Any = ""

    for (i in 0 .. 1) {
        sb.appendLine(x)
        cnt++
        val y = object {
            override fun toString() = cnt.toString()
        }
        x = y
    }
    sb.appendLine(x)

    assertEquals("""
        
        1
        2

    """.trimIndent(), sb.toString())
    return "OK"
}
