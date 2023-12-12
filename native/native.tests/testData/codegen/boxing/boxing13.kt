/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun is42(x: Any?) {
    sb.appendLine(x == 42)
    sb.appendLine(42 == x)
}

fun box(): String {
    is42(16)
    is42(42)
    is42("42")
    val nonConst16 = 16
    val nonConst42 = 42
    val nonConst42String = "42"
    is42(nonConst16)
    is42(nonConst42)
    is42(nonConst42String)

    assertEquals("""
        false
        false
        true
        true
        false
        false
        false
        false
        true
        true
        false
        false
        
    """.trimIndent(), sb.toString())
    return "OK"
}