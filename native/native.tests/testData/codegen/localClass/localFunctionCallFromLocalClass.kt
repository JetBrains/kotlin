/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    var x = 1
    fun local1() {
        x++
    }

    class A {
        fun bar() {
            local1()
        }
    }
    A().bar()
    return "OK"
}