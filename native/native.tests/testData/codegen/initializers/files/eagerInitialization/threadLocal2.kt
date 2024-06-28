/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
@ThreadLocal
var z1 = false
var z2 = false
@ThreadLocal
var z3 = false

// FILE: lib2.kt
import kotlin.test.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
@ThreadLocal
val x = run { z1 = true; 42 }

// Won't be initialized (cause no function from the file will be called during [x] initialization).
val y1 = run { z2 = true; 117 }

@ThreadLocal
val y2 = run { z3 = true; 117 }

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertTrue(z1)
    assertFalse(z2)
    assertFalse(z3)

    return "OK"
}
