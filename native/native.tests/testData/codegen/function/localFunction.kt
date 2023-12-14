/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val x = 1
    fun local0() = sb.appendLine(x)
    fun local1() {
        fun local2() {
            local1()
        }
        local0()
    }

    fun l1() {
        var x = 1
        fun l2() {
            fun l3() {
                l1()
                x = 5
            }
            l3()
        }
    }

    assertEquals("", sb.toString())
    return "OK"
}