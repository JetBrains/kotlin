/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    foo<String>("17")
    bar<String>("17")
    foo<String>(42)
    bar<String>(42)

    assertEquals("17\n17\n42\n42\n", sb.toString())
    return "OK"
}

fun <T> foo(x: Any?) {
    val y = x as T
    sb.appendLine(y.toString())
}

fun <T> bar(x: Any?) {
    val y = x as? T
    sb.appendLine(y.toString())
}