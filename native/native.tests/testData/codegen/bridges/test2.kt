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
    val res1 = c.foo().toString()
    if (res1 != "42") return "FAIL 1: $res1"
    val res2 = a.foo().toString()
    if (res2 != "42") return "FAIL 2: $res2"

    return "OK"
}