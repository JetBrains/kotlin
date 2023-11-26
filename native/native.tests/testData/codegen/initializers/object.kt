/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
var z = false

// FILE: lib2.kt
val x = foo()

fun foo(): Int {
    z = true
    return 42
}

object Z {
    fun bar() { }
    fun baz() = x
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    Z.bar()
    assertFalse(z)
    assertEquals(42, Z.baz())
    assertTrue(z)

    return "OK"
}
