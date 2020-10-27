/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.escape1

import kotlin.test.*

class B(val s: String)

class A {
    val b = B("zzz")
}

fun foo(): B {
    val a = A()
    return a.b
}

@Test fun runTest() {
    println(foo().s)
}