// OUTPUT_DATA_FILE: defaults5.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class TestClass(val x: Int) {
    fun foo(y: Int = x) {
        println(y)
    }
}

fun TestClass.bar(y: Int = x) {
    println(y)
}

fun box(): String {
    TestClass(5).foo()
    TestClass(6).bar()

    return "OK"
}
