// OUTPUT_DATA_FILE: globalIsInitialized.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

lateinit var s: String

fun foo() {
    println(::s.isInitialized)
}

class A(val x: Int)

fun box(): String {
    foo()
    s = "zzz"
    foo()

    return "OK"
}
