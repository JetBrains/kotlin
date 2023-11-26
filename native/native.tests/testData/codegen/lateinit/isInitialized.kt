// OUTPUT_DATA_FILE: isInitialized.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() {
        println(::s.isInitialized)
    }
}

fun box(): String {
    val a = A()
    a.foo()
    a.s = "zzz"
    a.foo()

    return "OK"
}
