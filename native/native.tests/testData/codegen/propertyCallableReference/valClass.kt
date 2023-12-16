/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A(val x: Int)

fun box(): String {
    val p1 = A::x
    assertEquals(42, p1.get(A(42)))
    val a = A(117)
    val p2 = a::x
    assertEquals(117, p2.get())

    return "OK"
}