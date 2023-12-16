/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A(var x: Int)

fun box(): String {
    val p1 = A::x
    val a = A(42)
    p1.set(a, 117)
    assertEquals(117, a.x)
    assertEquals(117, p1.get(a))
    val p2 = a::x
    p2.set(42)
    assertEquals(42, a.x)
    assertEquals(42, p2.get())

    return "OK"
}