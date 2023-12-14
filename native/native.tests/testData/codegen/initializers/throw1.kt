/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
val x = computeX()

private fun computeX() = 42

fun baz1() { }

fun baz2() { }

// FILE: main.kt
import kotlin.test.*

fun bar(x: Int) = if (x == 0) error("") else x

fun foo(x: Int) {
    try {
        bar(x)
        baz1()
    } catch (t: Throwable) { }
}

fun box(): String {
    foo(0)
    assertEquals(42, x)
    return "OK"
}
