/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface I1<T> {
    fun defaultX(): T
    fun foo(x: T = defaultX()): T
}

interface I2 : I1<Int> {

}

class C : I2 {
    override fun defaultX() = 42
    override fun foo(x: Int) = x
}

fun box(): String {
    val c: I2 = C()
    assertEquals(42, c.foo())

    return "OK"
}