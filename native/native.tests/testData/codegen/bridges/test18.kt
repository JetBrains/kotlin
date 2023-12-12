/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// overriden function returns Unit
open class A {
    open fun foo(): Any = 42
}

open class B: A() {
    override fun foo(): Unit { }
}

fun box(): String {
    val a: A = B()
    val afoo = a.foo()
    if (afoo != Unit) return "FAIL $afoo"
    return "OK"
}
