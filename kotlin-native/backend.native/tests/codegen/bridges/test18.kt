/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test18

import kotlin.test.*

// overriden function returns Unit
open class A {
    open fun foo(): Any = 42
}

open class B: A() {
    override fun foo(): Unit { }
}

@Test fun runTest() {
    val a: A = B()
    println(a.foo())
}
