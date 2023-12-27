/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
import kotlin.test.*

val x = foo()

@ThreadLocal
val y = bar()

private fun foo() = 42

private fun bar(): Int {
    assertEquals(x, 42)
    return 117
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertEquals(y, 117)
    return "OK"
}
