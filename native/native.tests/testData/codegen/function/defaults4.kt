/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

open class A {
    open fun foo(x: Int = 42) = sb.append(x)
}

open class B : A()

class C : B() {
    override fun foo(x: Int) = sb.append(x + 1)
}

fun box(): String {
    C().foo()

    assertEquals("43", sb.toString())
    return "OK"
}