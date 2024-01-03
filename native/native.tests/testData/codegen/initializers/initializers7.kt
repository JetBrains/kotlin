/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

object A {
    init {
        assertAUninitialized()
    }
    val a1 = 7
    val a2 = 12
}

// Check that A is initialized dynamically.
fun assertAUninitialized() {
    assertEquals(0, A.a1)
    assertEquals(0, A.a2)
}

object B {
    init {
        assertBUninitialized()
    }
    val b1 = A.a2
    val b2 = C.c1
}

// Check that B is initialized dynamically.
fun assertBUninitialized() {
    assertEquals(0, B.b1)
    assertEquals(0, B.b2)
}

object C {
    init {
        assertCUninitialized()
    }
    val c1 = 42
    val c2 = A.a1
    val c3 = B.b1
    val c4 = B.b2
}

// Check that C is initialized dynamically.
fun assertCUninitialized() {
    assertEquals(0, C.c1)
    assertEquals(0, C.c2)
    assertEquals(0, C.c3)
    assertEquals(0, C.c4)
}

fun box(): String {
    assertEquals(A.a1, C.c2)
    assertEquals(A.a2, C.c3)
    assertEquals(C.c1, C.c4)

    return "OK"
}
