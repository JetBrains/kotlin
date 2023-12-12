/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// non-generic interface, generic impl, vtable call + interface call
open class A<T> {
    open var size: T = 56 as T
}

interface C {
    var size: Int
}

open class B : C, A<Int>()

open class D: B() {
    override var size: Int = 117
}

fun <T> foo(a: A<T>) {
    a.size = 42 as T
}

fun box(): String {
    val b = B()

    foo(b)
    if (b.size != 42) return "fail 1"
    val d = D()
    if (d.size != 117) return "fail 2"
    foo(d)
    if (d.size != 42) return "fail 3"

    return "OK"
}
