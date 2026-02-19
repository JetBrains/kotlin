/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface I1<T> {
    fun foo(x: T): String
}
interface I2<T> {
    fun foo(x: T): String
}
class C : I1<String>, I2<Int> {
    override fun foo(x: String) = "I1.foo($x)"
    override fun foo(x: Int) = "I2.foo($x)"
}

fun box(): String {
    val c = C()
    val i1: I1<String> = c
    assertEquals("I1.foo(str)", i1.foo("str"))
    val i2: I2<Int> = c
    assertEquals("I2.foo(42)", i2.foo(42))

    return "OK"
}