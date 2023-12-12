/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// abstract bridge
interface A<T> {
    fun foo(): T
}

abstract class B<T>: A<T>

abstract class C: B<Int>()

class D: C() {
    override fun foo(): Int {
        return 42
    }
}

fun box(): String {
    val d = D()
    val c: C = d
    val b: B<Int> = d
    val a: A<Int> = d
    val foo0 = d.foo()
    if (foo0 != 42) return "FAIL d: $foo0"
    val foo1 = c.foo()
    if (foo1 != 42) return "FAIL c: $foo1"
    val foo2 = b.foo()
    if (foo2 != 42) return "FAIL b: $foo2"
    val foo3 = a.foo()
    if (foo3 != 42) return "FAIL a: $foo3"

    return "OK"
}