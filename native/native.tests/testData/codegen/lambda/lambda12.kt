/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val lambda = { s1: String, s2: String ->
        sb.appendLine(s1)
        sb.appendLine(s2)
    }

    lambda("one", "two")

    assertEquals("""
        one
        two

    """.trimIndent(), sb.toString())
    return "OK"
}