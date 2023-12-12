/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// abstract class vtable call
abstract class A {
    abstract fun foo(): String
}

abstract class B : A()

class Z : B() {
    override fun foo() = "Z"
}


fun box(): String {
    val z = Z()
    val b: B = z
    val a: A = z
    return when {
        z.foo() != "Z" -> "Fail #1"
        b.foo() != "Z" -> "Fail #2"
        a.foo() != "Z" -> "Fail #3"
        else -> "OK"
    }
}
