// OUTPUT_DATA_FILE: localFunction.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val x = 1
    fun local0() = println(x)
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

    println("OK")

    return "OK"
}
