/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: cacheMode=STATIC_ONLY_DIST
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:*

import kotlin.test.*
import kotlin.native.internal.*

class A {
    fun f(x: Int) = x + 13
}

fun f(x: Int): Int {
    val a = A()
    assertTrue(a.isStack())
    return a.f(x)
}

fun box(): String {
    assertEquals(f(42), 55)

    return "OK"
}
