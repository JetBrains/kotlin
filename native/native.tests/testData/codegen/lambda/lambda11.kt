/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val first = "first"
    val second = "second"

    run {
        sb.appendLine(first)
        sb.appendLine(second)
    }

    assertEquals("""
        first
        second

    """.trimIndent(), sb.toString())
    return "OK"
}

fun run(f: () -> Unit) {
    f()
}