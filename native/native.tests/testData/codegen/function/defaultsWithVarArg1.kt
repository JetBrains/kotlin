/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun foo(s: String = "", vararg args: Any) {
    if (args == null) {
        sb.appendLine("Failed!")
    } else {
        sb.append("$s ")
        args.forEach {
            sb.append("$it")
        }
        sb.appendLine(", Correct!")
    }
}

fun box(): String {
    foo("Hello")
    foo("Hello", "World")
    foo()

    assertEquals("""
        Hello , Correct!
        Hello World, Correct!
         , Correct!

    """.trimIndent(), sb.toString())
    return "OK"
}