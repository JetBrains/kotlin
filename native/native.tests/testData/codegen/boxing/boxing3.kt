/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun printInt(x: Int) = sb.appendLine(x)

fun foo(arg: Int?) {
    if (arg != null)
        printInt(arg)
}

fun box(): String {
    foo(42)
    val nonConst = 42
    foo(nonConst)

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}