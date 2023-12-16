/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        Done
        Finally
        0

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo(): Int {
    try {
        sb.appendLine("Done")
        return 0
    } finally {
        sb.appendLine("Finally")
    }

    sb.appendLine("After")
    return 1
}