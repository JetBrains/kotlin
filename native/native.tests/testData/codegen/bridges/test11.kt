// OUTPUT_DATA_FILE: test11.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

interface I {
    fun foo(x: Int)
}

abstract class A<T> {
    abstract fun foo(x: T)
}

class B : A<Int>(), I {
    override fun foo(x: Int) = println(x)
}

fun box(): String {
    val b = B()
    val a: A<Int> = b
    val c: I = b
    b.foo(42)
    a.foo(42)
    c.foo(42)

    return "OK"
}
