/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A(y: Int) {
    var x = y
}

var A.z: Int
    get() = this.x
    set(value: Int) {
        this.x = value
    }

fun box(): String {
    val p1 = A::z
    val a = A(42)
    p1.set(a, 117)
    assertEquals(117, a.x)
    assertEquals(117, p1.get(a))
    val p2 = a::z
    p2.set(42)
    assertEquals(42, a.x)
    assertEquals(42, p2.get())

    return "OK"
}