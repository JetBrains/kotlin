/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaultsFromFakeOverride

import kotlin.test.*

interface I<T> {
    fun f(x: String = "42"): String
}

open class A<T> {
    open fun f(x: String) = x
}

class B : A<String>(), I<String>

@Test fun runTest() {
    val b = B()
    println(b.f())
}