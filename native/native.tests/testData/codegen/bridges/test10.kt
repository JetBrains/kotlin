/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

open class A<T> {
    open fun foo(x: T) {
        sb.appendLine(x.toString())
    }
}

interface I {
    fun foo(x: Int)
}

class B : A<Int>(), I {
    var z: Int = 5
    override fun foo(x: Int) {
        z = x
    }
}

fun zzz(a: A<Int>) {
    a.foo(42)
}

fun box(): String {
    val b = B()
    zzz(b)
    val a = A<Int>()
    zzz(a)
    sb.appendLine(b.z.toString())

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}