/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// vtable call + interface call
interface Z {
    fun foo(): Any
}

interface Y {
    fun foo(): Int
}

open class A {
    fun foo(): Int = 42
}

open class B: A(), Z, Y

fun box(): String {
    val z: Z = B()
    val y: Y = z as Y
    val res1 = z.foo().toString()
    if (res1 != "42") return "FAIL 1: $res1"
    val res2 = y.foo().toString()
    if (res2 != "42") return "FAIL 2: $res2"

    return "OK"
}