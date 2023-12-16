/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var str = "original"

    val lambda = {
        sb.appendLine(str)
    }

    lambda()

    str = "changed"
    lambda()

    assertEquals("""
        original
        changed

    """.trimIndent(), sb.toString())
    return "OK"
}