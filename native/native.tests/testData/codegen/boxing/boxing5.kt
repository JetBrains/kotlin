/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Int?) {
    printInt(arg ?: 16)
}

fun box(): String {
    foo(null)
    foo(42)
    val nonConstNull = null
    val nonConstInt = 42
    foo(nonConstNull)
    foo(nonConstInt)

    assertEquals("16\n42\n16\n42\n", sb.toString())
    return "OK"
}