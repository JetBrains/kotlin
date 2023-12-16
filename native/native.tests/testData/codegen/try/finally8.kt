/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    sb.appendLine(foo())

    assertEquals("""
        Finally 1
        Finally 2
        42

    """.trimIndent(), sb.toString())
    return "OK"
}

fun foo(): Int {
    try {
        try {
            return 42
        } finally {
            sb.appendLine("Finally 1")
        }
    } finally {
        sb.appendLine("Finally 2")
    }

    sb.appendLine("After")
    return 2
}