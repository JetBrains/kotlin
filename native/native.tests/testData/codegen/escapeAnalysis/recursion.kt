/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A {
    var f: A? = null
}

fun foo(k: Int, a1: A, a2: A): A {
    val a3 = A()
    if (k == 0)
        return a1
    a3.f = a1
    return foo(k - 1, a2, a3)
}

fun box(): String {
    foo(3, A(), A()).toString()

    return "OK"
}