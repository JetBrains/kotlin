/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.propertyCallableReference.valClass

import kotlin.test.*

class A(val x: Int)

@Test fun runTest() {
    val p1 = A::x
    println(p1.get(A(42)))
    val a = A(117)
    val p2 = a::x
    println(p2.get())
}