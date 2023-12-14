/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

object C {
    const val x = 42
}

fun getC(): C {
    sb.appendLine(123)
    return C
}

fun box(): String {
    sb.appendLine(getC().x)

    assertEquals("""
        123
        42

    """.trimIndent(), sb.toString())
    return "OK"
}

