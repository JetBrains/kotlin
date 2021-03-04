/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test0

import kotlin.test.*

// vtable call
open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 42
}

@Test fun runTest() {
    val c = C()
    val a: A = c
    println(c.foo().toString())
    println(a.foo().toString())
}