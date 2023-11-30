/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: interface0.out

import kotlin.test.*

interface A {
    fun b() = c()
    fun c()
}

class B(): A {
    override fun c() {
        println("PASSED")
    }
}

fun box(): String {
    val a:A = B()
    a.b()

    return "OK"
}

