/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val x = foo {
        it + 1
    }
    assertEquals(43, x)
    return "OK"
}

fun foo(f: (Int) -> Int) = f(42)