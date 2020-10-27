/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.localClass.localFunctionCallFromLocalClass

import kotlin.test.*

@Test fun runTest() {
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
    println("OK")
}