/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
var z1 = false
var z2 = false

// FILE: lib2.kt
import kotlin.test.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val x = foo()

private fun foo(): Int {
    z1 = true
    return 42
}

// Will be initialized since [x]'s initializer calls a function from the file.
val y = run { z2 = true; 117 }

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertTrue(z1)
    assertTrue(z2)

    return "OK"
}
