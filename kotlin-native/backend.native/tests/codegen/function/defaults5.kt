/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults5

import kotlin.test.*

class TestClass(val x: Int) {
    fun foo(y: Int = x) {
        println(y)
    }
}

fun TestClass.bar(y: Int = x) {
    println(y)
}

@Test fun runTest() {
    TestClass(5).foo()
    TestClass(6).bar()
}