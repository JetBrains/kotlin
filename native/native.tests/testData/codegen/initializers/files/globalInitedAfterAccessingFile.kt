/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
var z = false

// FILE: lib2.kt
import kotlin.test.*

val x = foo()

private fun foo(): Int {
    z = true
    return 42
}

fun bar() = 117

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    bar()
    assertTrue(z)

    return "OK"
}
