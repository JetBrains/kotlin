/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val x = try {
        sb.appendLine("Try")
        5
    } catch (e: Throwable) {
        throw e
    }

    sb.appendLine(x)

    assertEquals("""
        Try
        5

    """.trimIndent(), sb.toString())
    return "OK"
}