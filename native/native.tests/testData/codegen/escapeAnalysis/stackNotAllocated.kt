/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// Escape analysis is turned off for the delta-main scheme, nothing is allocated on the stack.
// DISABLE_NATIVE: gcStackMapScheme=DELTA_MAIN
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=NO

import kotlin.test.*
import kotlin.native.internal.*

class A {
    fun f(x: Int) = x + 13
}

fun f(x: Int): Int {
    val a = A()
    assertFalse(a.isStack())
    return a.f(x)
}

fun box(): String {
    assertEquals(f(42), 55)
    return "OK"
}
