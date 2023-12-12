/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Any) {
    printInt(arg as? Int ?: 16)
}

fun box(): String {
    foo(42)
    foo("Hello")
    val nonConstInt = 42
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstString)

    assertEquals("42\n16\n42\n16\n", sb.toString())
    return "OK"
}