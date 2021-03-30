/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test6

import kotlin.test.*

// vtable call + interface call
interface Z {
    fun foo(): Any
}

interface Y {
    fun foo(): Int
}

open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 42
}

open class D: C(), Y, Z

@Test fun runTest() {
    val d = D()
    val y: Y = d
    val z: Z = d
    val c: C = d
    val a: A = d
    println(d.foo().toString())
    println(y.foo().toString())
    println(z.foo().toString())
    println(c.foo().toString())
    println(a.foo().toString())
}