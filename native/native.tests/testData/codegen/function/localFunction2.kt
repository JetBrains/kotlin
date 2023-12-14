/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    var a = 0
    fun local() {
        class A {
            val b = 0
            fun f() {
                a = b
            }

        }
        fun local2() : A {
            return A()
        }
    }
    return "OK"
}