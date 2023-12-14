/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
val y = computeY()

private fun computeY() = 42

fun baz1() { }

fun baz2() { }

// FILE: main.kt
import kotlin.test.*

fun bar(x: Int) = if (x == 0) error("") else x

val sb = StringBuilder()

fun foo(x: Int) {
    try {
        bar(x)
        baz1()
    } catch (t: Throwable) {
        sb.appendLine(y)
    }
}

fun box(): String {
    foo(0)

    assertEquals("42\n", sb.toString())
    return "OK"
}
