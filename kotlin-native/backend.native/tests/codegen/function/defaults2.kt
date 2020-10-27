/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults2

import kotlin.test.*


fun Int.foo(inc0:Int, inc:Int = 0) = this + inc0 + inc

@Test fun runTest() {
    val v = 42.foo(0)
    if (v != 42) {
        println("test failed v:$v expected:42")
        throw Error()
    }
}
