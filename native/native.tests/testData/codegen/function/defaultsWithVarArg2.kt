/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun foo(vararg arr: Int = intArrayOf(1, 2)) {
    arr.forEach { sb.appendLine(it) }
}

fun box(): String {
    foo()
    foo(42)

    assertEquals("""
        1
        2
        42

    """.trimIndent(), sb.toString())
    return "OK"
}