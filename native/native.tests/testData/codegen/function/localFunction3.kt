/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    fun bar() {
        fun local1() {
            bar()
        }
        local1()

        var x = 0
        fun local2() {
            x++
            bar()
        }
        local2()
    }
    return "OK"
}