/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.localClass.localFunctionInLocalClass

import kotlin.test.*

@Test fun runTest() {
    var x = 0
    class A {
        fun bar() {
            fun local() {
                class B {
                    fun baz() {
                        fun local2() {
                            x++
                        }
                        local2()
                    }
                }
                B().baz()
            }
            local()
        }
    }
    A().bar()
    println("OK")
}