/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun foo(vararg args: Any?) {
    for (arg in args) {
        sb.appendLine(arg.toString())
    }
}

fun box(): String {
    foo(1, null, true, "Hello")
    val nonConstInt = 1
    val nonConstNull = null
    val nonConstBool = true
    val nonConstString = "Hello"
    foo(nonConstInt, nonConstNull, nonConstBool, nonConstString)

    assertEquals("""
        1
        null
        true
        Hello
        1
        null
        true
        Hello

    """.trimIndent(), sb.toString())
    return "OK"
}