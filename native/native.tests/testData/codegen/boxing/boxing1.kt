/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun foo(arg: Any) {
    sb.appendLine(arg.toString())
}

fun box(): String {
    foo(1)
    foo(2u)
    foo(false)
    foo("Hello")
    val nonConstInt = 1
    val nonConstUInt = 2u
    val nonConstBool = false
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstUInt)
    foo(nonConstBool)
    foo(nonConstString)

    assertEquals("""
        1
        2
        false
        Hello
        1
        2
        false
        Hello

    """.trimIndent(), sb.toString())
    return "OK"
}