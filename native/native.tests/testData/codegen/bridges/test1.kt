/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// interface call, bridge overridden
interface Z1 {
    fun foo(x: Int) : Any
}

open class A : Z1 {
    override fun foo(x: Int) : Int = 5
}

open class B : A() {
    override fun foo(x: Int) : Int = 42
}

fun box(): String {
    val z1: A = B()
    val res = (z1.foo(1) + 1000).toString()
    if (res != "1042") return "FAIL: $res"

    return "OK"
}

