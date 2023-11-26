// OUTPUT_DATA_FILE: test2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

// vtable call, bridge inherited
open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 42
}

open class D: C()

fun box(): String {
    val c = D()
    val a: A = c
    println(c.foo().toString())
    println(a.foo().toString())

    return "OK"
}
