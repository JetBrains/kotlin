/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

interface I {
    fun foo(x: Int)
}

abstract class A<T> {
    abstract fun foo(x: T)
}

class B : A<Int>(), I {
    override fun foo(x: Int) {
        sb.appendLine(x)
        Unit
    }
}

fun box(): String {
    val b = B()
    val a: A<Int> = b
    val c: I = b
    b.foo(42)
    a.foo(42)
    c.foo(42)

    assertEquals("42\n42\n42\n", sb.toString())
    return "OK"
}