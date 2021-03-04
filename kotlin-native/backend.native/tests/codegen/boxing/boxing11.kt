/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing11

import kotlin.test.*

fun printInt(x: Int) = println(x)

class Foo(val value: Int?) {
    fun foo() {
        printInt(if (value != null) value else 42)
    }
}

@Test fun runTest() {
    Foo(17).foo()
    Foo(null).foo()
}