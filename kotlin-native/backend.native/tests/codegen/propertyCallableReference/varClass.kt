/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.propertyCallableReference.varClass

import kotlin.test.*

class A(var x: Int)

@Test fun runTest() {
    val p1 = A::x
    val a = A(42)
    p1.set(a, 117)
    println(a.x)
    println(p1.get(a))
    val p2 = a::x
    p2.set(42)
    println(a.x)
    println(p2.get())
}