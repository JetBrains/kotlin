/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.escape2

import kotlin.test.*

class A(val s: String)

class B {
    var a: A? = null
}

class C(val b: B)

fun foo(c: C) {
    c.b.a = A("zzz")
}

fun bar(b: B) {
    val c = C(b)
    foo(c)
}

@ThreadLocal
val global = B()

@Test fun runTest() {
    bar(global)
    println(global.a!!.s)
}