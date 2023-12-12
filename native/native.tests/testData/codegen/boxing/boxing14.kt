/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    42.println()
    val nonConst = 42
    nonConst.println()

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}

fun <T> T.println() = sb.appendLine(this)