/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

class A(val msg: String) {
    init {
        sb.appendLine("init $msg")
    }
    override fun toString(): String = msg
}

val globalValue1 = 1
val globalValue2 = A("globalValue2")
val globalValue3 = A("globalValue3")

fun box(): String {
    sb.appendLine(globalValue1.toString())
    sb.appendLine(globalValue2.toString())
    sb.appendLine(globalValue3.toString())

    assertEquals("""
        init globalValue2
        init globalValue3
        1
        globalValue2
        globalValue3

    """.trimIndent(), sb.toString())
    return "OK"
}

