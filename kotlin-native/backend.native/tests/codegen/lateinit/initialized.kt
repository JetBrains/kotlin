/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lateinit.initialized

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

@Test fun runTest() {
    val a = A()
    a.s = "zzz"
    println(a.foo())
}