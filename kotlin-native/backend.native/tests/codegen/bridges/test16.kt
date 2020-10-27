/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test16

import kotlin.test.*

interface A {
    fun foo(): String
}

abstract class C: A

open class B: C() {
    override fun foo(): String {
        return "OK"
    }
}

fun bar(c: C) = c.foo()

@Test fun runTest() {
    val b = B()
    val c: C = b
    println(bar(b))
    println(bar(c))
}