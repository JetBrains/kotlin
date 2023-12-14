/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    sb.appendLine(y)
}

fun box(): String {
    val z = Z()
    z.foo()
    assertEquals("42\n", sb.toString())
    return "OK"
}