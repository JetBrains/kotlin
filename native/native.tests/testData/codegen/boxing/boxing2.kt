/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x.toString())
fun printBoolean(x: Boolean) = sb.appendLine(x.toString())
fun printUInt(x: UInt) = sb.appendLine(x.toString())

fun foo(arg: Any) {
    if (arg is Int)
        printInt(arg)
    else if (arg is Boolean)
        printBoolean(arg)
    else if (arg is UInt)
        printUInt(arg)
    else
        sb.appendLine("other")
}

fun box(): String {
    foo(1)
    foo(2u)
    foo(true)
    foo("Hello")
    val nonConstInt = 1
    val nonConstUInt = 2u
    val nonConstBool = true
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstUInt)
    foo(nonConstBool)
    foo(nonConstString)

    assertEquals("""
        1
        2
        true
        other
        1
        2
        true
        other
        
    """.trimIndent(), sb.toString())
    return "OK"
}